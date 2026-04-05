package es.checkpol.service;

public record SesSubmissionResult(
    int responseCode,
    String responseDescription,
    String loteCode
) {
}
