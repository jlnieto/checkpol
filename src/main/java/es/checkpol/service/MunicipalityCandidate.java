package es.checkpol.service;

public record MunicipalityCandidate(
    String municipalityCode,
    String municipalityName,
    String provinceCode,
    String provinceName,
    String postalCode,
    String type
) {
}
