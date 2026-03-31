package es.checkpol.web;

import es.checkpol.config.SecurityConfig;
import es.checkpol.domain.Accommodation;
import es.checkpol.domain.Address;
import es.checkpol.domain.Booking;
import es.checkpol.domain.BookingChannel;
import es.checkpol.domain.PaymentType;
import es.checkpol.service.AddressService;
import es.checkpol.service.BookingDetails;
import es.checkpol.service.BookingOperationalStatus;
import es.checkpol.service.BookingService;
import es.checkpol.service.GuestSelfServiceDetails;
import es.checkpol.service.GuestSelfServiceService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = AddressController.class)
@Import(SecurityConfig.class)
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AddressService addressService;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private GuestSelfServiceService guestSelfServiceService;

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void showsInternalAddressFormWithComplementField() throws Exception {
        when(bookingService.getDetails(1L)).thenReturn(sampleDetails());

        mockMvc.perform(get("/bookings/1/addresses/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("addresses/form"))
            .andExpect(content().string(containsString("name=\"addressComplement\"")));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    void createsInternalAddressWithComplementAndReturnsToStep3() throws Exception {
        when(addressService.create(eq(1L), any(AddressForm.class))).thenReturn(sampleAddress(sampleDetails().booking(), 9L));

        mockMvc.perform(post("/bookings/1/addresses")
                .with(csrf())
                .param("addressLine", "Calle Mayor 1")
                .param("addressComplement", "Piso 2B")
                .param("postalCode", "28001")
                .param("municipalityCode", "28079")
                .param("country", "ESP"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/bookings/1/guests/new?step=3&selectedAddressId=9"));

        ArgumentCaptor<AddressForm> captor = ArgumentCaptor.forClass(AddressForm.class);
        verify(addressService).create(eq(1L), captor.capture());
        assertEquals("Piso 2B", captor.getValue().addressComplement());
    }

    @Test
    void showsPublicAddressFormWithComplementField() throws Exception {
        when(guestSelfServiceService.getByToken("abc")).thenReturn(sampleAccess());

        mockMvc.perform(get("/guest-access/abc/addresses/new").param("slot", "2"))
            .andExpect(status().isOk())
            .andExpect(view().name("public/address-form"))
            .andExpect(content().string(containsString("name=\"addressComplement\"")));
    }

    @Test
    void createsPublicAddressWithComplementAndReturnsToStep3() throws Exception {
        when(guestSelfServiceService.createAddress(eq("abc"), any(AddressForm.class))).thenReturn(12L);

        mockMvc.perform(post("/guest-access/abc/addresses")
                .with(csrf())
                .param("slot", "2")
                .param("addressLine", "Calle Mayor 1")
                .param("addressComplement", "Piso 2B")
                .param("postalCode", "28001")
                .param("municipalityCode", "28079")
                .param("country", "ESP"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/guest-access/abc/guests/new?step=3&selectedAddressId=12&slot=2"));

        ArgumentCaptor<AddressForm> captor = ArgumentCaptor.forClass(AddressForm.class);
        verify(guestSelfServiceService).createAddress(eq("abc"), captor.capture());
        assertEquals("Piso 2B", captor.getValue().addressComplement());
    }

    private BookingDetails sampleDetails() {
        Booking booking = sampleBooking();
        return new BookingDetails(
            booking,
            List.of(),
            0,
            2,
            true,
            false,
            Optional.empty(),
            0,
            List.of(),
            Optional.empty(),
            BookingOperationalStatus.INCOMPLETE,
            0,
            0,
            false,
            false,
            false,
            false,
            "Faltan huespedes",
            "Faltan huespedes",
            List.of("Faltan huespedes")
        );
    }

    private GuestSelfServiceDetails sampleAccess() {
        Booking booking = sampleBooking();
        booking.updateSelfServiceAccess("abc", OffsetDateTime.now().plusDays(7));
        return new GuestSelfServiceDetails(
            booking,
            0,
            2,
            List.of(),
            List.of(sampleAddress(booking, 1L))
        );
    }

    private Booking sampleBooking() {
        return new Booking(
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
    }

    private Address sampleAddress(Booking booking, Long id) {
        Address address = new Address(
            booking,
            "Calle Mayor 1",
            "Piso 2B",
            "28079",
            "Madrid",
            "28001",
            "ESP"
        );
        ReflectionTestUtils.setField(address, "id", id);
        return address;
    }
}
