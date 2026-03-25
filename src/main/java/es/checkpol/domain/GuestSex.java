package es.checkpol.domain;

public enum GuestSex {
    H("Hombre"),
    M("Mujer"),
    O("Otro");

    private final String label;

    GuestSex(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
