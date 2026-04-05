package es.checkpol.web;

import es.checkpol.domain.Accommodation;
import es.checkpol.domain.Address;
import es.checkpol.domain.Booking;
import es.checkpol.domain.BookingChannel;
import es.checkpol.domain.DocumentType;
import es.checkpol.config.SecurityConfig;
import es.checkpol.service.BookingDetails;
import es.checkpol.service.BookingService;
import es.checkpol.service.AddressService;
import es.checkpol.service.GuestService;
import es.checkpol.service.GuestSelfServiceService;
import es.checkpol.service.SelfServiceAccess;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
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
                .string(org.hamcrest.Matchers.containsString("Paso 1 de 4")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Usar esta dirección")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("name=\"phone2\"")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("España (ESP)")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Guía rápida")));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void showsBlockingReadinessMessageOnBookingDetailWhenXmlCannotBeGenerated() throws Exception {
        when(bookingService.getDetails(1L)).thenReturn(sampleDetails());

        mockMvc.perform(get("/bookings/1"))
            .andExpect(status().isOk())
            .andExpect(view().name("bookings/details"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Falta 1 huésped para completar esta estancia")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("1 de 2 huéspedes registrados en esta estancia.")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Enviar enlace a los huéspedes")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("data-review-feedback")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("booking-details.js")));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void preparesShareMessageUsingSelfServiceLink() throws Exception {
        when(bookingService.getDetails(1L)).thenReturn(sampleDetails());
        when(guestSelfServiceService.issueAccess(1L)).thenReturn(new SelfServiceAccess("abc123", OffsetDateTime.now().plusDays(7)));

        mockMvc.perform(post("/bookings/1/share-self-service-link").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/bookings/1"))
            .andExpect(flash().attributeExists("shareMessage"));
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
    void showsGuestReviewForm() throws Exception {
        BookingDetails details = samplePendingReviewDetails();
        when(bookingService.getDetails(1L)).thenReturn(details);

        mockMvc.perform(get("/bookings/1/guests/7/review"))
            .andExpect(status().isOk())
            .andExpect(view().name("guests/review"))
            .andExpect(model().attributeExists("reviewIssues"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Marcar como correcto")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Editar si ves algo raro")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Código de municipio")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("28079")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Código postal")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("28001")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(not(org.hamcrest.Matchers.containsString("Enlace huesped"))))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(not(org.hamcrest.Matchers.containsString("Solo revisa esto"))));
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
    void redirectsBackToBookingWhenSavingReview() throws Exception {
        BookingDetails details = samplePendingReviewDetails();
        when(bookingService.getDetails(1L)).thenReturn(details);

        mockMvc.perform(post("/bookings/1/guests/7/review")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/bookings/1"))
            .andExpect(flash().attribute("flashMessage", "Datos guardados."));

        verify(guestService).markReviewed(7L);
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void redirectsBackToBookingWithXmlMessageWhenReviewFinishesLastPendingGuest() throws Exception {
        BookingDetails afterSave = sampleReadyDetails();
        when(bookingService.getDetails(1L)).thenReturn(afterSave);

        mockMvc.perform(post("/bookings/1/guests/7/review")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/bookings/1"))
            .andExpect(flash().attribute("flashMessage", "Datos guardados. La estancia ya está lista para descargar el archivo para SES."));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void submitsTravelerPartToSesWhenConfigured() throws Exception {
        when(travelerPartService.submitTravelerPart(1L))
            .thenReturn(new es.checkpol.service.SesSubmissionResult(0, "ok", "lote-1"));

        mockMvc.perform(post("/bookings/1/submit-to-ses").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/bookings/1"))
            .andExpect(flash().attribute("flashMessage", "Envío enviado a SES. Lote: lote-1."));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void refreshesSesSubmissionStatus() throws Exception {
        when(travelerPartService.refreshSesSubmissionStatus(1L, 9L))
            .thenReturn(new es.checkpol.service.SesLoteStatusResult(0, "ok", "lote-1", 0, "Procesado", "com-1", null, null, OffsetDateTime.now()));

        mockMvc.perform(post("/bookings/1/communications/9/refresh-ses-status").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/bookings/1"))
            .andExpect(flash().attribute("flashMessage", "SES ya ha procesado la comunicación. Código: com-1."));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void cancelsSesSubmission() throws Exception {
        when(travelerPartService.cancelSesSubmission(1L, 9L))
            .thenReturn(new es.checkpol.service.SesSubmissionResult(0, "ok", null));

        mockMvc.perform(post("/bookings/1/communications/9/cancel-ses").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/bookings/1"))
            .andExpect(flash().attribute("flashMessage", "Lote anulado en SES."));
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

    private BookingDetails samplePendingReviewDetails() {
        Accommodation accommodation = new Accommodation("Casa Olivo", "H123456789", "VT-123");
        Booking booking = new Booking(accommodation, "ABC123", 2,
            LocalDate.now(), BookingChannel.DIRECT, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4),
            es.checkpol.domain.PaymentType.EFECT, LocalDate.now(), null, null, null);
        Address address = sampleAddress(booking);

        es.checkpol.domain.Guest firstGuest = new es.checkpol.domain.Guest(
            booking,
            "Ana",
            "Lopez",
            "",
            DocumentType.PAS,
            "X1234567",
            "",
            LocalDate.now().minusYears(30),
            "ESP",
            es.checkpol.domain.GuestSex.M,
            address,
            "+34 600000000",
            null,
            null,
            null,
            es.checkpol.domain.GuestSubmissionSource.SELF_SERVICE,
            es.checkpol.domain.GuestReviewStatus.PENDING_REVIEW,
            OffsetDateTime.now()
        );
        ReflectionTestUtils.setField(firstGuest, "id", 7L);

        es.checkpol.domain.Guest secondGuest = new es.checkpol.domain.Guest(
            booking,
            "Luis",
            "Perez",
            "",
            DocumentType.PAS,
            "Y7654321",
            "",
            LocalDate.now().minusYears(31),
            "ESP",
            es.checkpol.domain.GuestSex.M,
            address,
            "+34 611111111",
            null,
            null,
            null,
            es.checkpol.domain.GuestSubmissionSource.SELF_SERVICE,
            es.checkpol.domain.GuestReviewStatus.PENDING_REVIEW,
            OffsetDateTime.now()
        );
        ReflectionTestUtils.setField(secondGuest, "id", 8L);

        return new BookingDetails(
            booking,
            List.of(firstGuest, secondGuest),
            2,
            2,
            false,
            false,
            false,
            Optional.empty(),
            0,
            List.of(),
            Optional.empty(),
            es.checkpol.service.BookingOperationalStatus.REVIEW_PENDING,
            2,
            2,
            false,
            false,
            true,
            false,
            "2 huespedes pendientes de revision",
            "Todavia hay 2 huespedes pendientes de revision. Revisalos antes de generar el archivo.",
            List.of("Todavia hay 2 huespedes pendientes de revision. Revisalos antes de generar el archivo.")
        );
    }

    private BookingDetails sampleSinglePendingReviewDetails() {
        Accommodation accommodation = new Accommodation("Casa Olivo", "H123456789", "VT-123");
        Booking booking = new Booking(accommodation, "ABC123", 1,
            LocalDate.now(), BookingChannel.DIRECT, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4),
            es.checkpol.domain.PaymentType.EFECT, LocalDate.now(), null, null, null);
        Address address = sampleAddress(booking);

        es.checkpol.domain.Guest guest = new es.checkpol.domain.Guest(
            booking,
            "Ana",
            "Lopez",
            "",
            DocumentType.PAS,
            "X1234567",
            "",
            LocalDate.now().minusYears(30),
            "ESP",
            es.checkpol.domain.GuestSex.M,
            address,
            "+34 600000000",
            null,
            null,
            null,
            es.checkpol.domain.GuestSubmissionSource.SELF_SERVICE,
            es.checkpol.domain.GuestReviewStatus.PENDING_REVIEW,
            OffsetDateTime.now()
        );
        ReflectionTestUtils.setField(guest, "id", 7L);

        return new BookingDetails(
            booking,
            List.of(guest),
            1,
            1,
            false,
            false,
            false,
            Optional.empty(),
            0,
            List.of(),
            Optional.empty(),
            es.checkpol.service.BookingOperationalStatus.REVIEW_PENDING,
            1,
            1,
            false,
            false,
            true,
            false,
            "1 huesped pendiente de revision",
            "Todavia hay 1 huesped pendiente de revision. Revisalo antes de generar el archivo.",
            List.of("Todavia hay 1 huesped pendiente de revision. Revisalo antes de generar el archivo.")
        );
    }

    private BookingDetails sampleReadyDetails() {
        Accommodation accommodation = new Accommodation("Casa Olivo", "H123456789", "VT-123");
        Booking booking = new Booking(accommodation, "ABC123", 1,
            LocalDate.now(), BookingChannel.DIRECT, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4),
            es.checkpol.domain.PaymentType.EFECT, LocalDate.now(), null, null, null);
        Address address = sampleAddress(booking);
        es.checkpol.domain.Guest guest = new es.checkpol.domain.Guest(
            booking,
            "Ana",
            "Lopez",
            "",
            DocumentType.PAS,
            "X1234567",
            "",
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
        ReflectionTestUtils.setField(guest, "id", 7L);

        return new BookingDetails(
            booking,
            List.of(guest),
            1,
            1,
            false,
            true,
            false,
            Optional.empty(),
            0,
            List.of(),
            Optional.empty(),
            es.checkpol.service.BookingOperationalStatus.READY_FOR_XML,
            0,
            0,
            false,
            false,
            false,
            false,
            "Lista para descargar el archivo SES",
            null,
            List.of()
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
            "28001",
            "ESP"
        );
        ReflectionTestUtils.setField(address, "id", 1L);
        return address;
    }
}
