package es.checkpol.domain;

public enum AppIncidentSeverity {
    INFO("Info"),
    WARNING("Aviso"),
    ERROR("Error");

    private final String label;

    AppIncidentSeverity(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
