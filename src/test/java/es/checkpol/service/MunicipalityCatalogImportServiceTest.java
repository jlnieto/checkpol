package es.checkpol.service;

import es.checkpol.repository.MunicipalityCatalogEntryRepository;
import es.checkpol.repository.PostalCodeMunicipalityMappingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "checkpol.municipality.catalog.import-on-startup=false")
@ActiveProfiles("test")
@Transactional
class MunicipalityCatalogImportServiceTest {

    @Autowired
    private MunicipalityCatalogImportService municipalityCatalogImportService;

    @Autowired
    private MunicipalityCatalogEntryRepository municipalityCatalogEntryRepository;

    @Autowired
    private PostalCodeMunicipalityMappingRepository postalCodeMunicipalityMappingRepository;

    @Test
    void importsMunicipalitiesAndPostalMappingsIdempotently() {
        MunicipalityCatalogImportService.ImportSummary firstImport = municipalityCatalogImportService.importFromResources(
            csv("""
                provinceCode;provinceName;municipalityCode;municipalityName
                28;Madrid;28079;Madrid
                35;Las Palmas;35014;La Oliva
                """),
            csv("""
                postalCode;municipalityCode
                28001;28079
                35540;35014
                """),
            "test-import",
            "v1"
        );

        assertEquals(2, firstImport.importedMunicipalities());
        assertEquals(2, firstImport.importedPostalMappings());
        assertEquals(2, municipalityCatalogEntryRepository.findAll().stream().filter(entry -> entry.isActive()).count());
        assertEquals(2, postalCodeMunicipalityMappingRepository.findAll().stream().filter(mapping -> mapping.isActive()).count());

        MunicipalityCatalogImportService.ImportSummary secondImport = municipalityCatalogImportService.importFromResources(
            csv("""
                provinceCode;provinceName;municipalityCode;municipalityName
                28;Madrid;28079;Madrid
                """),
            csv("""
                postalCode;municipalityCode
                28001;28079
                """),
            "test-import",
            "v2"
        );

        assertEquals(1, secondImport.deactivatedMunicipalities());
        assertEquals(1, secondImport.deactivatedPostalMappings());
        assertEquals(1, municipalityCatalogEntryRepository.findAll().stream().filter(entry -> entry.isActive()).count());
        assertEquals(1, postalCodeMunicipalityMappingRepository.findAll().stream().filter(mapping -> mapping.isActive()).count());
    }

    @Test
    void rejectsDuplicatedMunicipalityCodesInsideSameCsv() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> municipalityCatalogImportService.importFromResources(
                csv("""
                    provinceCode;provinceName;municipalityCode;municipalityName
                    28;Madrid;28079;Madrid
                    28;Madrid;28079;Madrid Centro
                    """),
                csv("""
                    postalCode;municipalityCode
                    28001;28079
                    """),
                "test-import",
                "v1"
            )
        );

        assertEquals("El CSV de municipios contiene un código repetido: 28079", exception.getMessage());
    }

    @Test
    void rejectsInvalidPostalMappingRows() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> municipalityCatalogImportService.importFromResources(
                csv("""
                    provinceCode;provinceName;municipalityCode;municipalityName
                    28;Madrid;28079;Madrid
                    """),
                csv("""
                    postalCode;municipalityCode
                    28A01;28079
                    """),
                "test-import",
                "v1"
            )
        );

        assertEquals("postalCode debe tener 5 dígitos: 28A01", exception.getMessage());
    }

    private ByteArrayResource csv(String value) {
        return new ByteArrayResource(value.getBytes(StandardCharsets.UTF_8));
    }
}
