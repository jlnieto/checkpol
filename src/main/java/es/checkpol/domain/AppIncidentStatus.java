package es.checkpol.domain;

public enum AppIncidentStatus {
    OPEN("Abierta"),
    REVIEWED("Revisada"),
    RESOLVED("Resuelta");

    private final String label;

    AppIncidentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
