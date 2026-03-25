package es.checkpol.service;

public enum BookingOperationalStatus {
    WAITING_GUESTS("Sin huespedes"),
    REVIEW_PENDING("Revision pendiente"),
    INCOMPLETE("Incompleta"),
    READY_FOR_XML("Lista para XML"),
    XML_GENERATED("XML generado");

    private final String label;

    BookingOperationalStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
