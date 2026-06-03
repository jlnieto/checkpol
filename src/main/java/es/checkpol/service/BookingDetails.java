package es.checkpol.service;

import es.checkpol.domain.Booking;
import es.checkpol.domain.GeneratedCommunication;
import es.checkpol.domain.Guest;

import java.util.List;
import java.util.Optional;

public record BookingDetails(
    Booking booking,
    List<Guest> guests,
    long guestCount,
    int expectedGuestCount,
    boolean guestCountMismatch,
    boolean readyForTravelerPart,
    boolean sesSubmissionAvailable,
    Optional<GeneratedCommunication> lastGeneratedCommunication,
    boolean lastGeneratedCommunicationMatchesCurrentXml,
    int generatedCommunicationCount,
    List<GeneratedCommunication> generatedCommunications,
    Optional<SelfServiceAccess> selfServiceAccess,
    BookingOperationalStatus operationalStatus,
    long pendingReviewGuestCount,
    long selfServiceGuestCount,
    boolean blockedByBookingData,
    boolean blockedByGuestData,
    boolean blockedByPendingReview,
    boolean blockedByAddressExport,
    String blockingSummary,
    String blockingMessage,
    List<String> blockingReasons
) {
    public boolean canDelete() {
        return !booking.isArchived() && generatedCommunicationCount == 0;
    }

    public boolean canArchive() {
        return !booking.isArchived() && generatedCommunicationCount > 0;
    }

    public BookingDetails(
        Booking booking,
        List<Guest> guests,
        long guestCount,
        int expectedGuestCount,
        boolean guestCountMismatch,
        boolean readyForTravelerPart,
        boolean sesSubmissionAvailable,
        Optional<GeneratedCommunication> lastGeneratedCommunication,
        int generatedCommunicationCount,
        List<GeneratedCommunication> generatedCommunications,
        Optional<SelfServiceAccess> selfServiceAccess,
        BookingOperationalStatus operationalStatus,
        long pendingReviewGuestCount,
        long selfServiceGuestCount,
        boolean blockedByBookingData,
        boolean blockedByGuestData,
        boolean blockedByPendingReview,
        boolean blockedByAddressExport,
        String blockingSummary,
        String blockingMessage,
        List<String> blockingReasons
    ) {
        this(
            booking,
            guests,
            guestCount,
            expectedGuestCount,
            guestCountMismatch,
            readyForTravelerPart,
            sesSubmissionAvailable,
            lastGeneratedCommunication,
            false,
            generatedCommunicationCount,
            generatedCommunications,
            selfServiceAccess,
            operationalStatus,
            pendingReviewGuestCount,
            selfServiceGuestCount,
            blockedByBookingData,
            blockedByGuestData,
            blockedByPendingReview,
            blockedByAddressExport,
            blockingSummary,
            blockingMessage,
            blockingReasons
        );
    }
}
