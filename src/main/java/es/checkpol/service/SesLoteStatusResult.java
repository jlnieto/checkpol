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
    OffsetDateTime processedAt
) {
}
