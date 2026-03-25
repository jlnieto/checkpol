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

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "guests")
public class Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName1;

    @Column(length = 50)
    private String lastName2;

    @Enumerated(EnumType.STRING)
    @Column(length = 5)
    private DocumentType documentType;

    @Column(length = 15)
    private String documentNumber;

    @Column(length = 9)
    private String documentSupport;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(length = 3)
    private String nationality;

    @Enumerated(EnumType.STRING)
    @Column(length = 1)
    private GuestSex sex;

    @Column(name = "address_line", nullable = false, length = 120)
    private String addressLine;

    @Column(name = "address_complement", length = 120)
    private String addressComplement;

    @Column(name = "municipality_code", length = 5)
    private String municipalityCode;

    @Column(name = "municipality_name", length = 80)
    private String municipalityName;

    @Column(name = "postal_code", nullable = false, length = 12)
    private String postalCode;

    @Column(nullable = false, length = 3)
    private String country;

    @Column(length = 20)
    private String phone;

    @Column(length = 20)
    private String phone2;

    @Column(length = 250)
    private String email;

    @Column(length = 5)
    private String relationship;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_source", nullable = false, length = 20)
    private GuestSubmissionSource submissionSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    private GuestReviewStatus reviewStatus;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    protected Guest() {
    }

    public Guest(
        Booking booking,
        String firstName,
        String lastName1,
        String lastName2,
        DocumentType documentType,
        String documentNumber,
        String documentSupport,
        LocalDate birthDate,
        String nationality,
        GuestSex sex,
        String addressLine,
        String addressComplement,
        String municipalityCode,
        String municipalityName,
        String postalCode,
        String country,
        String phone,
        String phone2,
        String email,
        String relationship,
        GuestSubmissionSource submissionSource,
        GuestReviewStatus reviewStatus,
        OffsetDateTime submittedAt
    ) {
        this.booking = booking;
        this.firstName = firstName;
        this.lastName1 = lastName1;
        this.lastName2 = lastName2;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.documentSupport = documentSupport;
        this.birthDate = birthDate;
        this.nationality = nationality;
        this.sex = sex;
        this.addressLine = addressLine;
        this.addressComplement = addressComplement;
        this.municipalityCode = municipalityCode;
        this.municipalityName = municipalityName;
        this.postalCode = postalCode;
        this.country = country;
        this.phone = phone;
        this.phone2 = phone2;
        this.email = email;
        this.relationship = relationship;
        this.submissionSource = submissionSource;
        this.reviewStatus = reviewStatus;
        this.submittedAt = submittedAt;
    }

    public Long getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName1() {
        return lastName1;
    }

    public String getLastName2() {
        return lastName2;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public String getDocumentSupport() {
        return documentSupport;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getNationality() {
        return nationality;
    }

    public GuestSex getSex() {
        return sex;
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

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }

    public String getPhone() {
        return phone;
    }

    public String getPhone2() {
        return phone2;
    }

    public String getEmail() {
        return email;
    }

    public String getRelationship() {
        return relationship;
    }

    public GuestSubmissionSource getSubmissionSource() {
        return submissionSource;
    }

    public GuestReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public String getDisplayName() {
        return lastName2 == null || lastName2.isBlank()
            ? firstName + " " + lastName1
            : firstName + " " + lastName1 + " " + lastName2;
    }

    public boolean hasMinimumDataForTravelerPart() {
        return hasText(firstName)
            && hasText(lastName1)
            && birthDate != null
            && hasText(addressLine)
            && hasText(postalCode)
            && hasText(country)
            && (hasText(municipalityCode) || hasText(municipalityName))
            && hasAnyContact();
    }

    private boolean hasAnyContact() {
        return hasText(phone) || hasText(phone2) || hasText(email);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public void update(
        String firstName,
        String lastName1,
        String lastName2,
        DocumentType documentType,
        String documentNumber,
        String documentSupport,
        LocalDate birthDate,
        String nationality,
        GuestSex sex,
        String addressLine,
        String addressComplement,
        String municipalityCode,
        String municipalityName,
        String postalCode,
        String country,
        String phone,
        String phone2,
        String email,
        String relationship
    ) {
        this.firstName = firstName;
        this.lastName1 = lastName1;
        this.lastName2 = lastName2;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.documentSupport = documentSupport;
        this.birthDate = birthDate;
        this.nationality = nationality;
        this.sex = sex;
        this.addressLine = addressLine;
        this.addressComplement = addressComplement;
        this.municipalityCode = municipalityCode;
        this.municipalityName = municipalityName;
        this.postalCode = postalCode;
        this.country = country;
        this.phone = phone;
        this.phone2 = phone2;
        this.email = email;
        this.relationship = relationship;
    }

    public void markReviewed() {
        this.reviewStatus = GuestReviewStatus.REVIEWED;
    }
}
