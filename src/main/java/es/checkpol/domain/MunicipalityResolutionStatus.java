package es.checkpol.domain;

public enum MunicipalityResolutionStatus {
    EXACT("Exacta", false),
    LEARNED("Aprendida", false),
    APPROXIMATED("Aproximada", true),
    PROVINCE_FALLBACK("Fallback provincial", true),
    FORCED_FALLBACK("Fallback forzado", true),
    MANUAL_OVERRIDE("Corregida manualmente", false),
    NOT_REQUIRED("No aplica", false);

    private final String label;
    private final boolean requiresReview;

    MunicipalityResolutionStatus(String label, boolean requiresReview) {
        this.label = label;
        this.requiresReview = requiresReview;
    }

    public String getLabel() {
        return label;
    }

    public boolean requiresReview() {
        return requiresReview;
    }
}
