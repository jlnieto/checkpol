package es.checkpol.service;

import es.checkpol.domain.Booking;

public record BookingListItem(
    Booking booking,
    long guestCount,
    int expectedGuestCount,
    boolean readyForTravelerPart,
    boolean sesSubmissionAvailable,
    BookingOperationalStatus operationalStatus,
    long pendingReviewGuestCount,
    boolean guestCountMismatch,
    String blockingSummary,
    String communicationStatusSummary
) {
}
