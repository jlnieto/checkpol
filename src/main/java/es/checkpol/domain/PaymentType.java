package es.checkpol.domain;

public enum PaymentType {
    DESTI("Destino"),
    EFECT("Efectivo"),
    TARJT("Tarjeta"),
    PLATF("Plataforma"),
    TRANS("Transferencia"),
    MOVIL("Movil"),
    TREG("Tarjeta regalo"),
    OTRO("Otro");

    private final String label;

    PaymentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
