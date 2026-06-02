package es.checkpol.web.billing;

import es.checkpol.service.billing.BillingConfigurationException;
import es.checkpol.service.billing.BillingProviderException;
import es.checkpol.service.billing.StripeWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StripeWebhookController {

    private final StripeWebhookService stripeWebhookService;

    public StripeWebhookController(StripeWebhookService stripeWebhookService) {
        this.stripeWebhookService = stripeWebhookService;
    }

    @PostMapping("/webhooks/stripe")
    public ResponseEntity<Void> stripeWebhook(
        @RequestBody String payload,
        @RequestHeader(name = "Stripe-Signature", required = false) String signatureHeader
    ) {
        stripeWebhookService.process(payload, signatureHeader);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(BillingProviderException.class)
    public ResponseEntity<Void> invalidStripeWebhook() {
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(BillingConfigurationException.class)
    public ResponseEntity<Void> billingNotConfigured() {
        return ResponseEntity.internalServerError().build();
    }
}
