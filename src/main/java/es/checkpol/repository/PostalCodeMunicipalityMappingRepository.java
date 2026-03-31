package es.checkpol.repository;

import es.checkpol.domain.PostalCodeMunicipalityMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostalCodeMunicipalityMappingRepository extends JpaRepository<PostalCodeMunicipalityMapping, Long> {

    long countByActiveTrue();

    List<PostalCodeMunicipalityMapping> findAllByPostalCodeAndActiveTrueOrderByMunicipalityCodeAsc(String postalCode);

    Optional<PostalCodeMunicipalityMapping> findByPostalCodeAndMunicipalityCodeAndActiveTrue(String postalCode, String municipalityCode);
}
