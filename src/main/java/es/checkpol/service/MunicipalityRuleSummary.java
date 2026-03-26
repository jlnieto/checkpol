package es.checkpol.service;

import java.time.OffsetDateTime;

public record MunicipalityRuleSummary(
    String countryCode,
    String postalCodePrefix,
    String municipalityQueryLabel,
    String municipalityCode,
    String municipalityName,
    OffsetDateTime updatedAt
) {
}
