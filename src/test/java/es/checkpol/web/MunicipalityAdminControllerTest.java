package es.checkpol.web;

import es.checkpol.config.SecurityConfig;
import es.checkpol.service.AppUserPrincipal;
import es.checkpol.service.CurrentAppUserService;
import es.checkpol.service.MunicipalityAdminService;
import es.checkpol.service.MunicipalityCatalogImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = MunicipalityAdminController.class)
@Import(SecurityConfig.class)
class MunicipalityAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MunicipalityAdminService municipalityAdminService;

    @MockitoBean
    private CurrentAppUserService currentAppUserService;

    private AppUserPrincipal adminPrincipal() {
        return mock(AppUserPrincipal.class, invocation -> {
            if ("getUsername".equals(invocation.getMethod().getName())) {
                return "admin";
            }
            return null;
        });
    }

    @Test
    @WithMockUser(username = "admin", roles = "SUPER_ADMIN")
    void showsMunicipalitiesDashboard() throws Exception {
        when(currentAppUserService.requireAuthenticatedUser()).thenReturn(adminPrincipal());
        when(municipalityAdminService.findResumablePreview("admin")).thenReturn(java.util.Optional.empty());
        when(municipalityAdminService.defaultForm()).thenReturn(new AdminMunicipalityImportForm(
            "https://www.ine.es/daco/daco42/codmun/diccionario26.xlsx",
            "https://www.ine.es/prodyser/callejero/caj_esp/caj_esp_072025.zip",
            "ine-open-data",
            ""
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

        mockMvc.perform(get("/admin/municipalities"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/municipalities"))
            .andExpect(model().attributeExists("municipalityImportForm"))
            .andExpect(model().attributeExists("dashboard"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "SUPER_ADMIN")
    void previewsMunicipalityImport() throws Exception {
        when(currentAppUserService.requireAuthenticatedUser()).thenReturn(adminPrincipal());
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
        when(municipalityAdminService.previewImport(any(), eq("admin"))).thenReturn(new MunicipalityCatalogImportService.PreviewSummary(
            2,
            2,
            2,
            0,
            0,
            2,
            0,
            0,
            "ine-open-data",
            "2026-01",
            List.of(new MunicipalityCatalogImportService.MunicipalityRow("28", "Madrid", "28079", "Madrid")),
            List.of(new MunicipalityCatalogImportService.PostalMappingRow("28001", "28079"))
        ));

        mockMvc.perform(post("/admin/municipalities/preview")
                .with(csrf())
                .param("municipalitiesUrl", "https://www.ine.es/daco/daco42/codmun/diccionario26.xlsx")
                .param("postalMappingsUrl", "https://www.ine.es/prodyser/callejero/caj_esp/caj_esp_072025.zip")
                .param("source", "ine-open-data")
                .param("sourceVersion", "2026-01"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/municipalities"))
            .andExpect(model().attributeExists("preview"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "SUPER_ADMIN")
    void verifiesMunicipalitySources() throws Exception {
        when(currentAppUserService.requireAuthenticatedUser()).thenReturn(adminPrincipal());
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
        when(municipalityAdminService.verifySources(any(), eq("admin"))).thenReturn(new MunicipalityAdminService.VerificationSummary(
            "ok",
            "Fuentes oficiales verificadas.",
            8110,
            23000,
            List.of()
        ));

        mockMvc.perform(post("/admin/municipalities/verify")
                .with(csrf())
                .param("municipalitiesUrl", "https://www.ine.es/daco/daco42/codmun/diccionario26.xlsx")
                .param("postalMappingsUrl", "https://www.ine.es/prodyser/callejero/caj_esp/caj_esp_072025.zip")
                .param("source", "ine-open-data")
                .param("sourceVersion", "2026-01"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/municipalities"))
            .andExpect(model().attributeExists("verification"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "SUPER_ADMIN")
    void importsMunicipalityCatalog() throws Exception {
        when(currentAppUserService.requireAuthenticatedUser()).thenReturn(adminPrincipal());
        when(municipalityAdminService.importCatalog(any(), eq("admin"))).thenReturn(new MunicipalityCatalogImportService.ImportSummary(
            2,
            0,
            2,
            0,
            "ine-open-data",
            "2026-01"
        ));

        mockMvc.perform(post("/admin/municipalities/import")
                .with(csrf())
                .param("municipalitiesUrl", "https://www.ine.es/daco/daco42/codmun/diccionario26.xlsx")
                .param("postalMappingsUrl", "https://www.ine.es/prodyser/callejero/caj_esp/caj_esp_072025.zip")
                .param("source", "ine-open-data")
                .param("sourceVersion", "2026-01"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/municipalities"));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void blocksOwnerAccessToMunicipalitiesAdmin() throws Exception {
        mockMvc.perform(get("/admin/municipalities"))
            .andExpect(status().isForbidden());
    }
}
