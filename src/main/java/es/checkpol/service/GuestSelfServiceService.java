package es.checkpol.service;

import es.checkpol.domain.Booking;
import es.checkpol.domain.Guest;
import es.checkpol.repository.AddressRepository;
import es.checkpol.repository.BookingRepository;
import es.checkpol.repository.GuestRepository;
import es.checkpol.web.AddressForm;
import es.checkpol.web.GuestForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class GuestSelfServiceService {

    private final BookingRepository bookingRepository;
    private final AddressRepository addressRepository;
    private final GuestRepository guestRepository;
    private final GuestService guestService;
    private final AddressService addressService;

    public GuestSelfServiceService(
        BookingRepository bookingRepository,
        AddressRepository addressRepository,
        GuestRepository guestRepository,
        GuestService guestService,
        AddressService addressService
    ) {
        this.bookingRepository = bookingRepository;
        this.addressRepository = addressRepository;
        this.guestRepository = guestRepository;
        this.guestService = guestService;
        this.addressService = addressService;
    }

    @Transactional
    public SelfServiceAccess issueAccess(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("La estancia seleccionada no existe."));
        String token = UUID.randomUUID().toString().replace("-", "");
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(7);
        booking.updateSelfServiceAccess(token, expiresAt);
        return new SelfServiceAccess(token, expiresAt);
    }

    @Transactional(readOnly = true)
    public GuestSelfServiceDetails getByToken(String token) {
        Booking booking = bookingRepository.findBySelfServiceToken(token)
            .orElseThrow(() -> new IllegalArgumentException("El enlace indicado no existe."));
        validateToken(booking);
        var bookingGuests = guestRepository.findAllByBookingIdOrderByIdAsc(booking.getId());
        return new GuestSelfServiceDetails(
            booking,
            guestRepository.countByBookingId(booking.getId()),
            booking.getPersonCount() == null ? 0 : booking.getPersonCount(),
            bookingGuests.stream()
                .filter(guest -> guest.getSubmissionSource() == es.checkpol.domain.GuestSubmissionSource.SELF_SERVICE)
                .toList(),
            addressRepository.findAllByBookingIdOrderByIdAsc(booking.getId())
        );
    }

    @Transactional
    public void submitGuest(String token, GuestForm form) {
        Booking booking = bookingRepository.findBySelfServiceToken(token)
            .orElseThrow(() -> new IllegalArgumentException("El enlace indicado no existe."));
        validateToken(booking);
        guestService.createFromSelfService(booking.getId(), form);
    }

    @Transactional(readOnly = true)
    public GuestForm getGuestForm(String token, Long guestId) {
        return toGuestForm(findBookingGuest(token, guestId));
    }

    @Transactional
    public void updateGuest(String token, Long guestId, GuestForm form) {
        findBookingGuest(token, guestId);
        guestService.updateFromSelfService(guestId, form);
    }

    @Transactional
    public Long createAddress(String token, AddressForm form) {
        Booking booking = bookingRepository.findBySelfServiceToken(token)
            .orElseThrow(() -> new IllegalArgumentException("El enlace indicado no existe."));
        validateToken(booking);
        return addressService.createForSelfService(booking.getId(), form).getId();
    }

    @Transactional
    public void revokeAccess(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("La estancia seleccionada no existe."));
        booking.updateSelfServiceAccess(null, null);
    }

    private void validateToken(Booking booking) {
        if (booking.getSelfServiceExpiresAt() == null || booking.getSelfServiceExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("El enlace ha caducado.");
        }
    }

    private Guest findBookingGuest(String token, Long guestId) {
        Booking booking = bookingRepository.findBySelfServiceToken(token)
            .orElseThrow(() -> new IllegalArgumentException("El enlace indicado no existe."));
        validateToken(booking);
        return guestRepository.findByIdAndBookingId(guestId, booking.getId())
            .orElseThrow(() -> new IllegalArgumentException("El huésped indicado no pertenece a este enlace."));
    }

    private GuestForm toGuestForm(Guest guest) {
        return new GuestForm(
            guest.getFirstName(),
            guest.getLastName1(),
            guest.getLastName2() == null ? "" : guest.getLastName2(),
            guest.getDocumentType(),
            guest.getDocumentNumber() == null ? "" : guest.getDocumentNumber(),
            guest.getDocumentSupport() == null ? "" : guest.getDocumentSupport(),
            guest.getBirthDate(),
            guest.getNationality() == null ? "" : guest.getNationality(),
            guest.getSex(),
            guest.getAddressId(),
            guest.getPhone() == null ? "" : guest.getPhone(),
            guest.getPhone2() == null ? "" : guest.getPhone2(),
            guest.getEmail() == null ? "" : guest.getEmail(),
            guest.getRelationship() == null ? "" : guest.getRelationship()
        );
    }
}
