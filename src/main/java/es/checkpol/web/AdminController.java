package es.checkpol.web;

import es.checkpol.domain.AppUserRole;
import es.checkpol.service.AdminUserSummary;
import es.checkpol.service.AdminSettingsService;
import es.checkpol.service.AdminSesMonitoringService;
import es.checkpol.service.AppIncidentService;
import es.checkpol.service.AppUserAdminService;
import es.checkpol.service.AppUserPrincipal;
import es.checkpol.service.CurrentAppUserService;
import es.checkpol.service.MunicipalityAdminService;
import es.checkpol.service.SesConnectionTestResult;
import es.checkpol.service.SesLoteStatusResult;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.time.ZoneId;

@Controller
public class AdminController {

    private final AppUserAdminService appUserAdminService;
    private final MunicipalityAdminService municipalityAdminService;
    private final AdminSettingsService adminSettingsService;
    private final AdminSesMonitoringService adminSesMonitoringService;
    private final AppIncidentService appIncidentService;
    private final CurrentAppUserService currentAppUserService;
    private final AdminSettingsSummary adminSettingsSummary;

    public AdminController(
        AppUserAdminService appUserAdminService,
        MunicipalityAdminService municipalityAdminService,
        AdminSettingsService adminSettingsService,
        AdminSesMonitoringService adminSesMonitoringService,
        AppIncidentService appIncidentService,
        CurrentAppUserService currentAppUserService,
        @Value("${checkpol.security.bootstrap-admin.username}") String bootstrapAdminUsername,
        @Value("${checkpol.security.bootstrap-admin.display-name}") String bootstrapAdminDisplayName,
        @Value("${checkpol.municipality.catalog.import-on-startup:false}") boolean startupImportEnabled,
        @Value("${checkpol.municipality.catalog.source:classpath-csv}") String startupImportSource,
        @Value("${checkpol.municipality.catalog.source-version:example-v1}") String startupImportSourceVersion,
        @Value("${checkpol.municipality.admin.verification.enabled:false}") boolean verificationEnabled,
        @Value("${checkpol.municipality.admin.verification.cron:0 0 6 * * *}") String verificationCron,
        @Value("${checkpol.municipality.admin.verification.zone:Europe/Madrid}") String verificationZone,
        @Value("${checkpol.municipality.admin.verification.triggered-by:system-verifier}") String verificationTriggeredBy
    ) {
        this.appUserAdminService = appUserAdminService;
        this.municipalityAdminService = municipalityAdminService;
        this.adminSettingsService = adminSettingsService;
        this.adminSesMonitoringService = adminSesMonitoringService;
        this.appIncidentService = appIncidentService;
        this.currentAppUserService = currentAppUserService;
        this.adminSettingsSummary = new AdminSettingsSummary(
            bootstrapAdminUsername,
            bootstrapAdminDisplayName,
            startupImportEnabled,
            startupImportSource,
            startupImportSourceVersion,
            verificationEnabled,
            verificationCron,
            verificationZone,
            verificationTriggeredBy
        );
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        List<AdminUserSummary> users = appUserAdminService.findAllUsers();
        MunicipalityAdminService.DashboardSummary dashboard = municipalityAdminService.getDashboardSummary();
        AdminSesMonitoringService.SesDashboardSummary sesSummary = adminSesMonitoringService.getDashboardSummary();
        AppIncidentService.IncidentDashboardSummary incidentSummary = appIncidentService.getDashboardSummary();
        populateUserMetrics(model, users);
        populateAdminChrome(model, dashboard);
        model.addAttribute("sesSummary", sesSummary);
        model.addAttribute("sesProblemCount", sesProblemCount(sesSummary));
        model.addAttribute("incidentSummary", incidentSummary);
        model.addAttribute("adminAlertCount", ((Number) model.asMap().get("adminAlertCount")).longValue() + sesProblemCount(sesSummary) + incidentSummary.activeCount());
        model.addAttribute("users", users);
        return "admin/index";
    }

    @GetMapping("/admin/users")
    public String listUsers(Model model) {
        List<AdminUserSummary> users = appUserAdminService.findAllUsers();
        populateUserMetrics(model, users);
        populateAdminChrome(model, municipalityAdminService.getDashboardSummary());
        model.addAttribute("users", users);
        return "admin/users";
    }

