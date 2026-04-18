package es.checkpol.web;

import es.checkpol.domain.Accommodation;
import es.checkpol.domain.Address;
import es.checkpol.domain.Booking;
import es.checkpol.domain.BookingChannel;
import es.checkpol.domain.PaymentType;
import es.checkpol.config.SecurityConfig;
import es.checkpol.service.GuestSelfServiceDetails;
import es.checkpol.service.GuestSelfServiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = GuestSelfServiceController.class)
@Import({SecurityConfig.class, GuestWizardDraftStore.class})
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
            .andExpect(view().name("public/guest-access"))
            .andExpect(model().attributeExists("access"))
            .andExpect(model().attributeExists("guestCards"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Datos de huéspedes")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Tu estancia")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Código de acceso: ABC123")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Editar datos")));
    }

    @Test
    void showsClearPublicErrorWhenAccessTokenDoesNotExist() throws Exception {
        when(guestSelfServiceService.getByToken("missing"))
            .thenThrow(new IllegalArgumentException("El enlace indicado no existe."));

        mockMvc.perform(get("/guest-access/missing"))
            .andExpect(status().isNotFound())
            .andExpect(view().name("public/guest-access-unavailable"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Enlace no disponible")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Pide a la persona responsable")));
    }

    @Test
    void showsPublicCompletionScreenWhenAllGuestsAreCompleted() throws Exception {
        when(guestSelfServiceService.getByToken("all-done")).thenReturn(sampleCompletedAccess());

        mockMvc.perform(get("/guest-access/all-done/complete"))
            .andExpect(status().isOk())
            .andExpect(view().name("public/guest-complete"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Check-in finalizado")));
    }

    @Test
    void showsPublicGuestNewFormForSelectedSlot() throws Exception {
        when(guestSelfServiceService.getByToken("abc")).thenReturn(sampleAccess());

        mockMvc.perform(get("/guest-access/abc/guests/new").param("slot", "2"))
            .andExpect(status().isOk())
            .andExpect(view().name("public/guest-form"))
            .andExpect(model().attributeExists("guestForm"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Datos del huésped 2")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("name=\"phone2\"")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Selecciona...")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Nacionalidad <span class=\"text-slate-400\">(opcional)</span>")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Parentesco con una persona adulta de la estancia")));
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
    void redirectsToDashboardWhenPublicGuestEditLinkIsInvalid() throws Exception {
        when(guestSelfServiceService.getByToken("abc")).thenReturn(sampleAccess());
        when(guestSelfServiceService.getGuestForm("abc", 7L))
            .thenThrow(new IllegalArgumentException("El huésped indicado no pertenece a este enlace."));

        mockMvc.perform(get("/guest-access/abc/guests/7/edit"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/guest-access/abc"))
            .andExpect(flash().attribute("flashError", "El huésped indicado no pertenece a este enlace."));
    }

    @Test
    void submitsPublicGuestForm() throws Exception {
        when(guestSelfServiceService.getByToken("abc")).thenReturn(sampleAccess());

        mockMvc.perform(post("/guest-access/abc/guests")
                .with(csrf())
                .param("slot", "2")
                .param("firstName", "Ana")
                .param("lastName1", "Lopez")
                .param("lastName2", "Martin")
                .param("birthDate", format(LocalDate.now().minusYears(30)))
                .param("nationality", "ESP")
                .param("documentType", "NIF")
                .param("documentNumber", "00000000T")
                .param("documentSupport", "SUP123")
                .param("sex", "M")
                .param("addressId", "1")
                .param("phone", "+34 600000000")
                .param("relationship", "PM"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/guest-access/abc"));
    }

    @Test
    void restoresPublicDraftWhenReturningFromNewAddress() throws Exception {
        when(guestSelfServiceService.getByToken("abc")).thenReturn(sampleAccess());
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/guest-access/abc/guests/draft-address")
                .session(session)
                .with(csrf())
                .param("slot", "2")
                .param("firstName", "Ana")
                .param("lastName1", "Lopez")
                .param("birthDate", format(LocalDate.now().minusYears(30)))
                .param("sex", "M")
                .param("addressId", "1")
                .param("phone", "+34 600000000"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/guest-access/abc/addresses/new?slot=2"));

        mockMvc.perform(get("/guest-access/abc/guests/new")
                .session(session)
                .param("slot", "2")
                .param("step", "3")
                .param("selectedAddressId", "9"))
            .andExpect(status().isOk())
            .andExpect(view().name("public/guest-form"))
            .andExpect(model().attribute("guestForm", hasToString(allOf(
                containsString("firstName=Ana"),
                containsString("addressId=9")
            ))));
    }

    @Test
    void keepsPublicGuestFormWhenServiceFails() throws Exception {
        when(guestSelfServiceService.getByToken("abc")).thenReturn(sampleAccess());
        doThrow(new IllegalArgumentException("El enlace ha caducado.")).when(guestSelfServiceService).submitGuest(any(), any());

        mockMvc.perform(post("/guest-access/abc/guests")
                .with(csrf())
                .param("slot", "2")
                .param("firstName", "Ana")
                .param("lastName1", "Lopez")
                .param("lastName2", "Martin")
                .param("birthDate", format(LocalDate.now().minusYears(30)))
                .param("nationality", "ESP")
                .param("documentType", "NIF")
                .param("documentNumber", "00000000T")
                .param("documentSupport", "SUP123")
                .param("sex", "M")
                .param("addressId", "1")
                .param("phone", "+34 600000000")
                .param("relationship", "PM"))
            .andExpect(status().isOk())
            .andExpect(view().name("public/guest-form"));
    }

    @Test
    void updatesPublicGuestForm() throws Exception {
        when(guestSelfServiceService.getByToken("abc")).thenReturn(sampleAccess());

        mockMvc.perform(post("/guest-access/abc/guests/7")
                .with(csrf())
                .param("firstName", "Ana")
                .param("lastName1", "Lopez")
                .param("lastName2", "Martin")
                .param("birthDate", format(LocalDate.now().minusYears(30)))
                .param("nationality", "ESP")
                .param("documentType", "NIF")
                .param("documentNumber", "00000000T")
                .param("documentSupport", "SUP123")
                .param("sex", "M")
                .param("addressId", "1")
                .param("phone", "+34 600000000")
                .param("relationship", "PM"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/guest-access/abc"));
    }

    private GuestSelfServiceDetails sampleAccess() {
        Booking booking = new Booking(
            new Accommodation("Casa Olivo", "H123456789", "VT-123"),
            "ABC123",
            2,
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
        Address address = sampleAddress(booking, 1L);
        es.checkpol.domain.Guest guest = new es.checkpol.domain.Guest(
            booking, "Ana", "Lopez", "Martin", es.checkpol.domain.DocumentType.NIF, "00000000T", "SUP123",
            LocalDate.now().minusYears(30), "ESP", es.checkpol.domain.GuestSex.M, address,
            "+34 600000000", null, null, "PM", es.checkpol.domain.GuestSubmissionSource.SELF_SERVICE,
            es.checkpol.domain.GuestReviewStatus.PENDING_REVIEW, OffsetDateTime.now()
        );
        ReflectionTestUtils.setField(guest, "id", 7L);
        return new GuestSelfServiceDetails(
            booking,
            1,
            2,
            List.of(guest),
            List.of(address)
        );
    }

    private GuestSelfServiceDetails sampleCompletedAccess() {
        Booking booking = new Booking(
            new Accommodation("Casa Olivo", "H123456789", "VT-123"),
            "ABC123",
            1,
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
        booking.updateSelfServiceAccess("all-done", OffsetDateTime.now().plusDays(7));
        Address address = sampleAddress(booking, 1L);
        es.checkpol.domain.Guest guest = new es.checkpol.domain.Guest(
            booking, "Ana", "Lopez", "Martin", es.checkpol.domain.DocumentType.NIF, "00000000T", "SUP123",
            LocalDate.now().minusYears(30), "ESP", es.checkpol.domain.GuestSex.M, address,
            "+34 600000000", null, "ana@test.com", "PM", es.checkpol.domain.GuestSubmissionSource.SELF_SERVICE,
            es.checkpol.domain.GuestReviewStatus.PENDING_REVIEW, OffsetDateTime.now()
        );
        ReflectionTestUtils.setField(guest, "id", 11L);
        return new GuestSelfServiceDetails(
            booking,
            1,
            1,
            List.of(guest),
            List.of(address)
        );
    }

    private GuestForm sampleForm() {
        return new GuestForm(
            "Ana", "Lopez", "Martin", es.checkpol.domain.DocumentType.NIF, "00000000T", "SUP123",
            LocalDate.now().minusYears(30), "ESP", es.checkpol.domain.GuestSex.M, 1L, "+34 600000000", "", "", "PM"
        );
    }

    private Address sampleAddress(Booking booking, Long id) {
        Address address = new Address(
            booking,
            "Calle Mayor 1",
            null,
            "28079",
            "Madrid",
            "28001",
            "ESP"
        );
        ReflectionTestUtils.setField(address, "id", id);
        return address;
    }

    private String format(LocalDate date) {
        return date.toString();
    }
}
