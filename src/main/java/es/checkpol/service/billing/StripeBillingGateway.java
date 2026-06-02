package es.checkpol.service.billing;

import es.checkpol.domain.billing.PendingSignup;

public interface StripeBillingGateway {

    EmbeddedCheckoutSession createSubscriptionCheckout(PendingSignup signup);

    StripeSubscriptionSnapshot retrieveSubscription(String subscriptionId);

    String createCustomerPortalSession(String stripeCustomerId, String returnUrl);

    StripeWebhookEvent constructWebhookEvent(String payload, String signatureHeader);
}
