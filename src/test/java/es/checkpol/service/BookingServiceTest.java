package es.checkpol.service;

import es.checkpol.domain.Accommodation;
import es.checkpol.domain.BookingChannel;
import es.checkpol.domain.PaymentType;
import es.checkpol.repository.AccommodationRepository;
import es.checkpol.repository.BookingRepository;
import es.checkpol.repository.GeneratedCommunicationRepository;
import es.checkpol.repository.GuestRepository;
import es.checkpol.web.BookingForm;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingServiceTest {

    private final BookingRepository bookingRepository = Mockito.mock(BookingRepository.class);
    private final AccommodationRepository accommodationRepository = Mockito.mock(AccommodationRepository.class);
    private final GuestRepository guestRepository = Mockito.mock(GuestRepository.class);
    private final GeneratedCommunicationRepository generatedCommunicationRepository = Mockito.mock(GeneratedCommunicationRepository.class);
    private final BookingService bookingService = new BookingService(
        bookingRepository,
        accommodationRepository,
        guestRepository,
        generatedCommunicationRepository
    );

    @Test
    void rejectsCheckoutOnOrBeforeCheckin() {
        BookingForm form = new BookingForm(
            1L,
            BookingChannel.DIRECT,
            "ABC123",
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(2),
            LocalDate.now().plusDays(2),
            PaymentType.EFECT,
            null,
            "",
            "",
            ""
        );

        assertThrows(IllegalArgumentException.class, () -> bookingService.create(form));
    }

    @Test
    void rejectsUnknownAccommodation() {
        Mockito.when(accommodationRepository.findById(99L)).thenReturn(Optional.empty());

        BookingForm form = new BookingForm(
            99L,
            BookingChannel.DIRECT,
            "ABC123",
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(2),
            LocalDate.now().plusDays(4),
            PaymentType.EFECT,
            null,
            "",
            "",
            ""
        );

        assertThrows(IllegalArgumentException.class, () -> bookingService.create(form));
    }
}
