package es.checkpol.web;

import es.checkpol.config.SecurityConfig;
import es.checkpol.domain.Accommodation;
import es.checkpol.service.AccommodationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
    void showsAccommodationListWithClearCreateAndEditActions() throws Exception {
        Accommodation accommodation = new Accommodation("Casa del Puerto", "H123456789", "VT-1234-A", 2);
        ReflectionTestUtils.setField(accommodation, "id", 12L);
        when(accommodationService.findAll()).thenReturn(List.of(accommodation));

        mockMvc.perform(get("/accommodations"))
            .andExpect(status().isOk())
            .andExpect(view().name("accommodations/list"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Tus viviendas")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Crear vivienda")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Casa del Puerto")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Código SES: H123456789")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Editar vivienda")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Registrar estancia"))));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void showsEmptyAccommodationListWithFirstCreateAction() throws Exception {
        when(accommodationService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/accommodations"))
            .andExpect(status().isOk())
            .andExpect(view().name("accommodations/list"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Tus viviendas")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Crear primera vivienda")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Necesitas una vivienda antes de registrar una estancia.")));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void showsRequiredAndOptionalAccommodationSections() throws Exception {
        mockMvc.perform(get("/accommodations/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("accommodations/form"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Obligatorio para guardar")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Código SES del establecimiento *")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Opcional por ahora")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/accommodations\"")));
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
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Indica el código SES de la vivienda.")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("El código SES debe tener exactamente 10 caracteres."))));
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
            .andExpect(content().string(org.hamcrest.Matchers.containsString("El código SES debe tener exactamente 10 caracteres.")));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void redirectsToAccommodationListAfterCreatingAccommodation() throws Exception {
        mockMvc.perform(post("/accommodations")
                .with(csrf())
                .param("name", "Casa del Puerto")
                .param("sesEstablishmentCode", "H123456789")
                .param("registrationNumber", "VT-1234-A")
                .param("roomCount", "2"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/accommodations"));
    }
}
