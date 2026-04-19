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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = HomeController.class)
@Import(SecurityConfig.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void showsPublicHomeToAnonymousVisitors() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(view().name("public/home"))
            .andExpect(content().string(containsString("Que tus huéspedes lleguen sin el trámite pendiente")))
            .andExpect(content().string(containsString("3,90 € al mes por alojamiento")))
            .andExpect(content().string(containsString("Contratación mensual. Sin prueba gratuita.")));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void redirectsOwnerToBookings() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/bookings"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "SUPER_ADMIN")
    void redirectsAdminToAdminArea() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin"));
    }
}
