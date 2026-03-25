package es.checkpol.service;

import es.checkpol.domain.Booking;
import es.checkpol.domain.GeneratedCommunication;
import es.checkpol.domain.Guest;

import java.util.List;
import java.util.Optional;

public record BookingDetails(
    Booking booking,
    List<Guest> guests,
    boolean readyForTravelerPart,
    Optional<GeneratedCommunication> lastGeneratedCommunication,
    int generatedCommunicationCount,
    List<GeneratedCommunication> generatedCommunications,
    Optional<SelfServiceAccess> selfServiceAccess,
    BookingOperationalStatus operationalStatus,
    long pendingReviewGuestCount
) {
}
