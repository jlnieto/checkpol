package es.checkpol.web;

import es.checkpol.config.SecurityConfig;
import es.checkpol.service.AccommodationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = AccommodationController.class)
@Import(SecurityConfig.class)
class AccommodationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccommodationService accommodationService;

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void showsRequiredAndOptionalAccommodationSections() throws Exception {
        mockMvc.perform(get("/accommodations/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("accommodations/form"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Obligatorio para guardar")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Código SES del establecimiento *")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Opcional por ahora")));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void keepsFormWhenRequiredFieldsAreMissing() throws Exception {
        mockMvc.perform(post("/accommodations")
                .with(csrf())
                .param("name", "")
                .param("sesEstablishmentCode", "")
                .param("registrationNumber", "")
                .param("roomCount", ""))
            .andExpect(status().isOk())
            .andExpect(view().name("accommodations/form"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Indica un nombre para la vivienda.")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Indica el codigo SES de la vivienda.")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("El codigo SES debe tener exactamente 10 caracteres."))));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void rejectsSesCodeWithWrongLength() throws Exception {
        mockMvc.perform(post("/accommodations")
                .with(csrf())
                .param("name", "Casa del Puerto")
                .param("sesEstablishmentCode", "H123")
                .param("registrationNumber", "")
                .param("roomCount", ""))
            .andExpect(status().isOk())
            .andExpect(view().name("accommodations/form"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("El codigo SES debe tener exactamente 10 caracteres.")));
    }
}
