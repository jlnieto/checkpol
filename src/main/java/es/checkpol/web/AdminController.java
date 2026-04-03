package es.checkpol.web;

import es.checkpol.domain.AppUserRole;
import es.checkpol.service.AdminUserSummary;
import es.checkpol.service.AdminSettingsService;
import es.checkpol.service.AppUserAdminService;
import es.checkpol.service.CurrentAppUserService;
import es.checkpol.service.MunicipalityAdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.time.ZoneId;

@Controller
public class AdminController {

    private final AppUserAdminService appUserAdminService;
    private final MunicipalityAdminService municipalityAdminService;
    private final AdminSettingsService adminSettingsService;
    private final CurrentAppUserService currentAppUserService;
    private final AdminSettingsSummary adminSettingsSummary;

    public AdminController(
        AppUserAdminService appUserAdminService,
        MunicipalityAdminService municipalityAdminService,
        AdminSettingsService adminSettingsService,
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
        populateUserMetrics(model, users);
        populateAdminChrome(model, dashboard);
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
    public String newOwner(Model model) {
        return populateForm(model, new AdminUserForm(), "/admin/users", "Nuevo usuario", "Crear usuario");
    }

    @PostMapping("/admin/users")
    public String createOwner(
        @Valid @ModelAttribute("adminUserForm") AdminUserForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return populateForm(model, form, "/admin/users", "Nuevo usuario", "Crear usuario");
        }

        try {
            appUserAdminService.createOwner(form);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("user.invalid", exception.getMessage());
            return populateForm(model, form, "/admin/users", "Nuevo usuario", "Crear usuario");
        }

        redirectAttributes.addFlashAttribute("flashMessage", "Usuario creado correctamente.");
        redirectAttributes.addFlashAttribute("flashKind", "success");
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/users/{userId}/edit")
    public String editOwner(@PathVariable Long userId, Model model) {
        return populateForm(model, appUserAdminService.getOwnerForm(userId), "/admin/users/" + userId, "Editar usuario", "Guardar cambios");
    }

    @PostMapping("/admin/users/{userId}")
    public String updateOwner(
        @PathVariable Long userId,
        @Valid @ModelAttribute("adminUserForm") AdminUserForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return populateForm(model, form, "/admin/users/" + userId, "Editar usuario", "Guardar cambios");
        }

        try {
            appUserAdminService.updateOwner(userId, form);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("user.invalid", exception.getMessage());
            return populateForm(model, form, "/admin/users/" + userId, "Editar usuario", "Guardar cambios");
        }

        redirectAttributes.addFlashAttribute("flashMessage", "Usuario actualizado correctamente.");
        redirectAttributes.addFlashAttribute("flashKind", "success");
        return "redirect:/admin/users";
    }

    private String populateForm(Model model, AdminUserForm form, String action, String title, String submitLabel) {
        model.addAttribute("adminUserForm", form);
        model.addAttribute("formAction", action);
        model.addAttribute("formTitle", title);
        model.addAttribute("submitLabel", submitLabel);
        populateAdminChrome(model, municipalityAdminService.getDashboardSummary());
        return "admin/user-form";
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
