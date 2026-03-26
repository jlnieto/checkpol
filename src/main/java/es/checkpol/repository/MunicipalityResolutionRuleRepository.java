package es.checkpol.repository;

import es.checkpol.domain.MunicipalityResolutionRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MunicipalityResolutionRuleRepository extends JpaRepository<MunicipalityResolutionRule, Long> {

    Optional<MunicipalityResolutionRule> findFirstByCountryCodeAndPostalCodePrefixAndMunicipalityQueryNormalized(
        String countryCode,
        String postalCodePrefix,
        String municipalityQueryNormalized
    );

    Optional<MunicipalityResolutionRule> findFirstByCountryCodeAndPostalCodePrefixIsNullAndMunicipalityQueryNormalized(
        String countryCode,
        String municipalityQueryNormalized
    );

    List<MunicipalityResolutionRule> findAllByOrderByUpdatedAtDesc();
}
