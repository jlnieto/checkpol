package es.checkpol.service;

import es.checkpol.domain.Accommodation;
import es.checkpol.domain.Booking;
import es.checkpol.domain.BookingChannel;
import es.checkpol.domain.PaymentType;
import es.checkpol.repository.BookingRepository;
import es.checkpol.repository.GuestRepository;
import es.checkpol.web.GuestForm;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

class GuestServiceTest {

    private final GuestRepository guestRepository = Mockito.mock(GuestRepository.class);
    private final BookingRepository bookingRepository = Mockito.mock(BookingRepository.class);
    private final GuestService guestService = new GuestService(guestRepository, bookingRepository);

    GuestServiceTest() {
        Mockito.when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking(LocalDate.now().plusDays(2))));
    }

    @Test
    void rejectsGuestWithoutMunicipality() {
        GuestForm form = new GuestForm(
            "Ana", "Lopez", "", null, "", "", LocalDate.now().minusYears(30),
            "ESP", null, "Calle Mayor 1", "", "", "", "28001", "ESP", "600000000", "", "", ""
        );

        assertThrows(IllegalArgumentException.class, () -> guestService.create(1L, form));
    }

    @Test
    void rejectsGuestWithoutContact() {
        GuestForm form = new GuestForm(
            "Ana", "Lopez", "", null, "", "", LocalDate.now().minusYears(30),
            "ESP", null, "Calle Mayor 1", "", "28079", "", "28001", "ESP", "", "", "", ""
        );

        assertThrows(IllegalArgumentException.class, () -> guestService.create(1L, form));
    }

    @Test
    void rejectsInvalidNifWithoutSecondSurname() {
        GuestForm form = new GuestForm(
            "Ana", "Lopez", "", es.checkpol.domain.DocumentType.NIF, "00000000T", "SUP123", LocalDate.now().minusYears(30),
            "ESP", null, "Calle Mayor 1", "", "28079", "", "28001", "ESP", "600000000", "", "", ""
        );

        assertThrows(IllegalArgumentException.class, () -> guestService.create(1L, form));
    }

    @Test
    void rejectsMinorWithoutRelationship() {
        GuestForm form = new GuestForm(
            "Ana", "Lopez", "Martin", null, "", "", LocalDate.now().minusYears(10),
            "ESP", null, "Calle Mayor 1", "", "28079", "", "28001", "ESP", "600000000", "", "", ""
        );

        assertThrows(IllegalArgumentException.class, () -> guestService.create(1L, form));
    }

    @Test
    void rejectsAdultWithoutDocument() {
        GuestForm form = new GuestForm(
            "Ana", "Lopez", "Martin", null, "", "", LocalDate.now().minusYears(30),
            "ESP", null, "Calle Mayor 1", "", "28079", "", "28001", "ESP", "600000000", "", "", "PM"
        );

        assertThrows(IllegalArgumentException.class, () -> guestService.create(1L, form));
    }

    @Test
    void rejectsSpanishGuestOlderThan14WithoutDocument() {
        Mockito.when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking(LocalDate.of(2026, 4, 1))));
        GuestForm form = new GuestForm(
            "Ana", "Lopez", "Martin", null, "", "", LocalDate.of(2010, 3, 1),
            "ESP", null, "Calle Mayor 1", "", "28079", "", "28001", "ESP", "600000000", "", "", "PM"
        );

        assertThrows(IllegalArgumentException.class, () -> guestService.create(1L, form));
    }

    @Test
    void rejectsInvalidNifLetter() {
        GuestForm form = new GuestForm(
            "Ana", "Lopez", "Martin", es.checkpol.domain.DocumentType.NIF, "00000000A", "SUP123", LocalDate.now().minusYears(30),
            "ESP", null, "Calle Mayor 1", "", "28079", "", "28001", "ESP", "600000000", "", "", "PM"
        );

        assertThrows(IllegalArgumentException.class, () -> guestService.create(1L, form));
    }

    private Booking sampleBooking(LocalDate checkInDate) {
        return new Booking(
            new Accommodation("Casa Olivo", "H123456789", "VT-123"),
            "ABC123",
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
}
