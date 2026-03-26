package es.checkpol.web;

import es.checkpol.domain.Accommodation;
import es.checkpol.domain.Address;
import es.checkpol.domain.Booking;
import es.checkpol.domain.BookingChannel;
import es.checkpol.config.SecurityConfig;
import es.checkpol.service.BookingDetails;
import es.checkpol.service.BookingService;
import es.checkpol.service.AddressService;
import es.checkpol.service.GuestService;
import es.checkpol.service.GuestSelfServiceService;
import es.checkpol.service.TravelerPartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = {GuestController.class})
@Import({SecurityConfig.class, GuestWizardDraftStore.class})
class GuestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private AddressService addressService;

    @MockitoBean
    private GuestService guestService;

    @MockitoBean
    private TravelerPartService travelerPartService;

    @MockitoBean
    private GuestSelfServiceService guestSelfServiceService;

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void showsGuestForm() throws Exception {
        when(bookingService.getDetails(1L)).thenReturn(sampleDetails());
        when(addressService.findByBookingId(1L)).thenReturn(List.of(sampleAddress(sampleDetails().booking())));

        mockMvc.perform(get("/bookings/1/guests/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("guests/form"))
            .andExpect(model().attributeExists("guestForm"))
            .andExpect(model().attributeExists("details"))
            .andExpect(model().attributeExists("addresses"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("¿Cuál es tu dirección habitual?")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Usar esta direccion")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("name=\"phone2\"")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Selecciona...")));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void showsBlockingReadinessMessageOnBookingDetailWhenXmlCannotBeGenerated() throws Exception {
        when(bookingService.getDetails(1L)).thenReturn(sampleDetails());

        mockMvc.perform(get("/bookings/1"))
            .andExpect(status().isOk())
            .andExpect(view().name("bookings/details"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Hay 1 huespedes registrados y la estancia esta configurada para 2 personas.")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("button-link button-link-disabled")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Corrige esto para descargar.")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("data-review-feedback")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("booking-details.js")));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void showsGuestEditForm() throws Exception {
        when(bookingService.getDetails(1L)).thenReturn(sampleDetails());
        when(addressService.findByBookingId(1L)).thenReturn(List.of(sampleAddress(sampleDetails().booking())));
        when(guestService.getForm(7L)).thenReturn(new GuestForm(
            "Ana", "Lopez", "", null, "", "", LocalDate.now().minusYears(30),
            "ESP", es.checkpol.domain.GuestSex.M, 1L, "+34 600000000", "", "", ""
        ));

        mockMvc.perform(get("/bookings/1/guests/7/edit"))
            .andExpect(status().isOk())
            .andExpect(view().name("guests/form"))
            .andExpect(model().attributeExists("guestForm"));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void createsGuestAndRedirects() throws Exception {
        when(bookingService.getDetails(1L)).thenReturn(sampleDetails());
        when(addressService.findByBookingId(1L)).thenReturn(List.of(sampleAddress(sampleDetails().booking())));

        mockMvc.perform(post("/bookings/1/guests")
                .with(csrf())
                .param("firstName", "Ana")
                .param("lastName1", "Lopez")
                .param("birthDate", format(LocalDate.now().minusYears(30)))
                .param("nationality", "ESP")
                .param("sex", "M")
                .param("addressId", "1")
                .param("phone", "+34 600000000"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/bookings/1"));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void restoresDraftWhenReturningFromNewAddress() throws Exception {
        when(bookingService.getDetails(1L)).thenReturn(sampleDetails());
        when(addressService.findByBookingId(1L)).thenReturn(List.of(sampleAddress(sampleDetails().booking())));
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/bookings/1/guests/draft-address")
                .session(session)
                .with(csrf())
                .param("firstName", "Ana")
                .param("lastName1", "Lopez")
                .param("birthDate", format(LocalDate.now().minusYears(30)))
                .param("sex", "M")
                .param("addressId", "1")
                .param("phone", "+34 600000000"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/bookings/1/addresses/new"));

        mockMvc.perform(get("/bookings/1/guests/new")
                .session(session)
                .param("step", "3")
                .param("selectedAddressId", "9"))
            .andExpect(status().isOk())
            .andExpect(view().name("guests/form"))
            .andExpect(model().attribute("guestForm", hasToString(allOf(
                containsString("firstName=Ana"),
                containsString("addressId=9")
            ))));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void keepsFormWhenServiceValidationFails() throws Exception {
        when(bookingService.getDetails(1L)).thenReturn(sampleDetails());
        when(addressService.findByBookingId(1L)).thenReturn(List.of(sampleAddress(sampleDetails().booking())));
        doThrow(new IllegalArgumentException("Indica al menos un telefono o un correo."))
            .when(guestService).create(any(), any());

        mockMvc.perform(post("/bookings/1/guests")
                .with(csrf())
                .param("firstName", "Ana")
                .param("lastName1", "Lopez")
                .param("birthDate", format(LocalDate.now().minusYears(30)))
                .param("nationality", "ESP")
                .param("sex", "M")
                .param("addressId", "1")
                .param("phone", "+34 600000000"))
            .andExpect(status().isOk())
            .andExpect(view().name("guests/form"))
            .andExpect(model().attributeExists("details"));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void redirectsBackToDetailWithFlashWhenXmlDownloadIsBlocked() throws Exception {
        doThrow(new IllegalStateException("No se puede generar el XML porque el numero de huespedes registrados no coincide con el numero de personas de la estancia."))
            .when(travelerPartService).generateXml(1L);

        mockMvc.perform(get("/bookings/1/traveler-part.xml"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/bookings/1"))
            .andExpect(flash().attribute("flashMessage", "No se puede generar el XML porque el numero de huespedes registrados no coincide con el numero de personas de la estancia."));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void marksGuestReviewedWithAjaxResponse() throws Exception {
        mockMvc.perform(post("/bookings/1/guests/7/review")
                .with(csrf())
                .header("X-Requested-With", "fetch"))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("\"success\":true")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Huesped marcado como revisado.")));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void returnsAjaxErrorWhenGuestReviewFails() throws Exception {
        doThrow(new IllegalArgumentException("No he encontrado esa persona."))
            .when(guestService).markReviewed(7L);

        mockMvc.perform(post("/bookings/1/guests/7/review")
                .with(csrf())
                .header("X-Requested-With", "fetch"))
            .andExpect(status().isBadRequest())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("\"success\":false")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("No se pudo marcar como revisado. Intentalo de nuevo.")));
    }

    private BookingDetails sampleDetails() {
        Accommodation accommodation = new Accommodation("Casa Olivo", "H123456789", "VT-123");
        Booking booking = new Booking(accommodation, "ABC123", 2,
            LocalDate.now(), BookingChannel.DIRECT, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4),
            es.checkpol.domain.PaymentType.EFECT, LocalDate.now(), null, null, null);
        Address address = sampleAddress(booking);
        es.checkpol.domain.Guest guest = new es.checkpol.domain.Guest(
            booking,
            "Ana",
            "Lopez",
            "Martin",
            null,
            null,
            null,
            LocalDate.now().minusYears(30),
            "ESP",
            es.checkpol.domain.GuestSex.M,
            address,
            "+34 600000000",
            null,
            null,
            null,
            es.checkpol.domain.GuestSubmissionSource.MANUAL,
            es.checkpol.domain.GuestReviewStatus.REVIEWED,
            null
        );
        return new BookingDetails(
            booking,
            List.of(guest),
            1,
            2,
            true,
            false,
            Optional.empty(),
            0,
            List.of(),
            Optional.empty(),
            es.checkpol.service.BookingOperationalStatus.GUEST_COUNT_MISMATCH,
            0,
            0,
            false,
            false,
            false,
            false,
            "1 huesped registrado · 2 personas esperadas",
            "No se puede generar el XML porque el numero de huespedes registrados no coincide con el numero de personas de la estancia.",
            List.of("No se puede generar el XML porque el numero de huespedes registrados no coincide con el numero de personas de la estancia.")
        );
    }

    private String format(LocalDate date) {
        return date.toString();
    }

    private Address sampleAddress(Booking booking) {
        Address address = new Address(
            booking,
            "Calle Mayor 1",
            null,
            "28079",
            "Madrid",
            "Madrid",
            es.checkpol.domain.MunicipalityResolutionStatus.EXACT,
            null,
            "28001",
            "ESP"
        );
        ReflectionTestUtils.setField(address, "id", 1L);
        return address;
    }
}
