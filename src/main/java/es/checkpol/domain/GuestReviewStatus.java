package es.checkpol.domain;

public enum GuestReviewStatus {
    PENDING_REVIEW("Pendiente revision"),
    REVIEWED("Revisado");

    private final String label;

    GuestReviewStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
