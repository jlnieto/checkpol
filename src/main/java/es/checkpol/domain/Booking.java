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
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    @Column(name = "reference_code", nullable = false, length = 80)
    private String referenceCode;

    @Column(name = "person_count", nullable = false)
    private Integer personCount;

    @Column(name = "contract_date", nullable = false)
    private LocalDate contractDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingChannel channel;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 5)
    private PaymentType paymentType;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "payment_holder", length = 100)
    private String paymentHolder;

    @Column(name = "card_expiry", length = 7)
    private String cardExpiry;

    @Column(name = "self_service_token", unique = true, length = 64)
    private String selfServiceToken;

    @Column(name = "self_service_expires_at")
    private OffsetDateTime selfServiceExpiresAt;

    protected Booking() {
    }

    public Booking(
        Accommodation accommodation,
        String referenceCode,
        Integer personCount,
        LocalDate contractDate,
        BookingChannel channel,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        PaymentType paymentType,
        LocalDate paymentDate,
        String paymentMethod,
        String paymentHolder,
        String cardExpiry
    ) {
        this.accommodation = accommodation;
        this.referenceCode = referenceCode;
        this.personCount = personCount;
        this.contractDate = contractDate;
        this.channel = channel;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.paymentType = paymentType;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.paymentHolder = paymentHolder;
        this.cardExpiry = cardExpiry;
    }

    public Long getId() {
        return id;
    }

    public Accommodation getAccommodation() {
        return accommodation;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public Integer getPersonCount() {
        return personCount;
    }

    public LocalDate getContractDate() {
        return contractDate;
    }

    public BookingChannel getChannel() {
        return channel;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentHolder() {
        return paymentHolder;
    }

    public String getCardExpiry() {
        return cardExpiry;
    }

    public String getSelfServiceToken() {
        return selfServiceToken;
    }

    public OffsetDateTime getSelfServiceExpiresAt() {
        return selfServiceExpiresAt;
    }

    public void update(
        Accommodation accommodation,
        String referenceCode,
        Integer personCount,
        LocalDate contractDate,
        BookingChannel channel,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        PaymentType paymentType,
        LocalDate paymentDate,
        String paymentMethod,
        String paymentHolder,
        String cardExpiry
    ) {
        this.accommodation = accommodation;
        this.referenceCode = referenceCode;
        this.personCount = personCount;
        this.contractDate = contractDate;
        this.channel = channel;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.paymentType = paymentType;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.paymentHolder = paymentHolder;
        this.cardExpiry = cardExpiry;
    }

    public void updateSelfServiceAccess(String token, OffsetDateTime expiresAt) {
        this.selfServiceToken = token;
        this.selfServiceExpiresAt = expiresAt;
    }
}
