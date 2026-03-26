package es.checkpol.domain;

public enum MunicipalityIssueStatus {
    OPEN("Pendiente"),
    RESOLVED("Resuelta");

    private final String label;

    MunicipalityIssueStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
