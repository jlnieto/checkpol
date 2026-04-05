package es.checkpol.web;

import es.checkpol.domain.Accommodation;
import es.checkpol.domain.Booking;
import es.checkpol.domain.BookingChannel;
import es.checkpol.domain.PaymentType;
import es.checkpol.config.SecurityConfig;
import es.checkpol.service.BookingListItem;
import es.checkpol.service.BookingOperationalStatus;
import es.checkpol.service.AccommodationService;
import es.checkpol.service.BookingFilter;
import es.checkpol.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = {HomeController.class, BookingController.class})
@Import(SecurityConfig.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private AccommodationService accommodationService;

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void showsBookingList() throws Exception {
        when(accommodationService.findAll()).thenReturn(List.of(new Accommodation("Casa Olivo", "H123456789", "VT-123")));
        when(bookingService.findAll()).thenReturn(List.of());
        when(bookingService.findAll(BookingFilter.ALL)).thenReturn(List.of());
        when(bookingService.findAll(BookingFilter.INCOMPLETE)).thenReturn(List.of());
        when(bookingService.findAll(BookingFilter.READY)).thenReturn(List.of());
        when(bookingService.findAll(BookingFilter.TODAY)).thenReturn(List.of());
        when(bookingService.findAll(BookingFilter.UPCOMING)).thenReturn(List.of());

        mockMvc.perform(get("/bookings"))
            .andExpect(status().isOk())
            .andExpect(view().name("bookings/list"))
            .andExpect(model().attributeExists("bookings"))
            .andExpect(model().attributeExists("countAll"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Cerrar sesión")));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void showsReadyBookingsWhenReadyFilterIsSelected() throws Exception {
        Accommodation accommodation = new Accommodation("Casa Olivo", "H123456789", "VT-123");
        when(accommodationService.findAll()).thenReturn(List.of(accommodation));
        Booking booking = new Booking(
            accommodation,
            "ABC123",
            2,
            LocalDate.of(2026, 3, 25),
            BookingChannel.AIRBNB,
            LocalDate.of(2026, 3, 27),
            LocalDate.of(2026, 4, 1),
            PaymentType.PLATF,
            null,
            "Airbnb",
            null,
            null
        );
        BookingListItem readyItem = new BookingListItem(booking, 2, 2, true, false, BookingOperationalStatus.READY_FOR_XML, 0, false, null, null);

        when(bookingService.findAll()).thenReturn(List.of(readyItem));
        when(bookingService.findAll(BookingFilter.INCOMPLETE)).thenReturn(List.of());
        when(bookingService.findAll(BookingFilter.READY)).thenReturn(List.of(readyItem));
        when(bookingService.findAll(BookingFilter.TODAY)).thenReturn(List.of());
        when(bookingService.findAll(BookingFilter.UPCOMING)).thenReturn(List.of());

        mockMvc.perform(get("/bookings").param("filter", "ready"))
            .andExpect(status().isOk())
            .andExpect(view().name("bookings/list"))
            .andExpect(model().attribute("countReady", 1))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("ABC123")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("No hay estancias todavía"))));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void showsBlockingSummaryInsteadOfReadyMessageWhenBookingIsNotReadyForXml() throws Exception {
        Accommodation accommodation = new Accommodation("Casa Olivo", "H123456789", "VT-123");
        when(accommodationService.findAll()).thenReturn(List.of(accommodation));
        Booking booking = new Booking(
            accommodation,
            "ABC123",
            2,
            LocalDate.of(2026, 3, 25),
            BookingChannel.AIRBNB,
            LocalDate.of(2026, 3, 27),
            LocalDate.of(2026, 4, 1),
            PaymentType.PLATF,
            null,
            "Airbnb",
            null,
            null
        );
        BookingListItem blockedItem = new BookingListItem(
            booking,
            4,
            2,
            false,
            false,
            BookingOperationalStatus.GUEST_COUNT_MISMATCH,
            0,
            true,
            "4 huéspedes registrados · 2 personas esperadas",
            null
        );

        when(bookingService.findAll()).thenReturn(List.of(blockedItem));
        when(bookingService.findAll(BookingFilter.INCOMPLETE)).thenReturn(List.of(blockedItem));
        when(bookingService.findAll(BookingFilter.READY)).thenReturn(List.of());
        when(bookingService.findAll(BookingFilter.TODAY)).thenReturn(List.of());
        when(bookingService.findAll(BookingFilter.UPCOMING)).thenReturn(List.of());

        mockMvc.perform(get("/bookings"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("4 huéspedes registrados · 2 personas esperadas")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Lista para descargar"))));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void showsFirstAccommodationStepWhenOwnerHasNoAccommodations() throws Exception {
        when(accommodationService.findAll()).thenReturn(List.of());
        when(bookingService.findAll()).thenReturn(List.of());
        when(bookingService.findAll(BookingFilter.ALL)).thenReturn(List.of());
        when(bookingService.findAll(BookingFilter.INCOMPLETE)).thenReturn(List.of());
        when(bookingService.findAll(BookingFilter.READY)).thenReturn(List.of());
        when(bookingService.findAll(BookingFilter.TODAY)).thenReturn(List.of());
        when(bookingService.findAll(BookingFilter.UPCOMING)).thenReturn(List.of());

        mockMvc.perform(get("/bookings"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Crea tu primera vivienda")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Crear primera vivienda")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("No hay nada bloqueado"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("0 estancias pendientes"))));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void createsBookingAndRedirects() throws Exception {
        when(accommodationService.findAll()).thenReturn(List.of(new Accommodation("Casa Olivo", "H123456789", "VT-123")));

        mockMvc.perform(post("/bookings")
                .with(csrf())
                .param("accommodationId", "1")
                .param("referenceCode", "ABC123")
                .param("personCount", "2")
                .param("contractDate", format(LocalDate.now()))
                .param("checkInDate", format(LocalDate.now().plusDays(3)))
                .param("checkOutDate", format(LocalDate.now().plusDays(5))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/bookings"))
            .andExpect(flash().attribute("flashKind", "success"));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void showsSingleFirstBookingStepWhenOwnerHasAccommodationButNoBookings() throws Exception {
        when(accommodationService.findAll()).thenReturn(List.of(new Accommodation("Casa Olivo", "H123456789", "VT-123")));
        when(bookingService.findAll()).thenReturn(List.of());
        when(bookingService.findAll(BookingFilter.INCOMPLETE)).thenReturn(List.of());
        when(bookingService.findAll(BookingFilter.READY)).thenReturn(List.of());
        when(bookingService.findAll(BookingFilter.TODAY)).thenReturn(List.of());
        when(bookingService.findAll(BookingFilter.UPCOMING)).thenReturn(List.of());

        mockMvc.perform(get("/bookings"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Tu vivienda ya está lista")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Crear primera estancia")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Crear nueva estancia"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("No hay nada que revisar ahora"))));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void keepsFormWhenServiceValidationFails() throws Exception {
        when(accommodationService.findAll()).thenReturn(List.of(new Accommodation("Casa Olivo", "H123456789", "VT-123")));
        doThrow(new IllegalArgumentException("La fecha de salida debe ser posterior a la fecha de entrada."))
            .when(bookingService).create(any());

        mockMvc.perform(post("/bookings")
                .with(csrf())
                .param("accommodationId", "1")
                .param("referenceCode", "ABC123")
                .param("contractDate", format(LocalDate.now()))
                .param("checkInDate", format(LocalDate.now().plusDays(3)))
                .param("checkOutDate", format(LocalDate.now().plusDays(2))))
            .andExpect(status().isOk())
            .andExpect(view().name("bookings/form"))
            .andExpect(model().attributeExists("accommodations"));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void showsStoredDatesWhenEditingBooking() throws Exception {
        when(accommodationService.findAll()).thenReturn(List.of(new Accommodation("Casa Olivo", "H123456789", "VT-123")));
        when(bookingService.getForm(1L)).thenReturn(new BookingForm(
            1L,
            "ABC123",
            2,
            LocalDate.of(2026, 3, 27),
            LocalDate.of(2026, 3, 27),
            LocalDate.of(2026, 4, 1)
        ));

        mockMvc.perform(get("/bookings/1/edit"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"2026-03-27\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"2026-04-01\"")));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void preselectsAccommodationWhenOnlyOneOptionIsAvailable() throws Exception {
        Accommodation accommodation = new Accommodation("Casa Olivo", "H123456789", "VT-123");
        ReflectionTestUtils.setField(accommodation, "id", 7L);
        when(accommodationService.findAll()).thenReturn(List.of(accommodation));

        mockMvc.perform(get("/bookings/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("bookings/form"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("<option value=\"7\" selected=\"selected\">Casa Olivo</option>")));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void keepsPlaceholderWhenSeveralAccommodationsAreAvailable() throws Exception {
        Accommodation firstAccommodation = new Accommodation("Casa Olivo", "H123456789", "VT-123");
        ReflectionTestUtils.setField(firstAccommodation, "id", 7L);
        Accommodation secondAccommodation = new Accommodation("Casa Azul", "H987654321", "VT-456");
        ReflectionTestUtils.setField(secondAccommodation, "id", 8L);
        when(accommodationService.findAll()).thenReturn(List.of(firstAccommodation, secondAccommodation));

        mockMvc.perform(get("/bookings/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("bookings/form"))
            .andExpect(model().attribute("bookingForm", new BookingForm()))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<option value=\"7\" selected=\"selected\">Casa Olivo</option>"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<option value=\"8\" selected=\"selected\">Casa Azul</option>"))));
    }

    @Test
    void redirectsAnonymousUserToLogin() throws Exception {
        mockMvc.perform(get("/bookings"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "SUPER_ADMIN")
    void blocksAdminAccessToOwnerArea() throws Exception {
        mockMvc.perform(get("/bookings"))
            .andExpect(status().isForbidden())
            .andExpect(forwardedUrl("/access-denied"));
    }

    private String format(LocalDate date) {
        return date.toString();
    }
}
