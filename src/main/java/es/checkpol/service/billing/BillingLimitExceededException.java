package es.checkpol.service.billing;

public class BillingLimitExceededException extends RuntimeException {

    public BillingLimitExceededException(String message) {
        super(message);
    }
}
