package es.checkpol.web;

import es.checkpol.domain.Accommodation;
import es.checkpol.domain.Booking;
import es.checkpol.domain.BookingChannel;
import es.checkpol.service.BookingDetails;
import es.checkpol.service.BookingService;
import es.checkpol.service.GuestService;
import es.checkpol.service.GuestSelfServiceService;
import es.checkpol.service.TravelerPartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = {GuestController.class})
class GuestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private GuestService guestService;

    @MockitoBean
    private TravelerPartService travelerPartService;

    @MockitoBean
    private GuestSelfServiceService guestSelfServiceService;

    @Test
    void showsGuestForm() throws Exception {
        when(bookingService.getDetails(1L)).thenReturn(sampleDetails());

        mockMvc.perform(get("/bookings/1/guests/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("guests/form"))
            .andExpect(model().attributeExists("guestForm"))
            .andExpect(model().attributeExists("details"));
    }

    @Test
    void showsGuestEditForm() throws Exception {
        when(bookingService.getDetails(1L)).thenReturn(sampleDetails());
        when(guestService.getForm(7L)).thenReturn(new GuestForm(
            "Ana", "Lopez", "", null, "", "", LocalDate.now().minusYears(30),
            "ESP", null, "Calle Mayor 1", "", "28079", "", "28001", "ESP", "600000000", "", "", ""
        ));

        mockMvc.perform(get("/bookings/1/guests/7/edit"))
            .andExpect(status().isOk())
            .andExpect(view().name("guests/form"))
            .andExpect(model().attributeExists("guestForm"));
    }

    @Test
    void createsGuestAndRedirects() throws Exception {
        when(bookingService.getDetails(1L)).thenReturn(sampleDetails());

        mockMvc.perform(post("/bookings/1/guests")
                .param("firstName", "Ana")
                .param("lastName1", "Lopez")
                .param("birthDate", LocalDate.now().minusYears(30).toString())
                .param("addressLine", "Calle Mayor 1")
                .param("municipalityCode", "28079")
                .param("postalCode", "28001")
                .param("country", "ESP")
                .param("phone", "600000000"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/bookings/1"));
    }

    @Test
    void keepsFormWhenServiceValidationFails() throws Exception {
        when(bookingService.getDetails(1L)).thenReturn(sampleDetails());
        doThrow(new IllegalArgumentException("Indica al menos un telefono o un correo."))
            .when(guestService).create(any(), any());

        mockMvc.perform(post("/bookings/1/guests")
                .param("firstName", "Ana")
                .param("lastName1", "Lopez")
                .param("birthDate", LocalDate.now().minusYears(30).toString())
                .param("addressLine", "Calle Mayor 1")
                .param("municipalityCode", "28079")
                .param("postalCode", "28001")
                .param("country", "ESP")
                .param("phone", "600000000"))
            .andExpect(status().isOk())
            .andExpect(view().name("guests/form"))
            .andExpect(model().attributeExists("details"));
    }

    private BookingDetails sampleDetails() {
        Accommodation accommodation = new Accommodation("Casa Olivo", "H123456789", "VT-123");
        Booking booking = new Booking(accommodation, "ABC123", LocalDate.now(),
            BookingChannel.DIRECT, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4),
            es.checkpol.domain.PaymentType.EFECT, LocalDate.now(), null, null, null);
        return new BookingDetails(
            booking,
            List.of(),
            false,
            Optional.empty(),
            0,
            List.of(),
            Optional.empty(),
            es.checkpol.service.BookingOperationalStatus.INCOMPLETE,
            0
        );
    }
}
