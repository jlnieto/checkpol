package es.checkpol.web;

import es.checkpol.config.SecurityConfig;
import es.checkpol.service.MunicipalityAdminDashboard;
import es.checkpol.service.MunicipalityIssueSummary;
import es.checkpol.service.MunicipalityReviewService;
import es.checkpol.service.MunicipalityRuleSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
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
    private MunicipalityReviewService municipalityReviewService;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void showsMunicipalityDashboard() throws Exception {
        when(municipalityReviewService.getDashboard()).thenReturn(sampleDashboard());

        mockMvc.perform(get("/admin/municipalities"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/municipalities"))
            .andExpect(model().attributeExists("dashboard"))
            .andExpect(model().attributeExists("issueCorrectionForm"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void storesManualCorrectionAndRedirects() throws Exception {
        when(municipalityReviewService.getDashboard()).thenReturn(sampleDashboard());

        mockMvc.perform(post("/admin/municipalities/issues/9")
                .with(csrf())
                .param("municipalityCode", "28079")
                .param("municipalityName", "Madrid")
                .param("resolutionNote", "Revisado manualmente"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/municipalities"));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void deniesOwnerAccessToAdminArea() throws Exception {
        mockMvc.perform(get("/admin/municipalities"))
            .andExpect(status().isForbidden());
    }

    private MunicipalityAdminDashboard sampleDashboard() {
        return new MunicipalityAdminDashboard(
            List.of(new MunicipalityIssueSummary(
                9L,
                1L,
                "ABC123",
                "Casa Olivo",
                "Ana Lopez",
                "28001",
                "Madrd",
                "28079",
                "Madrid",
                es.checkpol.domain.MunicipalityResolutionStatus.APPROXIMATED,
                "Municipio aproximado automaticamente a partir del texto indicado.",
                OffsetDateTime.now()
            )),
            List.of(new MunicipalityRuleSummary("ESP", "28", "Madrd", "28079", "Madrid", OffsetDateTime.now()))
        );
    }
}
