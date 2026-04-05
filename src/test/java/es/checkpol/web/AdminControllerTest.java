package es.checkpol.web;

import es.checkpol.config.SecurityConfig;
import es.checkpol.domain.AppUserRole;
import es.checkpol.domain.CommunicationDispatchMode;
import es.checkpol.domain.CommunicationDispatchStatus;
import es.checkpol.domain.SesConnectionTestStatus;
import es.checkpol.service.AdminUserSummary;
import es.checkpol.service.AdminSettingsService;
import es.checkpol.service.AdminSesMonitoringService;
import es.checkpol.service.AppUserPrincipal;
import es.checkpol.service.AppUserAdminService;
import es.checkpol.service.CurrentAppUserService;
import es.checkpol.service.MunicipalityAdminService;
import es.checkpol.service.SesConnectionTestResult;
import es.checkpol.service.SesLoteStatusResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserAdminService appUserAdminService;

    @MockitoBean
    private MunicipalityAdminService municipalityAdminService;

    @MockitoBean
    private AdminSettingsService adminSettingsService;

    @MockitoBean
    private AdminSesMonitoringService adminSesMonitoringService;

    @MockitoBean
    private CurrentAppUserService currentAppUserService;

    @Test
    @WithMockUser(username = "admin", roles = "SUPER_ADMIN")
    void showsAdminDashboard() throws Exception {
        when(appUserAdminService.findAllUsers()).thenReturn(List.of(
            new AdminUserSummary(1L, "admin", "Administrador", AppUserRole.SUPER_ADMIN, true)
        ));
        when(adminSesMonitoringService.getDashboardSummary()).thenReturn(sesSummary());
        when(municipalityAdminService.getDashboardSummary()).thenReturn(new MunicipalityAdminService.DashboardSummary(
            false,
            0,
            0,
            null,
            null,
            null,
            new MunicipalityAdminService.SourceHealthSummary("warning", "Fuentes oficiales sin verificar todavía.", "Lanza una verificación desde esta pantalla antes de la próxima importación.", null),
            null,
            List.of()
        ));

        mockMvc.perform(get("/admin"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/index"))
            .andExpect(model().attributeExists("ownerCount"))
            .andExpect(model().attributeExists("users"))
            .andExpect(model().attributeExists("sourceHealth"))
            .andExpect(model().attributeExists("sesSummary"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "SUPER_ADMIN")
    void createsOwnerUser() throws Exception {
        when(appUserAdminService.createOwner(any())).thenReturn(new es.checkpol.domain.AppUser(
            "jose",
            "hash",
            "José",
            AppUserRole.OWNER,
            true,
            OffsetDateTime.parse("2026-04-04T10:00:00Z"),
            OffsetDateTime.parse("2026-04-04T10:00:00Z")
        ));

        mockMvc.perform(post("/admin/users")
                .with(csrf())
                .param("username", "jose")
                .param("displayName", "José")
                .param("password", "secreta")
                .param("active", "true"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/users"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "SUPER_ADMIN")
    void showsVerificationsPage() throws Exception {
        mockDashboardAndUsers();
        when(municipalityAdminService.getRecentVerifications()).thenReturn(List.of(historyItem("VERIFY")));

        mockMvc.perform(get("/admin/verifications"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/operations"))
            .andExpect(model().attribute("activeSection", "verifications"))
            .andExpect(model().attributeExists("items"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "SUPER_ADMIN")
    void showsImportsPage() throws Exception {
        mockDashboardAndUsers();
        when(municipalityAdminService.getRecentImports()).thenReturn(List.of(historyItem("IMPORT")));

        mockMvc.perform(get("/admin/imports"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/operations"))
            .andExpect(model().attribute("activeSection", "imports"))
            .andExpect(model().attributeExists("items"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "SUPER_ADMIN")
    void showsActivityPage() throws Exception {
        mockDashboardAndUsers();
        when(municipalityAdminService.getRecentActivity()).thenReturn(List.of(historyItem("PREVIEW")));

        mockMvc.perform(get("/admin/activity"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/operations"))
            .andExpect(model().attribute("activeSection", "activity"))
            .andExpect(model().attributeExists("items"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "SUPER_ADMIN")
    void showsSettingsPage() throws Exception {
        mockDashboardAndUsers();
        when(adminSettingsService.getMunicipalityAdminDefaults()).thenReturn(defaults());
        when(adminSettingsService.getVerificationSettings()).thenReturn(verificationSettings());

        mockMvc.perform(get("/admin/settings"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/settings"))
            .andExpect(model().attribute("activeSection", "settings"))
            .andExpect(model().attributeExists("settings"))
            .andExpect(model().attributeExists("catalogDefaults"))
            .andExpect(model().attributeExists("catalogDefaultsForm"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "SUPER_ADMIN")
    void showsSesDashboard() throws Exception {
        mockDashboardAndUsers();
        when(adminSesMonitoringService.findRecentCommunications(null, false)).thenReturn(List.of(
            new AdminSesMonitoringService.SesCommunicationRow(
                10L,
                20L,
                30L,
                "Joana",
                "joana",
                "Apartamento Centro",
                "CHK-1",
                2,
                CommunicationDispatchMode.SES_WEB_SERVICE,
                CommunicationDispatchStatus.SUBMITTED_TO_SES,
                "Pendiente SES",
                "mono-pill-warning",
                "Lote ABC pendiente.",
                "ABC",
                null,
                OffsetDateTime.parse("2026-04-04T10:00:00Z"),
                OffsetDateTime.parse("2026-04-04T10:10:00Z"),
                null,
                null,
                true,
                false,
                true
            )
        ));

        mockMvc.perform(get("/admin/ses"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/ses"))
            .andExpect(model().attribute("activeSection", "ses"))
            .andExpect(model().attributeExists("sesRows"))
            .andExpect(model().attributeExists("sesSummary"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "SUPER_ADMIN")
    void showsSesOwnersPage() throws Exception {
        mockDashboardAndUsers();
        when(adminSesMonitoringService.findOwnerRows()).thenReturn(List.of(
            new AdminSesMonitoringService.SesOwnerRow(
                30L,
                "Joana",
                "joana",
                true,
                "Listo para WS",
                "mono-pill-success",
                "Puede presentar automáticamente en SES.",
                "A123456789",
                "joana-ws",
                true,
                SesConnectionTestStatus.OK,
                OffsetDateTime.parse("2026-04-04T10:15:00Z"),
                "Conexión WS correcta.",
                200,
                null,
                OffsetDateTime.parse("2026-04-04T10:20:00Z"),
                null,
                0,
                0
            )
        ));

        mockMvc.perform(get("/admin/ses/owners"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/ses-owners"))
            .andExpect(model().attribute("activeSection", "ses"))
            .andExpect(model().attributeExists("ownerRows"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "SUPER_ADMIN")
    void testsOwnerSesConnectionFromOwnersDashboard() throws Exception {
        when(appUserAdminService.testOwnerSesConnection(30L)).thenReturn(new SesConnectionTestResult(
            SesConnectionTestStatus.OK,
            "Conexión correcta con SES.",
            "Conexión WS correcta.",
            OffsetDateTime.parse("2026-04-04T10:15:00Z"),
            "https://pre-ses",
            200,
            null,
            null
        ));

        mockMvc.perform(post("/admin/ses/owners/30/test").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/ses/owners"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "SUPER_ADMIN")
    void refreshesSesCommunicationFromAdmin() throws Exception {
        when(adminSesMonitoringService.refreshCommunication(10L)).thenReturn(new SesLoteStatusResult(
            0,
            "OK",
            "ABC",
            1,
            "Procesado",
            "COM-1",
            null,
            null,
            OffsetDateTime.parse("2026-04-04T10:30:00Z")
        ));

        mockMvc.perform(post("/admin/ses/communications/10/refresh").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/ses"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "SUPER_ADMIN")
    void retriesSesCommunicationFromAdmin() throws Exception {
        mockMvc.perform(post("/admin/ses/communications/10/retry").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/ses"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "SUPER_ADMIN")
    void updatesCatalogDefaultSettings() throws Exception {
        when(currentAppUserService.requireAuthenticatedUser()).thenReturn(adminPrincipal());

        mockMvc.perform(post("/admin/settings/catalog-defaults")
                .with(csrf())
                .param("source", "ine-open-data")
                .param("municipalitiesUrl", "https://www.ine.es/daco/daco42/codmun/diccionario26.xlsx")
                .param("postalMappingsUrl", "https://www.ine.es/prodyser/callejero/caj_esp/caj_esp_072025.zip"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/settings"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "SUPER_ADMIN")
    void updatesVerificationSettings() throws Exception {
        when(currentAppUserService.requireAuthenticatedUser()).thenReturn(adminPrincipal());

        mockMvc.perform(post("/admin/settings/verification")
                .with(csrf())
                .param("enabled", "true")
                .param("cron", "0 0 6 * * *")
                .param("zone", "Europe/Madrid")
                .param("triggeredByUsername", "system-verifier"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/settings"));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void blocksOwnerAccessToAdmin() throws Exception {
        mockMvc.perform(get("/admin"))
            .andExpect(status().isForbidden())
            .andExpect(forwardedUrl("/access-denied"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "SUPER_ADMIN")
    void logsOutThroughSecurityFilter() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?logout"));
    }

    private void mockDashboardAndUsers() {
        when(appUserAdminService.findAllUsers()).thenReturn(List.of(
            new AdminUserSummary(1L, "admin", "Administrador", AppUserRole.SUPER_ADMIN, true)
        ));
        when(adminSesMonitoringService.getDashboardSummary()).thenReturn(sesSummary());
        when(adminSesMonitoringService.findRecentCommunications(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(List.of());
        when(adminSesMonitoringService.findOwnerRows()).thenReturn(List.of());
        when(adminSettingsService.getMunicipalityAdminDefaults()).thenReturn(defaults());
        when(adminSettingsService.getVerificationSettings()).thenReturn(verificationSettings());
        when(municipalityAdminService.getDashboardSummary()).thenReturn(new MunicipalityAdminService.DashboardSummary(
            true,
            8132,
            13957,
            "ine-open-data",
            "manual-2026-04-03",
            OffsetDateTime.parse("2026-04-03T10:47:16Z"),
            new MunicipalityAdminService.SourceHealthSummary("ok", "Fuentes oficiales verificadas.", "Las fuentes siguen disponibles.", OffsetDateTime.parse("2026-04-03T10:47:16Z")),
            null,
            List.of()
        ));
    }

    private AdminSesMonitoringService.SesDashboardSummary sesSummary() {
        return new AdminSesMonitoringService.SesDashboardSummary(
            2,
            1,
            1,
            3,
            new AdminSesMonitoringService.SesTechnicalHealth(
                "Parcial",
                "mono-pill-warning",
                new AdminSesMonitoringService.SesTechnicalCheck("Endpoint SES", "Configurado", "https://pre-ses", "mono-pill-success"),
                new AdminSesMonitoringService.SesTechnicalCheck("Truststore", "OK", "Cargado", "mono-pill-success"),
                new AdminSesMonitoringService.SesTechnicalCheck("Scheduler SES", "Activo", "Cada 60 s", "mono-pill-success")
            )
        );
    }

    private MunicipalityAdminService.ImportHistoryItem historyItem(String operationType) {
        return new MunicipalityAdminService.ImportHistoryItem(
            1L,
            operationType,
            "ine-open-data",
            "manual-2026-04-03",
            "SUCCESS",
            8132,
            13957,
            OffsetDateTime.parse("2026-04-03T10:47:16Z"),
            "admin",
            null
        );
    }

    private AppUserPrincipal adminPrincipal() {
        return org.mockito.Mockito.mock(AppUserPrincipal.class, invocation -> {
            if ("getUsername".equals(invocation.getMethod().getName())) {
                return "admin";
            }
            return null;
        });
    }

    private AdminSettingsService.MunicipalityAdminDefaults defaults() {
        return new AdminSettingsService.MunicipalityAdminDefaults(
            "ine-open-data",
            "https://www.ine.es/daco/daco42/codmun/diccionario26.xlsx",
            "https://www.ine.es/prodyser/callejero/caj_esp/caj_esp_072025.zip",
            OffsetDateTime.parse("2026-04-03T10:47:16Z"),
            "admin"
        );
    }

    private AdminSettingsService.VerificationSettings verificationSettings() {
        return new AdminSettingsService.VerificationSettings(
            true,
            "0 0 6 * * *",
            "Europe/Madrid",
            "system-verifier",
            OffsetDateTime.parse("2026-04-03T10:47:16Z"),
            "admin"
        );
    }
}
