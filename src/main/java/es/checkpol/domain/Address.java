package es.checkpol.domain;

import es.checkpol.service.MunicipalityResolution;
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

@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "address_line", nullable = false, length = 120)
    private String addressLine;

    @Column(name = "address_complement", length = 120)
    private String addressComplement;

    @Column(name = "municipality_code", length = 5)
    private String municipalityCode;

    @Column(name = "municipality_name", length = 80)
    private String municipalityName;

    @Column(name = "municipality_resolved_name", length = 80)
    private String municipalityResolvedName;

    @Enumerated(EnumType.STRING)
    @Column(name = "municipality_resolution_status", nullable = false, length = 30)
    private MunicipalityResolutionStatus municipalityResolutionStatus;

    @Column(name = "municipality_resolution_note", length = 255)
    private String municipalityResolutionNote;

    @Column(name = "postal_code", nullable = false, length = 12)
    private String postalCode;

    @Column(nullable = false, length = 3)
    private String country;

    protected Address() {
    }

    public Address(
        Booking booking,
        String addressLine,
        String addressComplement,
        String municipalityCode,
        String municipalityName,
        String municipalityResolvedName,
        MunicipalityResolutionStatus municipalityResolutionStatus,
        String municipalityResolutionNote,
        String postalCode,
        String country
    ) {
        this.booking = booking;
        this.addressLine = addressLine;
        this.addressComplement = addressComplement;
        this.municipalityCode = municipalityCode;
        this.municipalityName = municipalityName;
        this.municipalityResolvedName = municipalityResolvedName;
        this.municipalityResolutionStatus = municipalityResolutionStatus;
        this.municipalityResolutionNote = municipalityResolutionNote;
        this.postalCode = postalCode;
        this.country = country;
    }

    public Long getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public String getAddressComplement() {
        return addressComplement;
    }

    public String getMunicipalityCode() {
        return municipalityCode;
    }

    public String getMunicipalityName() {
        return municipalityName;
    }

    public String getMunicipalityResolvedName() {
        return municipalityResolvedName;
    }

    public MunicipalityResolutionStatus getMunicipalityResolutionStatus() {
        return municipalityResolutionStatus;
    }

    public String getMunicipalityResolutionNote() {
        return municipalityResolutionNote;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }

    public String getDisplayLine1() {
        if (addressComplement == null || addressComplement.isBlank()) {
            return addressLine;
        }
        return addressLine + ", " + addressComplement;
    }

    public String getDisplayLine2() {
        String municipality = municipalityName != null && !municipalityName.isBlank()
            ? municipalityName
            : municipalityResolvedName;
        return postalCode + " · " + municipality + " · " + country;
    }

    public MunicipalityResolution toMunicipalityResolution() {
        return new MunicipalityResolution(
            municipalityCode,
            municipalityResolvedName,
            municipalityResolutionStatus,
            municipalityResolutionNote,
            null,
            municipalityName,
            postalCode == null || postalCode.length() < 2 ? null : postalCode.substring(0, 2)
        );
    }

    public void applyMunicipalityResolution(
        String municipalityCode,
        String municipalityResolvedName,
        MunicipalityResolutionStatus municipalityResolutionStatus,
        String municipalityResolutionNote
    ) {
        this.municipalityCode = municipalityCode;
        if (this.municipalityName == null || this.municipalityName.isBlank()) {
            this.municipalityName = municipalityResolvedName;
        }
        this.municipalityResolvedName = municipalityResolvedName;
        this.municipalityResolutionStatus = municipalityResolutionStatus;
        this.municipalityResolutionNote = municipalityResolutionNote;
    }
}
