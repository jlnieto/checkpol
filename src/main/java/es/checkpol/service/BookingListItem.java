package es.checkpol.service;

import es.checkpol.domain.Booking;
import es.checkpol.domain.CommunicationDispatchStatus;

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
    String communicationStatusSummary,
    CommunicationDispatchStatus communicationDispatchStatus
) {
    public boolean needsCommunicationAction() {
        if (!readyForTravelerPart) {
            return false;
        }
        if (communicationDispatchStatus == null) {
            return true;
        }
        return switch (communicationDispatchStatus) {
            case XML_READY, SUBMISSION_FAILED, SES_PROCESSING_ERROR, SES_CANCELLED -> true;
            case SUBMITTED_TO_SES, SES_PROCESSED, SES_RESPONSE_NEEDS_REVIEW -> false;
        };
    }
}
