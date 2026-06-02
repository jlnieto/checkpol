package es.checkpol.service.billing;

import es.checkpol.domain.billing.PendingSignup;

public record RegistrationCheckout(
    PendingSignup signup,
    String publishableKey
) {
}
