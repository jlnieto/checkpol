package es.checkpol.service.billing;

import es.checkpol.config.StripeBillingProperties;
import es.checkpol.domain.AppUser;
import es.checkpol.domain.AppUserRole;
import es.checkpol.domain.billing.BillingAccount;
import es.checkpol.domain.billing.BillingSubscriptionStatus;
import es.checkpol.domain.billing.PendingSignup;
import es.checkpol.domain.billing.PendingSignupStatus;
import es.checkpol.repository.AppUserRepository;
import es.checkpol.repository.billing.BillingAccountRepository;
import es.checkpol.repository.billing.PendingSignupRepository;
import es.checkpol.web.billing.RegistrationForm;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class RegistrationBillingService {

    private final PendingSignupRepository pendingSignupRepository;
    private final AppUserRepository appUserRepository;
    private final BillingAccountRepository billingAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final StripeBillingGateway stripeBillingGateway;
    private final StripeBillingProperties properties;

    public RegistrationBillingService(
        PendingSignupRepository pendingSignupRepository,
        AppUserRepository appUserRepository,
        BillingAccountRepository billingAccountRepository,
        PasswordEncoder passwordEncoder,
        StripeBillingGateway stripeBillingGateway,
        StripeBillingProperties properties
    ) {
        this.pendingSignupRepository = pendingSignupRepository;
        this.appUserRepository = appUserRepository;
        this.billingAccountRepository = billingAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.stripeBillingGateway = stripeBillingGateway;
        this.properties = properties;
    }

    @Transactional
    public RegistrationCheckout startSignup(RegistrationForm form) {
        if (!properties.isCheckoutConfigured()) {
            throw new BillingConfigurationException("Falta configurar Stripe antes de aceptar contrataciones.");
        }
        String email = normalizeEmail(form.email());
        if (appUserRepository.existsByUsername(email)) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese email.");
        }
        OffsetDateTime now = OffsetDateTime.now();
        List<PendingSignup> pendingSignups = pendingSignupRepository.findAllByEmailAndStatus(email, PendingSignupStatus.PENDING_PAYMENT);
        for (PendingSignup pendingSignup : pendingSignups) {
            if (pendingSignup.isExpired(now)) {
                pendingSignup.expire();
            } else {
                throw new IllegalArgumentException("Ya hay un registro pendiente con ese email. Revisa el pago o vuelve a intentarlo más tarde.");
            }
        }
        if (!pendingSignups.isEmpty()) {
            pendingSignupRepository.flush();
        }

        PendingSignup signup = new PendingSignup(
            email,
            passwordEncoder.encode(form.password().trim()),
            form.accommodationQuantity(),
            UUID.randomUUID().toString(),
            now.plusMinutes(properties.getSignupExpirationMinutes()),
            now
        );
        pendingSignupRepository.saveAndFlush(signup);

        EmbeddedCheckoutSession checkoutSession = stripeBillingGateway.createSubscriptionCheckout(signup);
        signup.storeCheckout(checkoutSession.customerId(), checkoutSession.checkoutSessionId(), checkoutSession.clientSecret());
        return new RegistrationCheckout(signup, properties.getPublishableKey());
    }

    @Transactional(readOnly = true)
    public PendingSignup getSignupByToken(String token) {
        return pendingSignupRepository.findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado ese registro."));
    }

    public String getPublishableKey() {
        return properties.getPublishableKey();
    }

    @Transactional
    public void completeFromCheckout(StripeCheckoutSessionSnapshot checkoutSession) {
        PendingSignup signup = findSignup(checkoutSession);
        if (signup.getStatus() == PendingSignupStatus.COMPLETED) {
            return;
        }
        if (!signup.isPendingPayment()) {
            throw new IllegalStateException("El registro ya no está pendiente de pago.");
        }
        StripeSubscriptionSnapshot subscription = stripeBillingGateway.retrieveSubscription(checkoutSession.subscriptionId());
        BillingSubscriptionStatus status = BillingSubscriptionStatus.fromStripeStatus(subscription.status());
        if (!status.isUsable(subscription.currentPeriodEnd(), properties.getGracePeriodDays(), OffsetDateTime.now())) {
            throw new IllegalStateException("Stripe todavía no ha dejado la suscripción en un estado utilizable.");
        }

        AppUser owner = createOwner(signup);
        BillingAccount account = new BillingAccount(
            owner,
            checkoutSession.customerId(),
            status,
            Math.max(1, subscription.quantity()),
            OffsetDateTime.now()
        );
        account.syncSubscription(
            subscription.id(),
            subscription.subscriptionItemId(),
            status,
            Math.max(1, subscription.quantity()),
            subscription.currentPeriodStart(),
            subscription.currentPeriodEnd(),
            subscription.cancelAtPeriodEnd()
        );
        account.updateCustomerTaxProfile(checkoutSession.customerCountry(), checkoutSession.customerType(), checkoutSession.taxMode());
        billingAccountRepository.save(account);
        signup.complete();
    }

    private PendingSignup findSignup(StripeCheckoutSessionSnapshot checkoutSession) {
        if (checkoutSession.id() != null) {
            return pendingSignupRepository.findByStripeCheckoutSessionId(checkoutSession.id())
                .orElseGet(() -> findSignupByMetadata(checkoutSession));
        }
        return findSignupByMetadata(checkoutSession);
    }

    private PendingSignup findSignupByMetadata(StripeCheckoutSessionSnapshot checkoutSession) {
        String pendingSignupId = checkoutSession.metadata().get("pending_signup_id");
        if (pendingSignupId == null || pendingSignupId.isBlank()) {
            throw new IllegalStateException("El webhook de Stripe no incluye el registro pendiente.");
        }
        return pendingSignupRepository.findById(Long.valueOf(pendingSignupId))
            .orElseThrow(() -> new IllegalStateException("El registro pendiente de Stripe no existe."));
    }

    private AppUser createOwner(PendingSignup signup) {
        if (appUserRepository.existsByUsername(signup.getEmail())) {
            return appUserRepository.findByUsername(signup.getEmail())
                .orElseThrow(() -> new IllegalStateException("El usuario existe pero no se puede cargar."));
        }
        OffsetDateTime now = OffsetDateTime.now();
        return appUserRepository.save(new AppUser(
            signup.getEmail(),
            signup.getPasswordHash(),
            displayName(signup.getEmail()),
            AppUserRole.OWNER,
            true,
            now,
            now
        ));
    }

    private String displayName(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return email;
        }
        String local = email.substring(0, at).replace('.', ' ').replace('_', ' ').trim();
        return local.isBlank() ? email : local;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
