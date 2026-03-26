package es.checkpol.service;

import es.checkpol.domain.MunicipalityResolutionRule;
import es.checkpol.domain.MunicipalityResolutionStatus;
import es.checkpol.repository.MunicipalityResolutionRuleRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MunicipalityResolverServiceTest {

    private final MunicipalityResolutionRuleRepository ruleRepository = Mockito.mock(MunicipalityResolutionRuleRepository.class);
    private final MunicipalityLookupClient lookupClient = Mockito.mock(MunicipalityLookupClient.class);
    private final MunicipalityResolverService resolverService = new MunicipalityResolverService(ruleRepository, lookupClient);

    @Test
    void usesLearnedRuleWhenAvailable() {
        MunicipalityResolutionRule rule = new MunicipalityResolutionRule(
            "ESP",
            "28",
            "madrd",
            "Madrd",
            "28079",
            "Madrid",
            OffsetDateTime.now(),
            OffsetDateTime.now()
        );
        Mockito.when(ruleRepository.findFirstByCountryCodeAndPostalCodePrefixAndMunicipalityQueryNormalized("ESP", "28", "madrd"))
            .thenReturn(Optional.of(rule));

        MunicipalityResolution resolution = resolverService.resolve("ESP", "28001", "", "Madrd");

        assertEquals("28079", resolution.municipalityCode());
        assertEquals("Madrid", resolution.municipalityResolvedName());
        assertEquals(MunicipalityResolutionStatus.LEARNED, resolution.status());
    }

    @Test
    void fallsBackToProvinceWhenNoMunicipalityMatchExists() {
        Mockito.when(ruleRepository.findFirstByCountryCodeAndPostalCodePrefixAndMunicipalityQueryNormalized("ESP", "28", "zzzz"))
            .thenReturn(Optional.empty());
        Mockito.when(ruleRepository.findFirstByCountryCodeAndPostalCodePrefixIsNullAndMunicipalityQueryNormalized("ESP", "zzzz"))
            .thenReturn(Optional.empty());
        Mockito.when(lookupClient.search("Zzzz")).thenReturn(List.of());
        Mockito.when(lookupClient.search("Madrid")).thenReturn(List.of(
            new MunicipalityCandidate("28079", "Madrid", "28", "Madrid", null, "Municipio")
        ));

        MunicipalityResolution resolution = resolverService.resolve("ESP", "28001", "", "Zzzz");

        assertEquals("28079", resolution.municipalityCode());
        assertEquals(MunicipalityResolutionStatus.PROVINCE_FALLBACK, resolution.status());
    }
}
