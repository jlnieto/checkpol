package es.checkpol.repository;

import es.checkpol.domain.MunicipalityCatalogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MunicipalityCatalogEntryRepository extends JpaRepository<MunicipalityCatalogEntry, Long> {

    boolean existsByCountryCodeAndActiveTrue(String countryCode);

    long countByCountryCodeAndActiveTrue(String countryCode);

    Optional<MunicipalityCatalogEntry> findByCountryCodeAndMunicipalityCodeAndActiveTrue(String countryCode, String municipalityCode);

    Optional<MunicipalityCatalogEntry> findTopByCountryCodeAndActiveTrueOrderByUpdatedAtDesc(String countryCode);

    List<MunicipalityCatalogEntry> findAllByMunicipalityCodeInAndActiveTrueOrderByMunicipalityNameAsc(Collection<String> municipalityCodes);
}
