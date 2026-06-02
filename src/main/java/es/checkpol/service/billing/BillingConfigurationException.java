package es.checkpol.service.billing;

public class BillingConfigurationException extends RuntimeException {

    public BillingConfigurationException(String message) {
        super(message);
    }

    public BillingConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
