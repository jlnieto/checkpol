package es.checkpol.service;

import es.checkpol.domain.MunicipalityResolutionStatus;

import java.time.OffsetDateTime;

public record MunicipalityIssueSummary(
    Long id,
    Long bookingId,
    String bookingReference,
    String accommodationName,
    String guestDisplayName,
    String postalCode,
    String municipalityQueryLabel,
    String assignedMunicipalityCode,
    String assignedMunicipalityName,
    MunicipalityResolutionStatus resolutionStatus,
    String resolutionNote,
    OffsetDateTime createdAt
) {
}
