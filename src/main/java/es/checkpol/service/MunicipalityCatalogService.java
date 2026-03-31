package es.checkpol.service;

import es.checkpol.domain.MunicipalityCatalogEntry;
import es.checkpol.repository.MunicipalityCatalogEntryRepository;
import es.checkpol.repository.PostalCodeMunicipalityMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MunicipalityCatalogService {

    private static final String SPAIN = "ESP";

    private final MunicipalityCatalogEntryRepository municipalityCatalogEntryRepository;
    private final PostalCodeMunicipalityMappingRepository postalCodeMunicipalityMappingRepository;

    public MunicipalityCatalogService(
        MunicipalityCatalogEntryRepository municipalityCatalogEntryRepository,
        PostalCodeMunicipalityMappingRepository postalCodeMunicipalityMappingRepository
    ) {
        this.municipalityCatalogEntryRepository = municipalityCatalogEntryRepository;
        this.postalCodeMunicipalityMappingRepository = postalCodeMunicipalityMappingRepository;
    }

    @Transactional(readOnly = true)
    public boolean hasSpanishCatalogData() {
        return municipalityCatalogEntryRepository.existsByCountryCodeAndActiveTrue(SPAIN);
    }

    @Transactional(readOnly = true)
    public Optional<MunicipalityCatalogEntry> findSpanishMunicipalityByCode(String municipalityCode) {
        if (municipalityCode == null || municipalityCode.isBlank()) {
            return Optional.empty();
        }
        return municipalityCatalogEntryRepository.findByCountryCodeAndMunicipalityCodeAndActiveTrue(SPAIN, municipalityCode.trim());
    }

    @Transactional(readOnly = true)
    public Optional<MunicipalityCatalogEntry> findSpanishMunicipalityByPostalCodeAndCode(String postalCode, String municipalityCode) {
        if (postalCode == null || postalCode.isBlank() || municipalityCode == null || municipalityCode.isBlank()) {
            return Optional.empty();
        }
        boolean mappingExists = postalCodeMunicipalityMappingRepository
            .findByPostalCodeAndMunicipalityCodeAndActiveTrue(postalCode.trim(), municipalityCode.trim())
            .isPresent();
        if (!mappingExists) {
            return Optional.empty();
        }
        return municipalityCatalogEntryRepository.findByCountryCodeAndMunicipalityCodeAndActiveTrue(SPAIN, municipalityCode.trim());
    }

    @Transactional(readOnly = true)
    public List<MunicipalityCatalogEntry> findSpanishMunicipalitiesByPostalCode(String postalCode) {
        if (postalCode == null || postalCode.isBlank()) {
            return List.of();
        }
        List<String> municipalityCodes = postalCodeMunicipalityMappingRepository.findAllByPostalCodeAndActiveTrueOrderByMunicipalityCodeAsc(postalCode.trim()).stream()
            .map(mapping -> mapping.getMunicipalityCode().trim())
            .distinct()
            .toList();
        if (municipalityCodes.isEmpty()) {
            return List.of();
        }
        List<MunicipalityCatalogEntry> entries = municipalityCatalogEntryRepository.findAllByMunicipalityCodeInAndActiveTrueOrderByMunicipalityNameAsc(municipalityCodes);
        Map<String, MunicipalityCatalogEntry> uniqueByCode = new LinkedHashMap<>();
        for (MunicipalityCatalogEntry entry : entries) {
            uniqueByCode.put(entry.getMunicipalityCode(), entry);
        }
        return List.copyOf(uniqueByCode.values());
    }

    @Transactional(readOnly = true)
    public List<MunicipalityOption> findSpanishOptionsByPostalCode(String postalCode) {
        return findSpanishMunicipalitiesByPostalCode(postalCode).stream()
            .map(entry -> new MunicipalityOption(
                entry.getMunicipalityCode(),
                entry.getMunicipalityName(),
                entry.getProvinceCode(),
                entry.getProvinceName()
            ))
            .toList();
    }

    public record MunicipalityOption(
        String municipalityCode,
        String municipalityName,
        String provinceCode,
        String provinceName
    ) {
    }
}
