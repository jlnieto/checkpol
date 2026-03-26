package es.checkpol.service;

import es.checkpol.domain.Accommodation;
import es.checkpol.domain.Address;
import es.checkpol.domain.Booking;
import es.checkpol.domain.BookingChannel;
import es.checkpol.domain.GuestSex;
import es.checkpol.domain.MunicipalityResolutionStatus;
import es.checkpol.domain.PaymentType;
import es.checkpol.repository.AddressRepository;
import es.checkpol.repository.BookingRepository;
import es.checkpol.repository.GuestRepository;
import es.checkpol.web.GuestForm;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GuestServiceTest {

    private final AddressRepository addressRepository = Mockito.mock(AddressRepository.class);
    private final GuestRepository guestRepository = Mockito.mock(GuestRepository.class);
    private final BookingRepository bookingRepository = Mockito.mock(BookingRepository.class);
    private final MunicipalityReviewService municipalityReviewService = Mockito.mock(MunicipalityReviewService.class);
    private final GuestService guestService = new GuestService(
        addressRepository,
        guestRepository,
        bookingRepository,
        municipalityReviewService
    );

    GuestServiceTest() {
        Booking booking = sampleBooking(LocalDate.now().plusDays(2));
        Mockito.when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        Mockito.when(addressRepository.findByIdAndBookingId(1L, booking.getId())).thenReturn(Optional.of(sampleAddress(booking)));
    }

    @Test
    void rejectsGuestWithoutMunicipality() {
        GuestForm form = new GuestForm(
            "Ana", "Lopez", "", null, "", "", LocalDate.now().minusYears(30),
            "ESP", GuestSex.M, null, "+34 600000000", "", "", ""
        );

        assertThrows(IllegalArgumentException.class, () -> guestService.create(1L, form));
    }

    @Test
    void rejectsGuestWithoutContact() {
        GuestForm form = new GuestForm(
            "Ana", "Lopez", "", null, "", "", LocalDate.now().minusYears(30),
            "ESP", GuestSex.M, 1L, "", "", "", ""
        );

        assertThrows(IllegalArgumentException.class, () -> guestService.create(1L, form));
    }

    @Test
    void rejectsInvalidNifWithoutSecondSurname() {
        GuestForm form = new GuestForm(
            "Ana", "Lopez", "", es.checkpol.domain.DocumentType.NIF, "00000000T", "SUP123", LocalDate.now().minusYears(30),
            "ESP", GuestSex.M, 1L, "+34 600000000", "", "", ""
        );

        assertThrows(IllegalArgumentException.class, () -> guestService.create(1L, form));
    }

    @Test
    void rejectsMinorWithoutRelationship() {
        GuestForm form = new GuestForm(
            "Ana", "Lopez", "Martin", null, "", "", LocalDate.now().minusYears(10),
            "ESP", GuestSex.M, 1L, "+34 600000000", "", "", ""
        );

        assertThrows(IllegalArgumentException.class, () -> guestService.create(1L, form));
    }

    @Test
    void rejectsAdultWithoutDocument() {
        GuestForm form = new GuestForm(
            "Ana", "Lopez", "Martin", null, "", "", LocalDate.now().minusYears(30),
            "ESP", GuestSex.M, 1L, "+34 600000000", "", "", "PM"
        );

        assertThrows(IllegalArgumentException.class, () -> guestService.create(1L, form));
    }

    @Test
    void rejectsSpanishGuestOlderThan14WithoutDocument() {
        Mockito.when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking(LocalDate.of(2026, 4, 1))));
        GuestForm form = new GuestForm(
            "Ana", "Lopez", "Martin", null, "", "", LocalDate.of(2010, 3, 1),
            "ESP", GuestSex.M, 1L, "+34 600000000", "", "", "PM"
        );

        assertThrows(IllegalArgumentException.class, () -> guestService.create(1L, form));
    }

    @Test
    void rejectsInvalidNifLetter() {
        GuestForm form = new GuestForm(
            "Ana", "Lopez", "Martin", es.checkpol.domain.DocumentType.NIF, "00000000A", "SUP123", LocalDate.now().minusYears(30),
            "ESP", GuestSex.M, 1L, "+34 600000000", "", "", "PM"
        );

        assertThrows(IllegalArgumentException.class, () -> guestService.create(1L, form));
    }

    @Test
    void acceptsGuestWithOnlyPhone2AsContact() {
        GuestForm form = new GuestForm(
            "Ana", "Lopez", "Martin", es.checkpol.domain.DocumentType.NIF, "00000000T", "SUP123", LocalDate.now().minusYears(30),
            "ESP", GuestSex.M, 1L, "", "+34 600000001", "", ""
        );

        assertDoesNotThrow(() -> guestService.create(1L, form));
    }

    @Test
    void rejectsNifWithoutDocumentSupport() {
        GuestForm form = new GuestForm(
            "Ana", "Lopez", "Martin", es.checkpol.domain.DocumentType.NIF, "00000000T", "", LocalDate.now().minusYears(30),
            "ESP", GuestSex.M, 1L, "+34 600000000", "", "", ""
        );

        assertThrows(IllegalArgumentException.class, () -> guestService.create(1L, form));
    }

    private Booking sampleBooking(LocalDate checkInDate) {
        return new Booking(
            new Accommodation("Casa Olivo", "H123456789", "VT-123"),
            "ABC123",
            2,
            checkInDate.minusDays(3),
            BookingChannel.DIRECT,
            checkInDate,
            checkInDate.plusDays(2),
            PaymentType.EFECT,
            checkInDate.minusDays(3),
            null,
            null,
            null
        );
    }

    private Address sampleAddress(Booking booking) {
        Address address = new Address(
            booking,
            "Calle Mayor 1",
            null,
            "28079",
            "Madrid",
            "Madrid",
            MunicipalityResolutionStatus.EXACT,
            "Municipio resuelto automaticamente.",
            "28001",
            "ESP"
        );
        ReflectionTestUtils.setField(address, "id", 1L);
        return address;
    }
}
