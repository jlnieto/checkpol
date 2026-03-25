package es.checkpol.web;

import es.checkpol.domain.Accommodation;
import es.checkpol.domain.Booking;
import es.checkpol.domain.BookingChannel;
import es.checkpol.domain.PaymentType;
import es.checkpol.service.GuestSelfServiceDetails;
import es.checkpol.service.GuestSelfServiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = GuestSelfServiceController.class)
class GuestSelfServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GuestSelfServiceService guestSelfServiceService;

    @Test
    void showsPublicGuestForm() throws Exception {
        when(guestSelfServiceService.getByToken("abc")).thenReturn(sampleAccess());

        mockMvc.perform(get("/guest-access/abc"))
            .andExpect(status().isOk())
            .andExpect(view().name("public/guest-form"))
            .andExpect(model().attributeExists("access"))
            .andExpect(model().attributeExists("guestForm"));
    }

    @Test
    void showsPublicGuestEditForm() throws Exception {
        when(guestSelfServiceService.getByToken("abc")).thenReturn(sampleAccess());
        when(guestSelfServiceService.getGuestForm("abc", 7L)).thenReturn(sampleForm());

        mockMvc.perform(get("/guest-access/abc/guests/7/edit"))
            .andExpect(status().isOk())
            .andExpect(view().name("public/guest-form"))
            .andExpect(model().attributeExists("publicEditGuestId"));
    }

    @Test
    void submitsPublicGuestForm() throws Exception {
        when(guestSelfServiceService.getByToken("abc")).thenReturn(sampleAccess());

        mockMvc.perform(post("/guest-access/abc")
                .param("firstName", "Ana")
                .param("lastName1", "Lopez")
                .param("lastName2", "Martin")
                .param("birthDate", LocalDate.now().minusYears(30).toString())
                .param("nationality", "ESP")
                .param("documentType", "NIF")
                .param("documentNumber", "00000000T")
                .param("documentSupport", "SUP123")
                .param("addressLine", "Calle Mayor 1")
                .param("municipalityCode", "28079")
                .param("postalCode", "28001")
                .param("country", "ESP")
                .param("phone", "600000000")
                .param("relationship", "PM"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/guest-access/abc"));
    }

    @Test
    void keepsPublicGuestFormWhenServiceFails() throws Exception {
        when(guestSelfServiceService.getByToken("abc")).thenReturn(sampleAccess());
        doThrow(new IllegalArgumentException("El enlace ha caducado.")).when(guestSelfServiceService).submitGuest(any(), any());

        mockMvc.perform(post("/guest-access/abc")
                .param("firstName", "Ana")
                .param("lastName1", "Lopez")
                .param("lastName2", "Martin")
                .param("birthDate", LocalDate.now().minusYears(30).toString())
                .param("nationality", "ESP")
                .param("documentType", "NIF")
                .param("documentNumber", "00000000T")
                .param("documentSupport", "SUP123")
                .param("addressLine", "Calle Mayor 1")
                .param("municipalityCode", "28079")
                .param("postalCode", "28001")
                .param("country", "ESP")
                .param("phone", "600000000")
                .param("relationship", "PM"))
            .andExpect(status().isOk())
            .andExpect(view().name("public/guest-form"));
    }

    @Test
    void updatesPublicGuestForm() throws Exception {
        when(guestSelfServiceService.getByToken("abc")).thenReturn(sampleAccess());

        mockMvc.perform(post("/guest-access/abc/guests/7")
                .param("firstName", "Ana")
                .param("lastName1", "Lopez")
                .param("lastName2", "Martin")
                .param("birthDate", LocalDate.now().minusYears(30).toString())
                .param("nationality", "ESP")
                .param("documentType", "NIF")
                .param("documentNumber", "00000000T")
                .param("documentSupport", "SUP123")
                .param("addressLine", "Calle Mayor 1")
                .param("municipalityCode", "28079")
                .param("postalCode", "28001")
                .param("country", "ESP")
                .param("phone", "600000000")
                .param("relationship", "PM"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/guest-access/abc"));
    }

    private GuestSelfServiceDetails sampleAccess() {
        Booking booking = new Booking(
            new Accommodation("Casa Olivo", "H123456789", "VT-123"),
            "ABC123",
            LocalDate.now(),
            BookingChannel.DIRECT,
            LocalDate.now().plusDays(2),
            LocalDate.now().plusDays(4),
            PaymentType.EFECT,
            LocalDate.now(),
            null,
            null,
            null
        );
        booking.updateSelfServiceAccess("abc", OffsetDateTime.now().plusDays(7));
        es.checkpol.domain.Guest guest = new es.checkpol.domain.Guest(
            booking, "Ana", "Lopez", "Martin", es.checkpol.domain.DocumentType.NIF, "00000000T", "SUP123",
            LocalDate.now().minusYears(30), "ESP", null, "Calle Mayor 1", null, "28079", null, "28001", "ESP",
            "600000000", null, null, "PM", es.checkpol.domain.GuestSubmissionSource.SELF_SERVICE,
            es.checkpol.domain.GuestReviewStatus.PENDING_REVIEW, OffsetDateTime.now()
        );
        return new GuestSelfServiceDetails(booking, 1, java.util.List.of(guest));
    }

    private GuestForm sampleForm() {
        return new GuestForm(
            "Ana", "Lopez", "Martin", es.checkpol.domain.DocumentType.NIF, "00000000T", "SUP123",
            LocalDate.now().minusYears(30), "ESP", null, "Calle Mayor 1", "", "28079", "", "28001", "ESP",
            "600000000", "", "", "PM"
        );
    }
}
