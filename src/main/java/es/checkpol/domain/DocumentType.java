package es.checkpol.domain;

public enum DocumentType {
    NIF("NIF"),
    NIE("NIE"),
    PAS("Pasaporte"),
    OTRO("Otro");

    private final String label;

    DocumentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