    @GetMapping("/admin/verifications")
    public String listVerifications(Model model) {
        return populateOperationsPage(
            model,
            "Verificaciones",
            "Historial de verificaciones de fuentes oficiales",
            "admin/operations",
            "verifications",
            municipalityAdminService.getRecentVerifications()
        );
    }

    @GetMapping("/admin/imports")
    public String listImports(Model model) {
        return populateOperationsPage(
            model,
            "Importaciones",
            "Historial de importaciones aplicadas a base de datos",
            "admin/operations",
            "imports",
            municipalityAdminService.getRecentImports()
        );
    }

    @GetMapping("/admin/activity")
    public String listActivity(Model model) {
        return populateOperationsPage(
            model,
            "Actividad reciente",
            "Timeline operativo del área administrativa",
            "admin/operations",
            "activity",
            municipalityAdminService.getRecentActivity()
        );
    }

    @GetMapping("/admin/ses")
    public String sesDashboard(
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "problemOnly", defaultValue = "false") boolean problemOnly,
        Model model
    ) {
        List<AdminUserSummary> users = appUserAdminService.findAllUsers();
        populateUserMetrics(model, users);
        populateAdminChrome(model, municipalityAdminService.getDashboardSummary());
        AdminSesMonitoringService.SesDashboardSummary sesSummary = adminSesMonitoringService.getDashboardSummary();
        model.addAttribute("activeSection", "ses");
        model.addAttribute("pageTitle", "Control SES");
        model.addAttribute("sesSummary", sesSummary);
        model.addAttribute("sesProblemCount", sesProblemCount(sesSummary));
        model.addAttribute("sesRows", adminSesMonitoringService.findRecentCommunications(status, problemOnly));
        model.addAttribute("selectedStatus", status == null || status.isBlank() ? "ALL" : status);
        model.addAttribute("problemOnly", problemOnly);
        return "admin/ses";
    }

    @GetMapping("/admin/ses/communications/{communicationId}")
    public String sesCommunicationDetail(
        @PathVariable Long communicationId,
        Model model
    ) {
        List<AdminUserSummary> users = appUserAdminService.findAllUsers();
        populateUserMetrics(model, users);
        populateAdminChrome(model, municipalityAdminService.getDashboardSummary());
        model.addAttribute("activeSection", "ses");
        model.addAttribute("pageTitle", "Trazabilidad SES");
        model.addAttribute("sesSummary", adminSesMonitoringService.getDashboardSummary());
        model.addAttribute("detail", adminSesMonitoringService.getCommunicationDetail(communicationId));
        return "admin/ses-detail";
    }

    @GetMapping("/admin/ses/communications/{communicationId}.xml")
    public ResponseEntity<String> downloadAdminCommunicationXml(@PathVariable Long communicationId) {
        String xml = adminSesMonitoringService.getCommunicationXml(communicationId);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename("checkpol-comunicacion-ses-" + communicationId + ".xml")
                .build()
                .toString())
            .body(xml);
    }

    @GetMapping("/admin/incidents")
    public String incidents(
        @RequestParam(name = "status", defaultValue = "ACTIVE") String status,
        Model model
    ) {
        List<AdminUserSummary> users = appUserAdminService.findAllUsers();
        populateUserMetrics(model, users);
        populateAdminChrome(model, municipalityAdminService.getDashboardSummary());
        model.addAttribute("activeSection", "incidents");
        model.addAttribute("pageTitle", "Incidencias");
        model.addAttribute("incidentSummary", appIncidentService.getDashboardSummary());
        model.addAttribute("incidents", appIncidentService.findRecentIncidents(status));
        model.addAttribute("selectedStatus", status == null || status.isBlank() ? "ACTIVE" : status);
        return "admin/incidents";
    }

    @PostMapping("/admin/incidents/{incidentId}/review")
    public String reviewIncident(
        @PathVariable Long incidentId,
        @RequestParam(name = "status", defaultValue = "ACTIVE") String status,
        RedirectAttributes redirectAttributes
    ) {
        try {
            appIncidentService.markReviewed(incidentId);
            redirectAttributes.addFlashAttribute("flashMessage", "Incidencia marcada como revisada.");
            redirectAttributes.addFlashAttribute("flashKind", "success");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("flashMessage", exception.getMessage());
            redirectAttributes.addFlashAttribute("flashKind", "error");
        }
        return "redirect:/admin/incidents?status=" + status;
    }

    @PostMapping("/admin/incidents/{incidentId}/resolve")
    public String resolveIncident(
        @PathVariable Long incidentId,
        @RequestParam(name = "status", defaultValue = "ACTIVE") String status,
        RedirectAttributes redirectAttributes
    ) {
        try {
            appIncidentService.markResolved(incidentId);
            redirectAttributes.addFlashAttribute("flashMessage", "Incidencia resuelta.");
            redirectAttributes.addFlashAttribute("flashKind", "success");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("flashMessage", exception.getMessage());
            redirectAttributes.addFlashAttribute("flashKind", "error");
        }
        return "redirect:/admin/incidents?status=" + status;
    }

    @GetMapping("/admin/ses/owners")
    public String sesOwners(Model model) {
        List<AdminUserSummary> users = appUserAdminService.findAllUsers();
        populateUserMetrics(model, users);
        populateAdminChrome(model, municipalityAdminService.getDashboardSummary());
        model.addAttribute("activeSection", "ses");
        model.addAttribute("pageTitle", "Owners y SES");
        model.addAttribute("sesSummary", adminSesMonitoringService.getDashboardSummary());
        model.addAttribute("ownerRows", adminSesMonitoringService.findOwnerRows());
        return "admin/ses-owners";
    }

    @PostMapping("/admin/ses/owners/{ownerId}/test")
    public String testOwnerSesConnection(
        @PathVariable Long ownerId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            SesConnectionTestResult result = appUserAdminService.testOwnerSesConnection(ownerId);
            redirectAttributes.addFlashAttribute("flashMessage", result.adminMessage());
            redirectAttributes.addFlashAttribute("flashKind", flashKindForAdmin(result));
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("flashMessage", exception.getMessage());
            redirectAttributes.addFlashAttribute("flashKind", "error");
        }
        return "redirect:/admin/ses/owners";
    }

    @PostMapping("/admin/ses/communications/{communicationId}/refresh")
    public String refreshSesCommunication(
        @PathVariable Long communicationId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            SesLoteStatusResult result = adminSesMonitoringService.refreshCommunication(communicationId);
            if (result.communicationCode() != null && !result.communicationCode().isBlank()) {
                redirectAttributes.addFlashAttribute("flashMessage", "SES ha confirmado la comunicación. Código: " + result.communicationCode());
                redirectAttributes.addFlashAttribute("flashKind", "success");
            } else if (result.responseCode() != 0) {
                redirectAttributes.addFlashAttribute("flashMessage", result.responseDescription());
                redirectAttributes.addFlashAttribute("flashKind", "error");
            } else {
                redirectAttributes.addFlashAttribute("flashMessage", "Lote consultado. SES todavía no ha devuelto un código final.");
                redirectAttributes.addFlashAttribute("flashKind", "success");
            }
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("flashMessage", exception.getMessage());
            redirectAttributes.addFlashAttribute("flashKind", "error");
        }
        return "redirect:/admin/ses";
    }

    @PostMapping("/admin/ses/communications/{communicationId}/retry")
    public String retrySesCommunication(
        @PathVariable Long communicationId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            var retry = adminSesMonitoringService.retryCommunication(communicationId);
            if (retry.getDispatchStatus() == es.checkpol.domain.CommunicationDispatchStatus.SUBMITTED_TO_SES) {
                redirectAttributes.addFlashAttribute("flashMessage", "Se ha lanzado un nuevo envío a SES desde admin.");
                redirectAttributes.addFlashAttribute("flashKind", "success");
            } else {
                redirectAttributes.addFlashAttribute("flashMessage", retry.getSesResponseDescription());
                redirectAttributes.addFlashAttribute("flashKind", "error");
            }
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("flashMessage", exception.getMessage());
            redirectAttributes.addFlashAttribute("flashKind", "error");
        }
        return "redirect:/admin/ses";
    }

    @PostMapping("/admin/ses/communications/{communicationId}/review")
    public String reviewSesCommunicationProblem(
        @PathVariable Long communicationId,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "problemOnly", defaultValue = "false") boolean problemOnly,
        Authentication authentication,
        RedirectAttributes redirectAttributes
    ) {
        try {
            adminSesMonitoringService.markCommunicationProblemReviewed(
                communicationId,
                currentAdminIdentity(authentication).username()
            );
            redirectAttributes.addFlashAttribute("flashMessage", "Incidencia SES marcada como revisada. Se mantiene en histórico, pero ya no cuenta como activa.");
            redirectAttributes.addFlashAttribute("flashKind", "success");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("flashMessage", exception.getMessage());
            redirectAttributes.addFlashAttribute("flashKind", "error");
        }
        String selectedStatus = status == null || status.isBlank() ? "ALL" : status;
        return "redirect:/admin/ses?status=" + selectedStatus + "&problemOnly=" + problemOnly;
    }

    @GetMapping("/admin/settings")
    public String settings(Model model) {
        return populateSettingsPage(model, null, null);
    }

    @PostMapping("/admin/settings/catalog-defaults")
    public String updateCatalogDefaults(
        @Valid @ModelAttribute("catalogDefaultsForm") AdminCatalogDefaultsForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return populateSettingsPage(model, form, null);
        }

        adminSettingsService.updateMunicipalityAdminDefaults(form, currentAppUserService.requireAuthenticatedUser().getUsername());
        redirectAttributes.addFlashAttribute("flashMessage", "Valores por defecto del catálogo actualizados.");
        redirectAttributes.addFlashAttribute("flashKind", "success");
        return "redirect:/admin/settings";
    }

    @PostMapping("/admin/settings/verification")
    public String updateVerificationSettings(
        @Valid @ModelAttribute("verificationSettingsForm") AdminVerificationSettingsForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        validateVerificationSettings(form, bindingResult);
        if (bindingResult.hasErrors()) {
            return populateSettingsPage(model, null, form);
        }

        adminSettingsService.updateVerificationSettings(form, currentAppUserService.requireAuthenticatedUser().getUsername());
        redirectAttributes.addFlashAttribute("flashMessage", "Configuración de verificación automática actualizada.");
        redirectAttributes.addFlashAttribute("flashKind", "success");
        return "redirect:/admin/settings";
    }

    private String populateSettingsPage(
        Model model,
        AdminCatalogDefaultsForm catalogFormOverride,
        AdminVerificationSettingsForm verificationFormOverride
    ) {
        List<AdminUserSummary> users = appUserAdminService.findAllUsers();
        populateUserMetrics(model, users);
        populateAdminChrome(model, municipalityAdminService.getDashboardSummary());
        model.addAttribute("activeSection", "settings");
        model.addAttribute("settings", adminSettingsSummary);
        AdminSettingsService.MunicipalityAdminDefaults catalogDefaults = adminSettingsService.getMunicipalityAdminDefaults();
        AdminSettingsService.VerificationSettings verificationSettings = adminSettingsService.getVerificationSettings();
        model.addAttribute("catalogDefaults", catalogDefaults);
        model.addAttribute("verificationSettings", verificationSettings);
        if (!model.containsAttribute("catalogDefaultsForm")) {
            AdminCatalogDefaultsForm form = catalogFormOverride != null
                ? catalogFormOverride
                : new AdminCatalogDefaultsForm(
                    catalogDefaults.source(),
                    catalogDefaults.municipalitiesUrl(),
                    catalogDefaults.postalMappingsUrl()
                );
            model.addAttribute("catalogDefaultsForm", form);
        }
        if (!model.containsAttribute("verificationSettingsForm")) {
            AdminVerificationSettingsForm form = verificationFormOverride != null
                ? verificationFormOverride
                : new AdminVerificationSettingsForm(
                    verificationSettings.enabled(),
                    verificationSettings.cron(),
                    verificationSettings.zone(),
                    verificationSettings.triggeredByUsername()
                );
            model.addAttribute("verificationSettingsForm", form);
        }
        return "admin/settings";
    }

    private void validateVerificationSettings(AdminVerificationSettingsForm form, BindingResult bindingResult) {
        try {
            CronExpression.parse(form.cron());
        } catch (IllegalArgumentException exception) {
            bindingResult.rejectValue("cron", "verification.cron.invalid", "La expresión cron no es válida.");
        }

        try {
            ZoneId.of(form.zone());
        } catch (RuntimeException exception) {
            bindingResult.rejectValue("zone", "verification.zone.invalid", "La zona horaria no es válida.");
        }
    }

    @GetMapping("/admin/users/new")
    public String newUser(Model model) {
        return populateForm(model, new AdminUserForm(), "/admin/users", "Nuevo usuario", "Crear usuario", false, false);
    }

    @PostMapping("/admin/users")
    public String createUser(
        @Valid @ModelAttribute("adminUserForm") AdminUserForm form,
        BindingResult bindingResult,
        @RequestParam(name = "afterSaveAction", required = false) String afterSaveAction,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return populateForm(model, form, "/admin/users", "Nuevo usuario", "Crear usuario", false, false);
        }

        try {
            var user = appUserAdminService.createUser(form);
            if ("test".equals(afterSaveAction) && user.getRole() == AppUserRole.OWNER) {
                SesConnectionTestResult result = appUserAdminService.testOwnerSesConnection(user.getId());
                redirectAttributes.addFlashAttribute("flashMessage", result.adminMessage());
                redirectAttributes.addFlashAttribute("flashKind", flashKindForAdmin(result));
                return "redirect:/admin/users/" + user.getId() + "/edit";
            }
            applyUserSavedFlash(user, redirectAttributes, "Usuario creado correctamente.");
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("user.invalid", exception.getMessage());
            return populateForm(model, form, "/admin/users", "Nuevo usuario", "Crear usuario", false, false);
        }

        return "redirect:/admin/users";
    }

    @GetMapping("/admin/users/{userId}/edit")
    public String editUser(@PathVariable Long userId, Model model) {
        var user = appUserAdminService.getUserEntity(userId);
        return populateForm(
            model,
            appUserAdminService.getUserForm(userId),
            "/admin/users/" + userId,
            "Editar usuario",
            "Guardar cambios",
            user
        );
    }

    @PostMapping("/admin/users/{userId}")
    public String updateUser(
        @PathVariable Long userId,
        @Valid @ModelAttribute("adminUserForm") AdminUserForm form,
        BindingResult bindingResult,
        @RequestParam(name = "afterSaveAction", required = false) String afterSaveAction,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            var user = appUserAdminService.getUserEntity(userId);
            return populateForm(
                model,
                form,
                "/admin/users/" + userId,
                "Editar usuario",
                "Guardar cambios",
                user
            );
        }

        try {
            var user = appUserAdminService.updateUser(userId, form);
            if ("test".equals(afterSaveAction) && user.getRole() == AppUserRole.OWNER) {
                SesConnectionTestResult result = appUserAdminService.testOwnerSesConnection(user.getId());
                redirectAttributes.addFlashAttribute("flashMessage", result.adminMessage());
                redirectAttributes.addFlashAttribute("flashKind", flashKindForAdmin(result));
                return "redirect:/admin/users/" + user.getId() + "/edit";
            }
            applyUserSavedFlash(user, redirectAttributes, "Usuario actualizado correctamente.");
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("user.invalid", exception.getMessage());
            var user = appUserAdminService.getUserEntity(userId);
            return populateForm(
                model,
                form,
                "/admin/users/" + userId,
                "Editar usuario",
                "Guardar cambios",
                user
            );
        }

        return "redirect:/admin/users";
    }

    @GetMapping("/admin/users/{userId}/password")
    public String editUserPassword(@PathVariable Long userId, Model model, Authentication authentication) {
        var user = appUserAdminService.getUserEntity(userId);
        CurrentAdminIdentity currentAdmin = currentAdminIdentity(authentication);
        return populatePasswordForm(model, user, currentAdmin, new AdminPasswordForm());
    }

    @PostMapping("/admin/users/{userId}/password")
    public String updateUserPassword(
        @PathVariable Long userId,
        @Valid @ModelAttribute("adminPasswordForm") AdminPasswordForm form,
        BindingResult bindingResult,
        Model model,
        Authentication authentication,
        RedirectAttributes redirectAttributes
    ) {
        var user = appUserAdminService.getUserEntity(userId);
        CurrentAdminIdentity currentAdmin = currentAdminIdentity(authentication);
        if (bindingResult.hasErrors()) {
            return populatePasswordForm(model, user, currentAdmin, form);
        }

        try {
            appUserAdminService.changePassword(userId, form, currentAdmin.userId(), currentAdmin.username());
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("password.invalid", exception.getMessage());
            return populatePasswordForm(model, user, currentAdmin, form);
        }

        redirectAttributes.addFlashAttribute("flashMessage", "Contraseña actualizada correctamente.");
        redirectAttributes.addFlashAttribute("flashKind", "success");
        return "redirect:/admin/users/" + userId + "/edit";
    }

    private String populateForm(Model model, AdminUserForm form, String action, String title, String submitLabel, boolean hasStoredSesPassword, boolean editing) {
        AppUserRole role = form.role() == null ? AppUserRole.OWNER : form.role();
        boolean ownerRole = role == AppUserRole.OWNER;
        boolean hasSesArrendadorCode = hasText(form.sesArrendadorCode());
        boolean hasSesUsername = hasText(form.sesWsUsername());
        boolean hasVisibleSesPassword = hasText(form.sesWsPassword());
        boolean sesReady = hasSesArrendadorCode && hasSesUsername && (hasVisibleSesPassword || hasStoredSesPassword);
        boolean sesManualOnly = !hasSesArrendadorCode && !hasSesUsername && !hasVisibleSesPassword && !hasStoredSesPassword;

        model.addAttribute("adminUserForm", form);
        model.addAttribute("formAction", action);
        model.addAttribute("formTitle", title);
        model.addAttribute("submitLabel", submitLabel);
        model.addAttribute("editing", editing);
        model.addAttribute("userRoles", AppUserRole.values());
        model.addAttribute("ownerRole", ownerRole);
        model.addAttribute("sesReady", sesReady);
        model.addAttribute("sesManualOnly", sesManualOnly);
        model.addAttribute("sesPasswordStored", hasStoredSesPassword);
        if (!model.containsAttribute("connectionTestStatus")) {
            model.addAttribute("connectionTestStatus", es.checkpol.domain.SesConnectionTestStatus.NOT_TESTED);
        }
        populateAdminChrome(model, municipalityAdminService.getDashboardSummary());
        return "admin/user-form";
    }

    private String populateForm(Model model, AdminUserForm form, String action, String title, String submitLabel, es.checkpol.domain.AppUser user) {
        model.addAttribute("editedUser", user);
        model.addAttribute("connectionTestStatus", user.getSesConnectionTestStatus() == null ? es.checkpol.domain.SesConnectionTestStatus.NOT_TESTED : user.getSesConnectionTestStatus());
        model.addAttribute("connectionTestedAt", user.getSesConnectionTestedAt());
        model.addAttribute("connectionOwnerMessage", user.getSesConnectionOwnerMessage());
        model.addAttribute("connectionAdminMessage", user.getSesConnectionAdminMessage());
        model.addAttribute("connectionTestEndpoint", user.getSesConnectionTestEndpoint());
        model.addAttribute("connectionTestHttpStatus", user.getSesConnectionTestHttpStatus());
        model.addAttribute("connectionTestErrorType", user.getSesConnectionTestErrorType());
        model.addAttribute("connectionTestRawDetail", user.getSesConnectionTestRawDetail());
        return populateForm(model, form, action, title, submitLabel, user.getSesWsPasswordEncrypted() != null && !user.getSesWsPasswordEncrypted().isBlank(), true);
    }

    private String populatePasswordForm(Model model, es.checkpol.domain.AppUser user, CurrentAdminIdentity currentAdmin, AdminPasswordForm form) {
        model.addAttribute("adminPasswordForm", form);
        model.addAttribute("editedUser", user);
        model.addAttribute("selfPasswordChange", isCurrentUser(user, currentAdmin));
        model.addAttribute("formTitle", "Cambiar contraseña");
        model.addAttribute("passwordAction", "/admin/users/" + user.getId() + "/password");
        populateAdminChrome(model, municipalityAdminService.getDashboardSummary());
        return "admin/user-password";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void applyUserSavedFlash(es.checkpol.domain.AppUser user, RedirectAttributes redirectAttributes, String successMessage) {
        if (user.getRole() != AppUserRole.OWNER || user.hasSesWebServiceConfiguration()) {
            redirectAttributes.addFlashAttribute("flashMessage", successMessage);
            redirectAttributes.addFlashAttribute("flashKind", "success");
            return;
        }

        redirectAttributes.addFlashAttribute(
            "flashMessage",
            successMessage + " No se podrá hacer el envío automático porque faltan datos de SES. Este owner trabajará en modo manual: descargar XML y presentarlo en la web de SES."
        );
        redirectAttributes.addFlashAttribute("flashKind", "warning");
    }

    private String flashKindForAdmin(SesConnectionTestResult result) {
        if (result.success()) {
            return "success";
        }
        return result.technicalIssue() ? "error" : "warning";
    }

    private CurrentAdminIdentity currentAdminIdentity(Authentication authentication) {
        try {
            AppUserPrincipal principal = currentAppUserService.requireAuthenticatedUser();
            if (principal != null) {
                return new CurrentAdminIdentity(principal.getId(), principal.getUsername());
            }
        } catch (RuntimeException ignored) {
            // WebMvc tests and some security fallbacks use a non-persisted principal.
        }
        return new CurrentAdminIdentity(null, authentication == null ? null : authentication.getName());
    }

    private boolean isCurrentUser(es.checkpol.domain.AppUser user, CurrentAdminIdentity currentAdmin) {
        return (currentAdmin.userId() != null && currentAdmin.userId().equals(user.getId()))
            || (currentAdmin.username() != null && currentAdmin.username().equals(user.getUsername()));
    }

    private record CurrentAdminIdentity(Long userId, String username) {
    }

    private String populateOperationsPage(
        Model model,
        String pageTitle,
        String pageSubtitle,
        String viewName,
        String activeSection,
        List<MunicipalityAdminService.ImportHistoryItem> items
    ) {
        List<AdminUserSummary> users = appUserAdminService.findAllUsers();
        populateUserMetrics(model, users);
        populateAdminChrome(model, municipalityAdminService.getDashboardSummary());
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("pageSubtitle", pageSubtitle);
        model.addAttribute("activeSection", activeSection);
        model.addAttribute("items", items);
        return viewName;
    }

    private void populateUserMetrics(Model model, List<AdminUserSummary> users) {
        long activeUserCount = users.stream().filter(AdminUserSummary::active).count();
        long ownerCount = users.stream().filter(user -> user.role() == AppUserRole.OWNER).count();
        long activeOwnerCount = users.stream().filter(user -> user.role() == AppUserRole.OWNER && user.active()).count();
        model.addAttribute("userCount", users.size());
        model.addAttribute("ownerCount", ownerCount);
        model.addAttribute("activeUserCount", activeUserCount);
        model.addAttribute("inactiveUserCount", users.size() - activeUserCount);
        model.addAttribute("activeOwnerCount", activeOwnerCount);
        model.addAttribute("inactiveOwnerCount", ownerCount - activeOwnerCount);
        model.addAttribute("ownerActivePercent", ownerCount == 0 ? 0 : Math.toIntExact((activeOwnerCount * 100) / ownerCount));
    }

    private void populateAdminChrome(Model model, MunicipalityAdminService.DashboardSummary dashboard) {
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("sourceHealth", dashboard.sourceHealth());
        model.addAttribute("catalogReadyPercent", dashboard.catalogLoaded() ? 100 : 0);
        model.addAttribute("sourceHealthPercent", switch (dashboard.sourceHealth().level()) {
            case "ok" -> 100;
            case "warning" -> 65;
            case "error" -> 20;
            default -> 0;
        });
        int alertCount = 0;
        if (!dashboard.catalogLoaded()) {
            alertCount++;
        }
        if (!"ok".equals(dashboard.sourceHealth().level())) {
            alertCount++;
        }
        model.addAttribute("adminAlertCount", alertCount);
    }

    private long sesProblemCount(AdminSesMonitoringService.SesDashboardSummary summary) {
        return summary.submissionErrorCount() + summary.processingErrorCount();
    }

    public record AdminSettingsSummary(
        String bootstrapAdminUsername,
        String bootstrapAdminDisplayName,
        boolean startupImportEnabled,
        String startupImportSource,
        String startupImportSourceVersion,
        boolean verificationEnabled,
        String verificationCron,
        String verificationZone,
        String verificationTriggeredBy
    ) {
    }
}
