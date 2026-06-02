package es.checkpol.service.billing;

import java.time.OffsetDateTime;
import java.util.Map;

public record StripeSubscriptionSnapshot(
    String id,
    String customerId,
    String subscriptionItemId,
    String status,
    int quantity,
    OffsetDateTime currentPeriodStart,
    OffsetDateTime currentPeriodEnd,
    boolean cancelAtPeriodEnd,
    Map<String, String> metadata
) {
}
