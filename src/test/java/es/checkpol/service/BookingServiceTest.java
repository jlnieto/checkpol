package es.checkpol.service;

import es.checkpol.domain.Accommodation;
import es.checkpol.domain.Address;
import es.checkpol.domain.AppUser;
import es.checkpol.domain.AppUserRole;
import es.checkpol.domain.Booking;
import es.checkpol.domain.BookingChannel;
import es.checkpol.domain.DocumentType;
import es.checkpol.domain.Guest;
import es.checkpol.domain.GuestReviewStatus;
import es.checkpol.domain.GuestSex;
import es.checkpol.domain.GuestSubmissionSource;
import es.checkpol.domain.PaymentType;
import es.checkpol.repository.AccommodationRepository;
import es.checkpol.repository.BookingRepository;
import es.checkpol.repository.GeneratedCommunicationRepository;
import es.checkpol.repository.GuestRepository;
import es.checkpol.web.BookingForm;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

class BookingServiceTest {

    private final BookingRepository bookingRepository = Mockito.mock(BookingRepository.class);
    private final AccommodationRepository accommodationRepository = Mockito.mock(AccommodationRepository.class);
    private final GuestRepository guestRepository = Mockito.mock(GuestRepository.class);
    private final GeneratedCommunicationRepository generatedCommunicationRepository = Mockito.mock(GeneratedCommunicationRepository.class);
    private final CurrentAppUserService currentAppUserService = Mockito.mock(CurrentAppUserService.class);
    private final BookingService bookingService = new BookingService(
        bookingRepository,
        accommodationRepository,
        guestRepository,
        generatedCommunicationRepository,
        currentAppUserService
    );

    @Test
    void rejectsCheckoutOnOrBeforeCheckin() {
        BookingForm form = new BookingForm(
            1L,
            "ABC123",
            2,
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(2),
            LocalDate.now().plusDays(2)
        );

        assertThrows(IllegalArgumentException.class, () -> bookingService.create(form));
    }

    @Test
    void rejectsUnknownAccommodation() {
        Mockito.when(currentAppUserService.requireCurrentUserId()).thenReturn(7L);
        Mockito.when(currentAppUserService.requireCurrentUserEntity()).thenReturn(sampleOwner());
        Mockito.when(accommodationRepository.findByIdAndOwnerId(99L, 7L)).thenReturn(Optional.empty());

        BookingForm form = new BookingForm(
            99L,
            "ABC123",
            2,
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(2),
            LocalDate.now().plusDays(4)
        );

        assertThrows(IllegalArgumentException.class, () -> bookingService.create(form));
    }

    @Test
    void storesAirbnbDefaultsForBookingMetadata() {
        Accommodation accommodation = sampleAccommodation();
        Mockito.when(currentAppUserService.requireCurrentUserEntity()).thenReturn(sampleOwner());
        Mockito.when(accommodationRepository.findByIdAndOwnerId(1L, 7L)).thenReturn(Optional.of(accommodation));
        Mockito.when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = bookingService.create(new BookingForm(
            1L,
            "ABC123",
            2,
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(2),
            LocalDate.now().plusDays(4)
        ));

        assertEquals(es.checkpol.domain.BookingChannel.AIRBNB, booking.getChannel());
        assertEquals(2, booking.getPersonCount());
        assertEquals(PaymentType.PLATF, booking.getPaymentType());
        assertEquals("Airbnb", booking.getPaymentMethod());
    }

    @Test
    void marksBookingReadyWhenGuestCountMatchesAndAllGuestsAreReviewedAndExportable() {
        Booking booking = sampleBooking(2);
        Mockito.when(currentAppUserService.requireCurrentUserId()).thenReturn(7L);
        Mockito.when(bookingRepository.findDetailById(1L, 7L)).thenReturn(Optional.of(booking));
        Mockito.when(guestRepository.findAllByBookingIdAndBookingOwnerIdOrderByIdAsc(1L, 7L)).thenReturn(List.of(
            sampleGuest(booking, 1L, GuestReviewStatus.REVIEWED, sampleSpanishAddress(booking, "28079")),
            sampleGuest(booking, 2L, GuestReviewStatus.REVIEWED, sampleSpanishAddress(booking, "28079"))
        ));
        Mockito.when(generatedCommunicationRepository.findAllByBookingIdAndBookingOwnerIdOrderByGeneratedAtDesc(1L, 7L)).thenReturn(List.of());

        BookingDetails details = bookingService.getDetails(1L);

        assertEquals(true, details.readyForTravelerPart());
        assertEquals(false, details.guestCountMismatch());
        assertEquals(BookingOperationalStatus.READY_FOR_XML, details.operationalStatus());
    }

    @Test
    void blocksBookingWhenGuestCountIsLowerThanExpected() {
        Booking booking = sampleBooking(2);
        Mockito.when(currentAppUserService.requireCurrentUserId()).thenReturn(7L);
        Mockito.when(bookingRepository.findDetailById(1L, 7L)).thenReturn(Optional.of(booking));
        Mockito.when(guestRepository.findAllByBookingIdAndBookingOwnerIdOrderByIdAsc(1L, 7L)).thenReturn(List.of(
            sampleGuest(booking, 1L, GuestReviewStatus.REVIEWED, sampleSpanishAddress(booking, "28079"))
        ));
        Mockito.when(generatedCommunicationRepository.findAllByBookingIdAndBookingOwnerIdOrderByGeneratedAtDesc(1L, 7L)).thenReturn(List.of());

        BookingDetails details = bookingService.getDetails(1L);

        assertEquals(false, details.readyForTravelerPart());
        assertEquals(true, details.guestCountMismatch());
        assertEquals(BookingOperationalStatus.GUEST_COUNT_MISMATCH, details.operationalStatus());
    }

