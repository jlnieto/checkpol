package es.checkpol.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "municipality_resolution_rules")
public class MunicipalityResolutionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country_code", nullable = false, length = 3)
    private String countryCode;

    @Column(name = "postal_code_prefix", length = 2)
    private String postalCodePrefix;

    @Column(name = "municipality_query_normalized", nullable = false, length = 120)
    private String municipalityQueryNormalized;

    @Column(name = "municipality_query_label", nullable = false, length = 120)
    private String municipalityQueryLabel;

    @Column(name = "municipality_code", nullable = false, length = 5)
    private String municipalityCode;

    @Column(name = "municipality_name", nullable = false, length = 80)
    private String municipalityName;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected MunicipalityResolutionRule() {
    }

    public MunicipalityResolutionRule(
        String countryCode,
        String postalCodePrefix,
        String municipalityQueryNormalized,
        String municipalityQueryLabel,
        String municipalityCode,
        String municipalityName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
    ) {
        this.countryCode = countryCode;
        this.postalCodePrefix = postalCodePrefix;
        this.municipalityQueryNormalized = municipalityQueryNormalized;
        this.municipalityQueryLabel = municipalityQueryLabel;
        this.municipalityCode = municipalityCode;
        this.municipalityName = municipalityName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getPostalCodePrefix() {
        return postalCodePrefix;
    }

    public String getMunicipalityQueryNormalized() {
        return municipalityQueryNormalized;
    }

    public String getMunicipalityQueryLabel() {
        return municipalityQueryLabel;
    }

    public String getMunicipalityCode() {
        return municipalityCode;
    }

    public String getMunicipalityName() {
        return municipalityName;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateResolution(String municipalityCode, String municipalityName, OffsetDateTime updatedAt) {
        this.municipalityCode = municipalityCode;
        this.municipalityName = municipalityName;
        this.updatedAt = updatedAt;
    }
}
