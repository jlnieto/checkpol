package es.checkpol.service.billing;

public record StripeWebhookEvent(
    String id,
    String type,
    StripeCheckoutSessionSnapshot checkoutSession,
    StripeSubscriptionSnapshot subscription,
    StripeInvoiceSnapshot invoice
) {
}