    @Test
    void blocksBookingWhenGuestCountIsHigherThanExpected() {
        Booking booking = sampleBooking(2);
        Mockito.when(currentAppUserService.requireCurrentUserId()).thenReturn(7L);
        Mockito.when(bookingRepository.findDetailById(1L, 7L)).thenReturn(Optional.of(booking));
        Mockito.when(guestRepository.findAllByBookingIdAndBookingOwnerIdOrderByIdAsc(1L, 7L)).thenReturn(List.of(
            sampleGuest(booking, 1L, GuestReviewStatus.REVIEWED, sampleSpanishAddress(booking, "28079")),
            sampleGuest(booking, 2L, GuestReviewStatus.REVIEWED, sampleSpanishAddress(booking, "28079")),
            sampleGuest(booking, 3L, GuestReviewStatus.REVIEWED, sampleSpanishAddress(booking, "28079"))
        ));
        Mockito.when(generatedCommunicationRepository.findAllByBookingIdAndBookingOwnerIdOrderByGeneratedAtDesc(1L, 7L)).thenReturn(List.of());

        BookingDetails details = bookingService.getDetails(1L);

        assertEquals(false, details.readyForTravelerPart());
        assertEquals(true, details.guestCountMismatch());
        assertEquals(BookingOperationalStatus.GUEST_COUNT_MISMATCH, details.operationalStatus());
    }

    @Test
    void blocksBookingWhenThereAreGuestsPendingReview() {
        Booking booking = sampleBooking(1);
        Mockito.when(currentAppUserService.requireCurrentUserId()).thenReturn(7L);
        Mockito.when(bookingRepository.findDetailById(1L, 7L)).thenReturn(Optional.of(booking));
        Mockito.when(guestRepository.findAllByBookingIdAndBookingOwnerIdOrderByIdAsc(1L, 7L)).thenReturn(List.of(
            sampleGuest(booking, 1L, GuestReviewStatus.PENDING_REVIEW, sampleSpanishAddress(booking, "28079"))
        ));
        Mockito.when(generatedCommunicationRepository.findAllByBookingIdAndBookingOwnerIdOrderByGeneratedAtDesc(1L, 7L)).thenReturn(List.of());

        BookingDetails details = bookingService.getDetails(1L);

        assertEquals(false, details.readyForTravelerPart());
        assertEquals(1L, details.pendingReviewGuestCount());
        assertEquals(BookingOperationalStatus.REVIEW_PENDING, details.operationalStatus());
    }

    @Test
    void blocksBookingWhenSpanishAddressHasNoExportableMunicipalityCode() {
        Booking booking = sampleBooking(1);
        Mockito.when(currentAppUserService.requireCurrentUserId()).thenReturn(7L);
        Mockito.when(bookingRepository.findDetailById(1L, 7L)).thenReturn(Optional.of(booking));
        Mockito.when(guestRepository.findAllByBookingIdAndBookingOwnerIdOrderByIdAsc(1L, 7L)).thenReturn(List.of(
            sampleGuest(booking, 1L, GuestReviewStatus.REVIEWED, sampleSpanishAddress(booking, null))
        ));
        Mockito.when(generatedCommunicationRepository.findAllByBookingIdAndBookingOwnerIdOrderByGeneratedAtDesc(1L, 7L)).thenReturn(List.of());

        BookingDetails details = bookingService.getDetails(1L);

        assertEquals(false, details.readyForTravelerPart());
        assertEquals(true, details.blockedByAddressExport());
        assertEquals(BookingOperationalStatus.INCOMPLETE, details.operationalStatus());
    }

    private Booking sampleBooking(int personCount) {
        AppUser owner = sampleOwner();
        return new Booking(
            owner,
            new Accommodation(owner, "Casa Olivo", "H123456789", "VT-123"),
            "ABC123",
            personCount,
            LocalDate.of(2026, 3, 20),
            BookingChannel.DIRECT,
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 4, 4),
            PaymentType.EFECT,
            LocalDate.of(2026, 3, 20),
            "Caja",
            "Ana Lopez",
            null
        );
    }

    private Accommodation sampleAccommodation() {
        return new Accommodation(sampleOwner(), "Casa Olivo", "H123456789", "VT-123");
    }

    private AppUser sampleOwner() {
        AppUser owner = new AppUser("owner", "hash", "Owner", AppUserRole.OWNER, true, java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now());
        ReflectionTestUtils.setField(owner, "id", 7L);
        return owner;
    }

    private Guest sampleGuest(Booking booking, Long id, GuestReviewStatus reviewStatus, Address address) {
        Guest guest = new Guest(
            booking,
            "Ana",
            "Lopez",
            "Martin",
            DocumentType.NIF,
            "00000000T",
            "SUP123",
            LocalDate.of(1990, 5, 1),
            "ESP",
            GuestSex.M,
            address,
            "+34 600000000",
            "",
            "",
            "",
            GuestSubmissionSource.MANUAL,
            reviewStatus,
            null
        );
        ReflectionTestUtils.setField(guest, "id", id);
        return guest;
    }

    private Address sampleSpanishAddress(Booking booking, String municipalityCode) {
        return new Address(
            booking,
            "Calle Mayor 1",
            null,
            municipalityCode,
            "Madrid",
            "28001",
            "ESP"
        );
    }
}
