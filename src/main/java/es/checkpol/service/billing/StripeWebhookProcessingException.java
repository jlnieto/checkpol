package es.checkpol.service.billing;

public class StripeWebhookProcessingException extends RuntimeException {

    public StripeWebhookProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
