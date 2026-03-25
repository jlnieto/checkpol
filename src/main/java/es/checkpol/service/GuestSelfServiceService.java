package es.checkpol.service;

import es.checkpol.domain.Booking;
import es.checkpol.repository.BookingRepository;
import es.checkpol.repository.GuestRepository;
import es.checkpol.web.GuestForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class GuestSelfServiceService {

    private final BookingRepository bookingRepository;
    private final GuestRepository guestRepository;
    private final GuestService guestService;

    public GuestSelfServiceService(
        BookingRepository bookingRepository,
        GuestRepository guestRepository,
        GuestService guestService
    ) {
        this.bookingRepository = bookingRepository;
        this.guestRepository = guestRepository;
        this.guestService = guestService;
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
        return new GuestSelfServiceDetails(
            booking,
            guestRepository.countByBookingId(booking.getId()),
            guestRepository.findAllByBookingIdOrderByIdAsc(booking.getId()).stream()
                .filter(guest -> guest.getSubmissionSource() == es.checkpol.domain.GuestSubmissionSource.SELF_SERVICE)
                .toList()
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
        GuestSelfServiceDetails details = getByToken(token);
        boolean allowed = details.guests().stream().anyMatch(guest -> guest.getId().equals(guestId));
        if (!allowed) {
            throw new IllegalArgumentException("El huesped indicado no pertenece a este enlace.");
        }
        return guestService.getForm(guestId);
    }

    @Transactional
    public void updateGuest(String token, Long guestId, GuestForm form) {
        GuestSelfServiceDetails details = getByToken(token);
        boolean allowed = details.guests().stream().anyMatch(guest -> guest.getId().equals(guestId));
        if (!allowed) {
            throw new IllegalArgumentException("El huesped indicado no pertenece a este enlace.");
        }
        guestService.updateFromSelfService(guestId, form);
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
}
