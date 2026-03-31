package es.checkpol.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "municipality_import_records")
public class MunicipalityImportRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String source;

    @Column(name = "source_version", nullable = false, length = 80)
    private String sourceVersion;

    @Column(name = "municipalities_url", nullable = false, length = 500)
    private String municipalitiesUrl;

    @Column(name = "postal_mappings_url", nullable = false, length = 500)
    private String postalMappingsUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MunicipalityImportStatus status;

    @Column(name = "imported_municipalities", nullable = false)
    private int importedMunicipalities;

    @Column(name = "deactivated_municipalities", nullable = false)
    private int deactivatedMunicipalities;

    @Column(name = "imported_postal_mappings", nullable = false)
    private int importedPostalMappings;

    @Column(name = "deactivated_postal_mappings", nullable = false)
    private int deactivatedPostalMappings;

    @Column(name = "triggered_by_username", nullable = false, length = 80)
    private String triggeredByUsername;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected MunicipalityImportRecord() {
    }

    public MunicipalityImportRecord(
        String source,
        String sourceVersion,
        String municipalitiesUrl,
        String postalMappingsUrl,
        MunicipalityImportStatus status,
        int importedMunicipalities,
        int deactivatedMunicipalities,
        int importedPostalMappings,
        int deactivatedPostalMappings,
        String triggeredByUsername,
        String errorMessage,
        OffsetDateTime createdAt
    ) {
        this.source = source;
        this.sourceVersion = sourceVersion;
        this.municipalitiesUrl = municipalitiesUrl;
        this.postalMappingsUrl = postalMappingsUrl;
        this.status = status;
        this.importedMunicipalities = importedMunicipalities;
        this.deactivatedMunicipalities = deactivatedMunicipalities;
        this.importedPostalMappings = importedPostalMappings;
        this.deactivatedPostalMappings = deactivatedPostalMappings;
        this.triggeredByUsername = triggeredByUsername;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    public String getMunicipalitiesUrl() {
        return municipalitiesUrl;
    }

    public String getPostalMappingsUrl() {
        return postalMappingsUrl;
    }

    public MunicipalityImportStatus getStatus() {
        return status;
    }

    public int getImportedMunicipalities() {
        return importedMunicipalities;
    }

    public int getDeactivatedMunicipalities() {
        return deactivatedMunicipalities;
    }

    public int getImportedPostalMappings() {
        return importedPostalMappings;
    }

    public int getDeactivatedPostalMappings() {
        return deactivatedPostalMappings;
    }

    public String getTriggeredByUsername() {
        return triggeredByUsername;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
