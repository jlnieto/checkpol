package es.checkpol.domain.billing;

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
@Table(name = "pending_signups")
public class PendingSignup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "accommodation_quantity", nullable = false)
    private int accommodationQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PendingSignupStatus status;

    @Column(nullable = false, unique = true, length = 36)
    private String token;

    @Column(name = "stripe_customer_id", length = 120)
    private String stripeCustomerId;

    @Column(name = "stripe_checkout_session_id", unique = true, length = 120)
    private String stripeCheckoutSessionId;

    @Column(name = "checkout_client_secret")
    private String checkoutClientSecret;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected PendingSignup() {
    }

    public PendingSignup(
        String email,
        String passwordHash,
        int accommodationQuantity,
        String token,
        OffsetDateTime expiresAt,
        OffsetDateTime createdAt
    ) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.accommodationQuantity = accommodationQuantity;
        this.status = PendingSignupStatus.PENDING_PAYMENT;
        this.token = token;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public int getAccommodationQuantity() {
        return accommodationQuantity;
    }

    public PendingSignupStatus getStatus() {
        return status;
    }

    public String getToken() {
        return token;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public String getStripeCheckoutSessionId() {
        return stripeCheckoutSessionId;
    }

    public String getCheckoutClientSecret() {
        return checkoutClientSecret;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isPendingPayment() {
        return status == PendingSignupStatus.PENDING_PAYMENT;
    }

    public boolean isExpired(OffsetDateTime now) {
        return expiresAt.isBefore(now);
    }

    public void storeCheckout(String stripeCustomerId, String stripeCheckoutSessionId, String checkoutClientSecret) {
        this.stripeCustomerId = stripeCustomerId;
        this.stripeCheckoutSessionId = stripeCheckoutSessionId;
        this.checkoutClientSecret = checkoutClientSecret;
        touch();
    }

    public void complete() {
        this.status = PendingSignupStatus.COMPLETED;
        touch();
    }

    public void expire() {
        this.status = PendingSignupStatus.EXPIRED;
        touch();
    }

    public void fail() {
        this.status = PendingSignupStatus.FAILED;
        touch();
    }

    private void touch() {
        this.updatedAt = OffsetDateTime.now();
    }
}
