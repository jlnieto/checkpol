package es.checkpol.domain;

public enum GuestSubmissionSource {
    MANUAL("Manual"),
    SELF_SERVICE("Enlace huesped");

    private final String label;

    GuestSubmissionSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
