package es.checkpol.web;

import es.checkpol.config.SecurityConfig;
import es.checkpol.domain.AppUser;
import es.checkpol.domain.AppUserRole;
import es.checkpol.domain.SesConnectionTestStatus;
import es.checkpol.service.OwnerSesSettingsService;
import es.checkpol.service.SesConnectionTestResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = OwnerSesSettingsController.class)
@Import(SecurityConfig.class)
class OwnerSesSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OwnerSesSettingsService ownerSesSettingsService;

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void showsOwnerSesSettingsPage() throws Exception {
        when(ownerSesSettingsService.getCurrentForm()).thenReturn(new OwnerSesSettingsForm("0000260833", "joana-ws", ""));
        when(ownerSesSettingsService.getCurrentOwner()).thenReturn(owner());

        mockMvc.perform(get("/bookings/ses-settings"))
            .andExpect(status().isOk())
            .andExpect(view().name("bookings/ses-settings"))
            .andExpect(model().attributeExists("ownerSesSettingsForm"))
            .andExpect(model().attributeExists("connectionTestStatus"));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void savesAndTestsOwnerSesSettings() throws Exception {
        when(ownerSesSettingsService.saveAndTest(any())).thenReturn(new SesConnectionTestResult(
            SesConnectionTestStatus.OK,
            "Conexión correcta con SES. Ya puedes usar la presentación automática desde Checkpol.",
            "Conexión WS correcta.",
            OffsetDateTime.parse("2026-04-04T18:10:00Z"),
            "https://pre-ses",
            200,
            null,
            null
        ));

        mockMvc.perform(post("/bookings/ses-settings")
                .with(csrf())
                .param("sesArrendadorCode", "0000260833")
                .param("sesWsUsername", "joana-ws")
                .param("sesWsPassword", "secreta")
                .param("afterSaveAction", "test"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/bookings/ses-settings"));
    }

    private AppUser owner() {
        AppUser owner = new AppUser(
            "joana",
            "hash",
            "Joana",
            AppUserRole.OWNER,
            true,
            OffsetDateTime.parse("2026-04-04T10:00:00Z"),
            OffsetDateTime.parse("2026-04-04T10:00:00Z")
        );
        owner.registerSesConnectionTest(
            SesConnectionTestStatus.OK,
            OffsetDateTime.parse("2026-04-04T18:00:00Z"),
            "Conexión correcta con SES. Ya puedes usar la presentación automática desde Checkpol.",
            "Conexión WS correcta.",
            "https://pre-ses",
            200,
            null,
            null
        );
        return owner;
    }
}
