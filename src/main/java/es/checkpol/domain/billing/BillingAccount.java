package es.checkpol.domain.billing;

import es.checkpol.domain.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "billing_accounts")
public class BillingAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private AppUser owner;

    @Column(name = "stripe_customer_id", nullable = false, unique = true, length = 120)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", unique = true, length = 120)
    private String stripeSubscriptionId;

    @Column(name = "stripe_subscription_item_id", length = 120)
    private String stripeSubscriptionItemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BillingSubscriptionStatus status;

    @Column(name = "paid_accommodation_limit", nullable = false)
    private int paidAccommodationLimit;

    @Column(name = "current_period_start")
    private OffsetDateTime currentPeriodStart;

    @Column(name = "current_period_end")
    private OffsetDateTime currentPeriodEnd;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd;

    @Column(name = "customer_country", length = 2)
    private String customerCountry;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false, length = 30)
    private BillingCustomerType customerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_mode", nullable = false, length = 30)
    private BillingTaxMode taxMode;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected BillingAccount() {
    }

    public BillingAccount(
        AppUser owner,
        String stripeCustomerId,
        BillingSubscriptionStatus status,
        int paidAccommodationLimit,
        OffsetDateTime createdAt
    ) {
        this.owner = owner;
        this.stripeCustomerId = stripeCustomerId;
        this.status = status;
        this.paidAccommodationLimit = paidAccommodationLimit;
        this.customerType = BillingCustomerType.UNKNOWN;
        this.taxMode = BillingTaxMode.UNKNOWN;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public AppUser getOwner() {
        return owner;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public String getStripeSubscriptionId() {
        return stripeSubscriptionId;
    }

    public String getStripeSubscriptionItemId() {
        return stripeSubscriptionItemId;
    }

    public BillingSubscriptionStatus getStatus() {
        return status;
    }

    public int getPaidAccommodationLimit() {
        return paidAccommodationLimit;
    }

    public OffsetDateTime getCurrentPeriodStart() {
        return currentPeriodStart;
    }

    public OffsetDateTime getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public boolean isCancelAtPeriodEnd() {
        return cancelAtPeriodEnd;
    }

    public String getCustomerCountry() {
        return customerCountry;
    }

    public BillingCustomerType getCustomerType() {
        return customerType;
    }

    public BillingTaxMode getTaxMode() {
        return taxMode;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void syncSubscription(
        String stripeSubscriptionId,
        String stripeSubscriptionItemId,
        BillingSubscriptionStatus status,
        int paidAccommodationLimit,
        OffsetDateTime currentPeriodStart,
        OffsetDateTime currentPeriodEnd,
        boolean cancelAtPeriodEnd
    ) {
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.stripeSubscriptionItemId = stripeSubscriptionItemId;
        this.status = status;
        this.paidAccommodationLimit = paidAccommodationLimit;
        this.currentPeriodStart = currentPeriodStart;
        this.currentPeriodEnd = currentPeriodEnd;
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
        touch();
    }

    public void updateCustomerTaxProfile(String customerCountry, BillingCustomerType customerType, BillingTaxMode taxMode) {
        this.customerCountry = customerCountry;
        this.customerType = customerType == null ? BillingCustomerType.UNKNOWN : customerType;
        this.taxMode = taxMode == null ? BillingTaxMode.UNKNOWN : taxMode;
        touch();
    }

    public boolean allowsAccommodationCreation(int currentAccommodationCount, int gracePeriodDays, OffsetDateTime now) {
        return status.isUsable(currentPeriodEnd, gracePeriodDays, now)
            && currentAccommodationCount < paidAccommodationLimit;
    }

    private void touch() {
        this.updatedAt = OffsetDateTime.now();
    }
}
