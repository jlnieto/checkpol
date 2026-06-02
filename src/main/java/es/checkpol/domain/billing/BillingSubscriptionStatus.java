package es.checkpol.domain.billing;

import java.time.OffsetDateTime;

public enum BillingSubscriptionStatus {
    PENDING,
    ACTIVE,
    TRIALING,
    PAST_DUE,
    UNPAID,
    CANCELED,
    INCOMPLETE,
    INCOMPLETE_EXPIRED,
    PAUSED;

    public static BillingSubscriptionStatus fromStripeStatus(String stripeStatus) {
        if (stripeStatus == null || stripeStatus.isBlank()) {
            return PENDING;
        }
        return switch (stripeStatus.trim().toLowerCase()) {
            case "active" -> ACTIVE;
            case "trialing" -> TRIALING;
            case "past_due" -> PAST_DUE;
            case "unpaid" -> UNPAID;
            case "canceled" -> CANCELED;
            case "incomplete" -> INCOMPLETE;
            case "incomplete_expired" -> INCOMPLETE_EXPIRED;
            case "paused" -> PAUSED;
            default -> PENDING;
        };
    }

    public boolean isUsable(OffsetDateTime currentPeriodEnd, int gracePeriodDays, OffsetDateTime now) {
        if (this == ACTIVE || this == TRIALING) {
            return true;
        }
        if (this == PAST_DUE && currentPeriodEnd != null) {
            return currentPeriodEnd.plusDays(gracePeriodDays).isAfter(now);
        }
        return false;
    }
}
