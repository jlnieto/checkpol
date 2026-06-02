package es.checkpol.service.billing;

import es.checkpol.domain.billing.BillingSubscriptionStatus;

import java.time.OffsetDateTime;

public record BillingStatusSummary(
    boolean billingManaged,
    BillingSubscriptionStatus status,
    int paidAccommodationLimit,
    int currentAccommodationCount,
    OffsetDateTime currentPeriodEnd,
    boolean cancelAtPeriodEnd,
    String customerCountry,
    String customerTypeLabel,
    String taxModeLabel
) {

    public boolean canCreateAccommodation() {
        return !billingManaged || currentAccommodationCount < paidAccommodationLimit;
    }

    public int remainingAccommodationSlots() {
        if (!billingManaged) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, paidAccommodationLimit - currentAccommodationCount);
    }
}
