package es.checkpol.service.billing;

import java.time.OffsetDateTime;

public record StripeInvoiceSnapshot(
    String id,
    String customerId,
    String subscriptionId,
    String number,
    String status,
    Long totalAmount,
    String currency,
    Long taxAmount,
    String taxCountry,
    String taxBehavior,
    String hostedInvoiceUrl,
    String invoicePdfUrl,
    OffsetDateTime periodStart,
    OffsetDateTime periodEnd
) {
}
