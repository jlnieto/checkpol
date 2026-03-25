package es.checkpol.web;

import es.checkpol.domain.Accommodation;
import es.checkpol.domain.BookingChannel;
import es.checkpol.service.AccommodationService;
import es.checkpol.service.BookingFilter;
import es.checkpol.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = {HomeController.class, BookingController.class})
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private AccommodationService accommodationService;

    @Test
    void showsBookingList() throws Exception {
        when(bookingService.findAll()).thenReturn(List.of());
        when(bookingService.findAll(BookingFilter.ALL)).thenReturn(List.of());

        mockMvc.perform(get("/bookings"))
            .andExpect(status().isOk())
            .andExpect(view().name("bookings/list"))
            .andExpect(model().attributeExists("bookings"))
            .andExpect(model().attributeExists("countAll"));
    }

    @Test
    void createsBookingAndRedirects() throws Exception {
        when(accommodationService.findAll()).thenReturn(List.of(new Accommodation("Casa Olivo", "H123456789", "VT-123")));

        mockMvc.perform(post("/bookings")
                .param("accommodationId", "1")
                .param("channel", BookingChannel.DIRECT.name())
                .param("referenceCode", "ABC123")
                .param("contractDate", LocalDate.now().toString())
                .param("checkInDate", LocalDate.now().plusDays(3).toString())
                .param("checkOutDate", LocalDate.now().plusDays(5).toString())
                .param("paymentType", es.checkpol.domain.PaymentType.EFECT.name()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/bookings"));
    }

    @Test
    void keepsFormWhenServiceValidationFails() throws Exception {
        when(accommodationService.findAll()).thenReturn(List.of(new Accommodation("Casa Olivo", "H123456789", "VT-123")));
        doThrow(new IllegalArgumentException("La fecha de salida debe ser posterior a la fecha de entrada."))
            .when(bookingService).create(any());

        mockMvc.perform(post("/bookings")
                .param("accommodationId", "1")
                .param("channel", BookingChannel.DIRECT.name())
                .param("referenceCode", "ABC123")
                .param("contractDate", LocalDate.now().toString())
                .param("checkInDate", LocalDate.now().plusDays(3).toString())
                .param("checkOutDate", LocalDate.now().plusDays(2).toString())
                .param("paymentType", es.checkpol.domain.PaymentType.EFECT.name()))
            .andExpect(status().isOk())
            .andExpect(view().name("bookings/form"))
            .andExpect(model().attributeExists("accommodations"));
    }
}
