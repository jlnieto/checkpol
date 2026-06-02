package es.checkpol.service.billing;

import es.checkpol.domain.billing.StripeEventLog;
import es.checkpol.domain.billing.StripeEventProcessingStatus;
import es.checkpol.repository.billing.StripeEventLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class StripeWebhookService {

    private final StripeBillingGateway stripeBillingGateway;
    private final StripeEventLogRepository stripeEventLogRepository;
    private final RegistrationBillingService registrationBillingService;
    private final BillingAccountService billingAccountService;

    public StripeWebhookService(
        StripeBillingGateway stripeBillingGateway,
        StripeEventLogRepository stripeEventLogRepository,
        RegistrationBillingService registrationBillingService,
        BillingAccountService billingAccountService
    ) {
        this.stripeBillingGateway = stripeBillingGateway;
        this.stripeEventLogRepository = stripeEventLogRepository;
        this.registrationBillingService = registrationBillingService;
        this.billingAccountService = billingAccountService;
    }

    @Transactional(noRollbackFor = StripeWebhookProcessingException.class)
    public void process(String payload, String signatureHeader) {
        StripeWebhookEvent event = stripeBillingGateway.constructWebhookEvent(payload, signatureHeader);
        StripeEventLog eventLog = stripeEventLogRepository.findByStripeEventId(event.id())
            .orElseGet(() -> stripeEventLogRepository.save(new StripeEventLog(
                event.id(),
                event.type(),
                payload,
                OffsetDateTime.now()
            )));
        if (eventLog.getProcessingStatus() == StripeEventProcessingStatus.PROCESSED) {
            return;
        }

        try {
            handle(event);
            eventLog.markProcessed(OffsetDateTime.now());
        } catch (RuntimeException ex) {
            eventLog.markFailed(ex.getMessage(), OffsetDateTime.now());
            throw new StripeWebhookProcessingException("No se ha podido procesar el webhook de Stripe.", ex);
        }
    }

    private void handle(StripeWebhookEvent event) {
        switch (event.type()) {
            case "checkout.session.completed" -> {
                if (event.checkoutSession() == null) {
                    throw new IllegalStateException("Stripe no ha enviado la sesión de Checkout.");
                }
                registrationBillingService.completeFromCheckout(event.checkoutSession());
            }
            case "customer.subscription.created", "customer.subscription.updated", "customer.subscription.deleted" -> {
                if (event.subscription() == null) {
                    throw new IllegalStateException("Stripe no ha enviado la suscripción.");
                }
                billingAccountService.syncSubscription(event.subscription());
            }
            case "invoice.created", "invoice.finalized", "invoice.paid", "invoice.payment_failed" -> {
                if (event.invoice() == null) {
                    throw new IllegalStateException("Stripe no ha enviado la factura.");
                }
                billingAccountService.syncInvoice(event.invoice());
            }
            default -> {
            }
        }
    }
}
