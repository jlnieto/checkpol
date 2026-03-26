package es.checkpol.service;

import es.checkpol.domain.Address;
import es.checkpol.domain.Booking;
import es.checkpol.domain.Guest;

import java.util.List;

public record GuestSelfServiceDetails(
    Booking booking,
    long guestCount,
    long expectedGuestCount,
    List<Guest> guests,
    List<Address> addresses
) {
}
