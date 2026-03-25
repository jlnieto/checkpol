package es.checkpol.service;

import es.checkpol.domain.Booking;

public record BookingListItem(
    Booking booking,
    long guestCount,
    boolean readyForTravelerPart,
    BookingOperationalStatus operationalStatus
) {
}
