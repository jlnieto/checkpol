package es.checkpol.service.billing;

public record EmbeddedCheckoutSession(
    String customerId,
    String checkoutSessionId,
    String clientSecret
) {
}
