package es.checkpol.web;

import es.checkpol.config.SecurityConfig;
import es.checkpol.domain.AppUserRole;
import es.checkpol.service.AdminUserSummary;
import es.checkpol.service.AdminSettingsService;
import es.checkpol.service.AppUserPrincipal;
import es.checkpol.service.AppUserAdminService;
import es.checkpol.service.CurrentAppUserService;
import es.checkpol.service.MunicipalityAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.time.OffsetDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private CurrentAppUserService currentAppUserService;

    @Test
    @WithMockUser(username = "admin", roles = "SUPER_ADMIN")
    void showsAdminDashboard() throws Exception {
        when(appUserAdminService.findAllUsers()).thenReturn(List.of(
            new AdminUserSummary(1L, "admin", "Administrador", AppUserRole.SUPER_ADMIN, true)
        ));
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
            .andExpect(model().attributeExists("sourceHealth"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "SUPER_ADMIN")
    void createsOwnerUser() throws Exception {
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
            .andExpect(status().isForbidden());
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
