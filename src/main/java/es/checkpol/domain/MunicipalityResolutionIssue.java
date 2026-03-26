package es.checkpol.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "municipality_resolution_issues")
public class MunicipalityResolutionIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;

    @Column(name = "country_code", nullable = false, length = 3)
    private String countryCode;

    @Column(name = "postal_code", nullable = false, length = 12)
    private String postalCode;

    @Column(name = "postal_code_prefix", length = 2)
    private String postalCodePrefix;

    @Column(name = "municipality_query_normalized", nullable = false, length = 120)
    private String municipalityQueryNormalized;

    @Column(name = "municipality_query_label", nullable = false, length = 120)
    private String municipalityQueryLabel;

    @Column(name = "assigned_municipality_code", nullable = false, length = 5)
    private String assignedMunicipalityCode;

    @Column(name = "assigned_municipality_name", length = 80)
    private String assignedMunicipalityName;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_status", nullable = false, length = 30)
    private MunicipalityResolutionStatus resolutionStatus;

    @Column(name = "resolution_note", length = 255)
    private String resolutionNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_status", nullable = false, length = 20)
    private MunicipalityIssueStatus issueStatus;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    protected MunicipalityResolutionIssue() {
    }

    public MunicipalityResolutionIssue(
        Guest guest,
        String countryCode,
        String postalCode,
        String postalCodePrefix,
        String municipalityQueryNormalized,
        String municipalityQueryLabel,
        String assignedMunicipalityCode,
        String assignedMunicipalityName,
        MunicipalityResolutionStatus resolutionStatus,
        String resolutionNote,
        MunicipalityIssueStatus issueStatus,
        OffsetDateTime createdAt,
        OffsetDateTime resolvedAt
    ) {
        this.guest = guest;
        this.countryCode = countryCode;
        this.postalCode = postalCode;
        this.postalCodePrefix = postalCodePrefix;
        this.municipalityQueryNormalized = municipalityQueryNormalized;
        this.municipalityQueryLabel = municipalityQueryLabel;
        this.assignedMunicipalityCode = assignedMunicipalityCode;
        this.assignedMunicipalityName = assignedMunicipalityName;
        this.resolutionStatus = resolutionStatus;
        this.resolutionNote = resolutionNote;
        this.issueStatus = issueStatus;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
    }

    public Long getId() {
        return id;
    }

    public Guest getGuest() {
        return guest;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getPostalCode() {
        return postalCode;
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

    public String getAssignedMunicipalityCode() {
        return assignedMunicipalityCode;
    }

    public String getAssignedMunicipalityName() {
        return assignedMunicipalityName;
    }

    public MunicipalityResolutionStatus getResolutionStatus() {
        return resolutionStatus;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public MunicipalityIssueStatus getIssueStatus() {
        return issueStatus;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void refreshAutomaticAssignment(
        String postalCode,
        String postalCodePrefix,
        String municipalityQueryNormalized,
        String municipalityQueryLabel,
        String assignedMunicipalityCode,
        String assignedMunicipalityName,
        MunicipalityResolutionStatus resolutionStatus,
        String resolutionNote
    ) {
        this.postalCode = postalCode;
        this.postalCodePrefix = postalCodePrefix;
        this.municipalityQueryNormalized = municipalityQueryNormalized;
        this.municipalityQueryLabel = municipalityQueryLabel;
        this.assignedMunicipalityCode = assignedMunicipalityCode;
        this.assignedMunicipalityName = assignedMunicipalityName;
        this.resolutionStatus = resolutionStatus;
        this.resolutionNote = resolutionNote;
        this.issueStatus = MunicipalityIssueStatus.OPEN;
        this.resolvedAt = null;
    }

    public void markResolved(String assignedMunicipalityCode, String assignedMunicipalityName, String resolutionNote, OffsetDateTime resolvedAt) {
        this.assignedMunicipalityCode = assignedMunicipalityCode;
        this.assignedMunicipalityName = assignedMunicipalityName;
        this.resolutionStatus = MunicipalityResolutionStatus.MANUAL_OVERRIDE;
        this.resolutionNote = resolutionNote;
        this.issueStatus = MunicipalityIssueStatus.RESOLVED;
        this.resolvedAt = resolvedAt;
    }

    public void closeSilently(OffsetDateTime resolvedAt) {
        this.issueStatus = MunicipalityIssueStatus.RESOLVED;
        this.resolvedAt = resolvedAt;
    }
}
