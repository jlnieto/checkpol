package es.checkpol.service;

public record SesSubmissionResult(
    int responseCode,
    String responseDescription,
    String loteCode,
    String rawResponse
) {
    public SesSubmissionResult(int responseCode, String responseDescription, String loteCode) {
        this(responseCode, responseDescription, loteCode, null);
    }
}
