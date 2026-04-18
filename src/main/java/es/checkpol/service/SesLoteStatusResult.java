package es.checkpol.service;

import java.time.OffsetDateTime;

public record SesLoteStatusResult(
    int responseCode,
    String responseDescription,
    String loteCode,
    Integer processingStateCode,
    String processingStateDescription,
    String communicationCode,
    String processingErrorType,
    String processingErrorDescription,
    OffsetDateTime processedAt,
    String rawResponse
) {
    public SesLoteStatusResult(
        int responseCode,
        String responseDescription,
        String loteCode,
        Integer processingStateCode,
        String processingStateDescription,
        String communicationCode,
        String processingErrorType,
        String processingErrorDescription,
        OffsetDateTime processedAt
    ) {
        this(
            responseCode,
            responseDescription,
            loteCode,
            processingStateCode,
            processingStateDescription,
            communicationCode,
            processingErrorType,
            processingErrorDescription,
            processedAt,
            null
        );
    }
}
