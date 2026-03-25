package es.checkpol.service;

public enum BookingFilter {
    ALL,
    INCOMPLETE,
    READY,
    TODAY,
    UPCOMING;

    public static BookingFilter fromParam(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }
        return switch (value.toLowerCase()) {
            case "incomplete" -> INCOMPLETE;
            case "ready" -> READY;
            case "today" -> TODAY;
            case "upcoming" -> UPCOMING;
            default -> ALL;
        };
    }
}
