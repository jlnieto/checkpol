package es.checkpol.domain;

public enum BookingChannel {
    DIRECT("Directa"),
    AIRBNB("Airbnb"),
    BOOKING("Booking.com"),
    OTHER("Otro");

    private final String label;

    BookingChannel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
