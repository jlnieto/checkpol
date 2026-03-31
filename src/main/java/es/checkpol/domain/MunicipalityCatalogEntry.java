package es.checkpol.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "municipality_catalog_entries")
public class MunicipalityCatalogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country_code", nullable = false, length = 3)
    private String countryCode;

    @Column(name = "province_code", nullable = false, length = 2)
    private String provinceCode;

    @Column(name = "province_name", nullable = false, length = 80)
    private String provinceName;

    @Column(name = "municipality_code", nullable = false, length = 5, unique = true)
    private String municipalityCode;

    @Column(name = "municipality_name", nullable = false, length = 80)
    private String municipalityName;

    @Column(name = "normalized_municipality_name", nullable = false, length = 120)
    private String normalizedMunicipalityName;

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

    protected MunicipalityCatalogEntry() {
    }

    public MunicipalityCatalogEntry(
        String countryCode,
        String provinceCode,
        String provinceName,
        String municipalityCode,
        String municipalityName,
        String normalizedMunicipalityName,
        boolean active,
        String source,
        String sourceVersion,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
    ) {
        this.countryCode = countryCode;
        this.provinceCode = provinceCode;
        this.provinceName = provinceName;
        this.municipalityCode = municipalityCode;
        this.municipalityName = municipalityName;
        this.normalizedMunicipalityName = normalizedMunicipalityName;
        this.active = active;
        this.source = source;
        this.sourceVersion = sourceVersion;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getProvinceCode() {
        return provinceCode;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public String getMunicipalityCode() {
        return municipalityCode;
    }

    public String getMunicipalityName() {
        return municipalityName;
    }

    public String getNormalizedMunicipalityName() {
        return normalizedMunicipalityName;
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

    public void refreshFromImport(
        String provinceCode,
        String provinceName,
        String municipalityName,
        String normalizedMunicipalityName,
        boolean active,
        String source,
        String sourceVersion,
        OffsetDateTime updatedAt
    ) {
        this.provinceCode = provinceCode;
        this.provinceName = provinceName;
        this.municipalityName = municipalityName;
        this.normalizedMunicipalityName = normalizedMunicipalityName;
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
