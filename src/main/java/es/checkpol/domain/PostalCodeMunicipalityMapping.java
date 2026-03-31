package es.checkpol.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "postal_code_municipality_mappings")
public class PostalCodeMunicipalityMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "postal_code", nullable = false, length = 12)
    private String postalCode;

    @Column(name = "municipality_code", nullable = false, length = 5)
    private String municipalityCode;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, length = 40)
    private String source;

    @Column(name = "source_version", nullable = false, length = 40)
    private String sourceVersion;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected PostalCodeMunicipalityMapping() {
    }

    public PostalCodeMunicipalityMapping(
        String postalCode,
        String municipalityCode,
        boolean active,
        String source,
        String sourceVersion,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
    ) {
        this.postalCode = postalCode;
        this.municipalityCode = municipalityCode;
        this.active = active;
        this.source = source;
        this.sourceVersion = sourceVersion;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getMunicipalityCode() {
        return municipalityCode;
    }

    public boolean isActive() {
        return active;
    }

    public String getSource() {
        return source;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void refreshFromImport(boolean active, String source, String sourceVersion, OffsetDateTime updatedAt) {
        this.active = active;
        this.source = source;
        this.sourceVersion = sourceVersion;
        this.updatedAt = updatedAt;
    }

    public void deactivate(String source, String sourceVersion, OffsetDateTime updatedAt) {
        this.active = false;
        this.source = source;
        this.sourceVersion = sourceVersion;
        this.updatedAt = updatedAt;
    }
}
