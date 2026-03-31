package es.checkpol.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "checkpol.municipality.catalog.import-on-startup", havingValue = "true")
public class MunicipalityCatalogImportRunner implements CommandLineRunner {

    private final MunicipalityCatalogImportService municipalityCatalogImportService;
    private final Resource municipalitiesResource;
    private final Resource postalMappingsResource;
    private final String source;
    private final String sourceVersion;

    public MunicipalityCatalogImportRunner(
        MunicipalityCatalogImportService municipalityCatalogImportService,
        @Value("${checkpol.municipality.catalog.municipalities-resource}") Resource municipalitiesResource,
        @Value("${checkpol.municipality.catalog.postal-mappings-resource}") Resource postalMappingsResource,
        @Value("${checkpol.municipality.catalog.source:" + MunicipalityCatalogImportService.DEFAULT_SOURCE + "}") String source,
        @Value("${checkpol.municipality.catalog.source-version:" + MunicipalityCatalogImportService.DEFAULT_SOURCE_VERSION + "}") String sourceVersion
    ) {
        this.municipalityCatalogImportService = municipalityCatalogImportService;
        this.municipalitiesResource = municipalitiesResource;
        this.postalMappingsResource = postalMappingsResource;
        this.source = source;
        this.sourceVersion = sourceVersion;
    }

    @Override
    public void run(String... args) {
        municipalityCatalogImportService.importFromResources(
            municipalitiesResource,
            postalMappingsResource,
            source,
            sourceVersion
        );
    }
}
