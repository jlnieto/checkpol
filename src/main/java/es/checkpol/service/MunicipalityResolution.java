package es.checkpol.service;

import es.checkpol.domain.MunicipalityResolutionStatus;

public record MunicipalityResolution(
    String municipalityCode,
    String municipalityResolvedName,
    MunicipalityResolutionStatus status,
    String note,
    String municipalityQueryNormalized,
    String municipalityQueryLabel,
    String postalCodePrefix
) {
}
