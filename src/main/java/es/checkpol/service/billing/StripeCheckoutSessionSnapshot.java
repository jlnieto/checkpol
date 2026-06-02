package es.checkpol.service.billing;

import es.checkpol.domain.billing.BillingCustomerType;
import es.checkpol.domain.billing.BillingTaxMode;

import java.util.Map;

public record StripeCheckoutSessionSnapshot(
    String id,
    String customerId,
    String subscriptionId,
    String paymentStatus,
    Map<String, String> metadata,
    String customerCountry,
    BillingCustomerType customerType,
    BillingTaxMode taxMode
) {
}
