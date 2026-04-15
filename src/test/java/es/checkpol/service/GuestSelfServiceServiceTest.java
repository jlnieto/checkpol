package es.checkpol.service;

import es.checkpol.domain.Accommodation;
import es.checkpol.domain.Address;
import es.checkpol.domain.Booking;
import es.checkpol.domain.BookingChannel;
import es.checkpol.domain.DocumentType;
import es.checkpol.domain.Guest;
import es.checkpol.domain.GuestReviewStatus;
import es.checkpol.domain.GuestSex;
import es.checkpol.domain.GuestSubmissionSource;
import es.checkpol.domain.PaymentType;
import es.checkpol.repository.AddressRepository;
import es.checkpol.repository.BookingRepository;
import es.checkpol.repository.GuestRepository;
import es.checkpol.web.GuestForm;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuestSelfServiceServiceTest {

    private final BookingRepository bookingRepository = Mockito.mock(BookingRepository.class);
    private final AddressRepository addressRepository = Mockito.mock(AddressRepository.class);
    private final GuestRepository guestRepository = Mockito.mock(GuestRepository.class);
    private final GuestService guestService = Mockito.mock(GuestService.class);
    private final AddressService addressService = Mockito.mock(AddressService.class);

    private final GuestSelfServiceService service = new GuestSelfServiceService(
        bookingRepository,
        addressRepository,
        guestRepository,
        guestService,
        addressService
    );

    @Test
    void returnsPublicGuestFormWithoutOwnerSession() {
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
        booking.updateSelfServiceAccess("public-token", OffsetDateTime.now().plusDays(2));
        ReflectionTestUtils.setField(booking, "id", 9L);

        Address address = new Address(
            booking,
            "Calle Mayor 1",
            null,
            "28079",
            "Madrid",
            "28001",
            "ESP"
        );
        ReflectionTestUtils.setField(address, "id", 3L);

        Guest guest = new Guest(
            booking,
            "Jose Luis",
            "Nieto",
            "Hinojosa",
            DocumentType.NIF,
            "00000000T",
            "SUP123",
            LocalDate.now().minusYears(40),
            "ESP",
            GuestSex.M,
            address,
            "+34 600000000",
            null,
            null,
            null,
            GuestSubmissionSource.SELF_SERVICE,
            GuestReviewStatus.PENDING_REVIEW,
            OffsetDateTime.now()
        );
        ReflectionTestUtils.setField(guest, "id", 7L);

        Mockito.when(bookingRepository.findBySelfServiceToken("public-token")).thenReturn(Optional.of(booking));
        Mockito.when(guestRepository.findByIdAndBookingId(7L, 9L)).thenReturn(Optional.of(guest));

        GuestForm form = service.getGuestForm("public-token", 7L);

        assertEquals("Jose Luis", form.firstName());
        assertEquals("Nieto", form.lastName1());
        assertEquals("Hinojosa", form.lastName2());
        assertEquals(3L, form.addressId());
    }

    @Test
    void returnsPublicGuestFormForBookingGuestEvenIfItIsNotInSelfServiceCards() {
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
        booking.updateSelfServiceAccess("public-token", OffsetDateTime.now().plusDays(2));
        ReflectionTestUtils.setField(booking, "id", 9L);

        Address address = new Address(
            booking,
            "Calle Mayor 1",
            null,
            "28079",
            "Madrid",
            "28001",
            "ESP"
        );
        ReflectionTestUtils.setField(address, "id", 3L);

        Guest guest = new Guest(
            booking,
            "Jose Luis",
            "Nieto",
            "Hinojosa",
            DocumentType.NIF,
            "00000000T",
            "SUP123",
            LocalDate.now().minusYears(40),
            "ESP",
            GuestSex.M,
            address,
            "+34 600000000",
            null,
            null,
            null,
            GuestSubmissionSource.MANUAL,
            GuestReviewStatus.REVIEWED,
            null
        );
        ReflectionTestUtils.setField(guest, "id", 7L);

        Mockito.when(bookingRepository.findBySelfServiceToken("public-token")).thenReturn(Optional.of(booking));
        Mockito.when(guestRepository.findByIdAndBookingId(7L, 9L)).thenReturn(Optional.of(guest));

        GuestForm form = service.getGuestForm("public-token", 7L);

        assertEquals("Jose Luis", form.firstName());
        assertEquals("Nieto", form.lastName1());
    }
}
