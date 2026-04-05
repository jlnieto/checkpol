package es.checkpol.web;

import es.checkpol.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = AccessDeniedController.class)
@Import(SecurityConfig.class)
class AccessDeniedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void showsOwnerRecoveryAction() throws Exception {
        mockMvc.perform(get("/access-denied").requestAttr("jakarta.servlet.forward.request_uri", "/admin"))
            .andExpect(status().isForbidden())
            .andExpect(view().name("access-denied"))
            .andExpect(model().attribute("actionUrl", "/bookings"))
            .andExpect(content().string(containsString("Página no disponible")))
            .andExpect(content().string(containsString("Volver a estancias")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "SUPER_ADMIN")
    void showsAdminRecoveryAction() throws Exception {
        mockMvc.perform(get("/access-denied").requestAttr("jakarta.servlet.forward.request_uri", "/bookings"))
            .andExpect(status().isForbidden())
            .andExpect(view().name("access-denied"))
            .andExpect(model().attribute("actionUrl", "/admin"))
            .andExpect(content().string(containsString("Esta pantalla no pertenece al área admin")))
            .andExpect(content().string(containsString("Volver al panel admin")));
    }
}
