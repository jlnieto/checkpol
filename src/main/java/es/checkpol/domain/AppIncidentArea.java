package es.checkpol.domain;

public enum AppIncidentArea {
    ADMIN("Admin"),
    OWNER("Owner"),
    PUBLIC_GUEST("Huéspedes"),
    SES("SES"),
    SYSTEM("Sistema");

    private final String label;

    AppIncidentArea(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
