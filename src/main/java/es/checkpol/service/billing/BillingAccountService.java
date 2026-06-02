package es.checkpol.service.billing;

import es.checkpol.config.StripeBillingProperties;
import es.checkpol.domain.AppUser;
import es.checkpol.domain.billing.BillingAccount;
import es.checkpol.domain.billing.BillingCustomerType;
import es.checkpol.domain.billing.BillingInvoice;
import es.checkpol.domain.billing.BillingSubscriptionStatus;
import es.checkpol.domain.billing.BillingTaxMode;
import es.checkpol.repository.AccommodationRepository;
import es.checkpol.repository.billing.BillingAccountRepository;
import es.checkpol.repository.billing.BillingInvoiceRepository;
import es.checkpol.service.CurrentAppUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BillingAccountService {

    private final BillingAccountRepository billingAccountRepository;
    private final BillingInvoiceRepository billingInvoiceRepository;
    private final AccommodationRepository accommodationRepository;
    private final CurrentAppUserService currentAppUserService;
    private final StripeBillingGateway stripeBillingGateway;
    private final StripeBillingProperties properties;

    public BillingAccountService(
        BillingAccountRepository billingAccountRepository,
        BillingInvoiceRepository billingInvoiceRepository,
        AccommodationRepository accommodationRepository,
        CurrentAppUserService currentAppUserService,
        StripeBillingGateway stripeBillingGateway,
        StripeBillingProperties properties
    ) {
        this.billingAccountRepository = billingAccountRepository;
        this.billingInvoiceRepository = billingInvoiceRepository;
        this.accommodationRepository = accommodationRepository;
        this.currentAppUserService = currentAppUserService;
        this.stripeBillingGateway = stripeBillingGateway;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public BillingStatusSummary getCurrentOwnerBillingStatus() {
        Long ownerId = currentAppUserService.requireCurrentUserId();
        int accommodationCount = accommodationRepository.countByOwnerId(ownerId);
        Optional<BillingAccount> account = billingAccountRepository.findByOwnerId(ownerId);
        if (account.isEmpty()) {
            return new BillingStatusSummary(false, null, 0, accommodationCount, null, false, null, "Cuenta manual", "No gestionado por Stripe");
        }
        BillingAccount billingAccount = account.get();
        return new BillingStatusSummary(
            true,
            billingAccount.getStatus(),
            billingAccount.getPaidAccommodationLimit(),
            accommodationCount,
            billingAccount.getCurrentPeriodEnd(),
            billingAccount.isCancelAtPeriodEnd(),
            billingAccount.getCustomerCountry(),
            customerTypeLabel(billingAccount.getCustomerType()),
            taxModeLabel(billingAccount.getTaxMode())
        );
    }

    @Transactional(readOnly = true)
    public List<BillingInvoice> getCurrentOwnerInvoices() {
        Long ownerId = currentAppUserService.requireCurrentUserId();
        return billingAccountRepository.findByOwnerId(ownerId)
            .map(billingInvoiceRepository::findTop5ByBillingAccountOrderByCreatedAtDesc)
            .orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public String createCurrentOwnerPortalSession() {
        AppUser owner = currentAppUserService.requireCurrentUserEntity();
        BillingAccount account = billingAccountRepository.findByOwnerId(owner.getId())
            .orElseThrow(() -> new IllegalStateException("Esta cuenta todavía no está gestionada por Stripe."));
        String returnUrl = properties.getPublicBaseUrl().replaceAll("/+$", "") + "/bookings/billing";
        return stripeBillingGateway.createCustomerPortalSession(account.getStripeCustomerId(), returnUrl);
    }

    @Transactional
    public void syncSubscription(StripeSubscriptionSnapshot subscription) {
        BillingAccount account = billingAccountRepository.findByStripeSubscriptionId(subscription.id())
            .or(() -> billingAccountRepository.findByStripeCustomerId(subscription.customerId()))
            .orElseThrow(() -> new IllegalStateException("No he encontrado la cuenta de billing de esa suscripción."));
        account.syncSubscription(
            subscription.id(),
            subscription.subscriptionItemId(),
            BillingSubscriptionStatus.fromStripeStatus(subscription.status()),
            Math.max(1, subscription.quantity()),
            subscription.currentPeriodStart(),
            subscription.currentPeriodEnd(),
            subscription.cancelAtPeriodEnd()
        );
    }

    @Transactional
    public void syncInvoice(StripeInvoiceSnapshot invoice) {
        BillingAccount account = findAccountForInvoice(invoice);
        BillingInvoice billingInvoice = billingInvoiceRepository.findByStripeInvoiceId(invoice.id())
            .orElseGet(() -> new BillingInvoice(account, invoice.id(), OffsetDateTime.now()));
        billingInvoice.sync(
            invoice.number(),
            invoice.status(),
            invoice.totalAmount(),
            invoice.currency(),
            invoice.taxAmount(),
            invoice.taxCountry(),
            invoice.taxBehavior(),
            invoice.hostedInvoiceUrl(),
            invoice.invoicePdfUrl(),
            invoice.periodStart(),
            invoice.periodEnd()
        );
        billingInvoiceRepository.save(billingInvoice);
    }

    @Transactional(readOnly = true)
    public void assertCanCreateAccommodation(AppUser owner) {
        Optional<BillingAccount> account = billingAccountRepository.findByOwnerId(owner.getId());
        if (account.isEmpty()) {
            return;
        }
        int currentCount = accommodationRepository.countByOwnerId(owner.getId());
        BillingAccount billingAccount = account.get();
        if (!billingAccount.getStatus().isUsable(billingAccount.getCurrentPeriodEnd(), properties.getGracePeriodDays(), OffsetDateTime.now())) {
            throw new BillingLimitExceededException("Tu suscripción no está activa. Revisa el pago antes de crear más viviendas.");
        }
        if (currentCount >= billingAccount.getPaidAccommodationLimit()) {
            throw new BillingLimitExceededException("Has contratado "
                + billingAccount.getPaidAccommodationLimit()
                + " vivienda(s). Para añadir otra, amplía la cantidad en facturación.");
        }
    }

    private BillingAccount findAccountForInvoice(StripeInvoiceSnapshot invoice) {
        if (invoice.subscriptionId() != null && !invoice.subscriptionId().isBlank()) {
            Optional<BillingAccount> bySubscription = billingAccountRepository.findByStripeSubscriptionId(invoice.subscriptionId());
            if (bySubscription.isPresent()) {
                return bySubscription.get();
            }
        }
        return billingAccountRepository.findByStripeCustomerId(invoice.customerId())
            .orElseThrow(() -> new IllegalStateException("No he encontrado la cuenta de billing de esa factura."));
    }

    private String customerTypeLabel(BillingCustomerType customerType) {
        if (customerType == null) {
            return "Sin confirmar";
        }
        return switch (customerType) {
            case BUSINESS -> "Empresa o autónomo";
            case INDIVIDUAL -> "Particular";
            case UNKNOWN -> "Sin confirmar";
        };
    }

    private String taxModeLabel(BillingTaxMode taxMode) {
        if (taxMode == null) {
            return "Sin confirmar";
        }
        return switch (taxMode) {
            case OSS_EU_B2C -> "OSS UE B2C";
            case EU_B2B_REVERSE_CHARGE -> "B2B UE";
            case DOMESTIC_OR_OTHER -> "Doméstico u otro";
            case UNKNOWN -> "Sin confirmar";
        };
    }
}
