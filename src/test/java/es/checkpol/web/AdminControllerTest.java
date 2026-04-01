package es.checkpol.web;

import es.checkpol.config.SecurityConfig;
import es.checkpol.domain.AppUserRole;
import es.checkpol.service.AdminUserSummary;
import es.checkpol.service.AppUserAdminService;
import es.checkpol.service.MunicipalityAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

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

    @Test
    @WithMockUser(username = "admin", roles = "SUPER_ADMIN")
    void showsAdminDashboard() throws Exception {
        when(appUserAdminService.countOwners()).thenReturn(2L);
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
    @WithMockUser(username = "owner", roles = "OWNER")
    void blocksOwnerAccessToAdmin() throws Exception {
        mockMvc.perform(get("/admin"))
            .andExpect(status().isForbidden());
    }
}
