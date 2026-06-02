package es.checkpol.infrastructure.stripe;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import es.checkpol.config.StripeBillingProperties;
import es.checkpol.domain.billing.BillingCustomerType;
import es.checkpol.domain.billing.BillingTaxMode;
import es.checkpol.domain.billing.PendingSignup;
import es.checkpol.service.billing.BillingConfigurationException;
import es.checkpol.service.billing.BillingProviderException;
import es.checkpol.service.billing.EmbeddedCheckoutSession;
import es.checkpol.service.billing.StripeBillingGateway;
import es.checkpol.service.billing.StripeCheckoutSessionSnapshot;
import es.checkpol.service.billing.StripeInvoiceSnapshot;
import es.checkpol.service.billing.StripeSubscriptionSnapshot;
import es.checkpol.service.billing.StripeWebhookEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class StripeBillingGatewayAdapter implements StripeBillingGateway {

    private static final String PENDING_SIGNUP_ID = "pending_signup_id";
    private static final Set<String> EU_COUNTRIES = Set.of(
        "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HU",
        "IE", "IT", "LV", "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE"
    );

    private final StripeBillingProperties properties;

    public StripeBillingGatewayAdapter(StripeBillingProperties properties) {
        this.properties = properties;
    }

    @Override
    public EmbeddedCheckoutSession createSubscriptionCheckout(PendingSignup signup) {
        requireCheckoutConfiguration();
        try {
            Stripe.apiKey = properties.getSecretKey();
            Customer customer = Customer.create(CustomerCreateParams.builder()
                .setEmail(signup.getEmail())
                .putMetadata(PENDING_SIGNUP_ID, signup.getId().toString())
                .build());

            String returnUrl = baseUrl() + "/registro/confirmando/" + signup.getToken()
                + "?session_id={CHECKOUT_SESSION_ID}";
            SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setUiMode(SessionCreateParams.UiMode.EMBEDDED_PAGE)
                .setReturnUrl(returnUrl)
                .setCustomer(customer.getId())
                .setBillingAddressCollection(SessionCreateParams.BillingAddressCollection.REQUIRED)
                .setAutomaticTax(SessionCreateParams.AutomaticTax.builder().setEnabled(true).build())
                .setTaxIdCollection(SessionCreateParams.TaxIdCollection.builder().setEnabled(true).build())
                .addLineItem(SessionCreateParams.LineItem.builder()
                    .setPrice(properties.getPriceCheckpolEsencial())
                    .setQuantity((long) signup.getAccommodationQuantity())
                    .build())
                .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                    .putMetadata(PENDING_SIGNUP_ID, signup.getId().toString())
                    .build())
                .putMetadata(PENDING_SIGNUP_ID, signup.getId().toString())
                .build();

            Session session = Session.create(params);
            return new EmbeddedCheckoutSession(customer.getId(), session.getId(), session.getClientSecret());
        } catch (StripeException ex) {
            throw new BillingProviderException("Stripe no ha podido iniciar el pago.", ex);
        }
    }

    @Override
    public StripeSubscriptionSnapshot retrieveSubscription(String subscriptionId) {
        requireSecretKey();
        try {
            Stripe.apiKey = properties.getSecretKey();
            return toSubscriptionSnapshot(Subscription.retrieve(subscriptionId));
        } catch (StripeException ex) {
            throw new BillingProviderException("Stripe no ha podido leer la suscripción.", ex);
        }
    }

    @Override
    public String createCustomerPortalSession(String stripeCustomerId, String returnUrl) {
        requireSecretKey();
        try {
            Stripe.apiKey = properties.getSecretKey();
            var builder = com.stripe.param.billingportal.SessionCreateParams.builder()
                .setCustomer(stripeCustomerId)
                .setReturnUrl(returnUrl);
            if (!properties.getCustomerPortalConfigurationId().isBlank()) {
                builder.setConfiguration(properties.getCustomerPortalConfigurationId());
            }
            return com.stripe.model.billingportal.Session.create(builder.build()).getUrl();
        } catch (StripeException ex) {
            throw new BillingProviderException("Stripe no ha podido abrir el portal de facturación.", ex);
        }
    }

    @Override
    public StripeWebhookEvent constructWebhookEvent(String payload, String signatureHeader) {
        if (!properties.isWebhookConfigured()) {
            throw new BillingConfigurationException("Falta configurar el secreto de webhook de Stripe.");
        }
        try {
            Stripe.apiKey = properties.getSecretKey();
            Event event = Webhook.constructEvent(payload, signatureHeader, properties.getWebhookSecret());
            StripeObject object = event.getDataObjectDeserializer().getObject()
                .orElseThrow(() -> new BillingProviderException("Stripe no ha enviado un objeto legible en el webhook.", null));
            return new StripeWebhookEvent(
                event.getId(),
                event.getType(),
                object instanceof Session session ? toCheckoutSessionSnapshot(session) : null,
                object instanceof Subscription subscription ? toSubscriptionSnapshot(subscription) : null,
                object instanceof Invoice invoice ? toInvoiceSnapshot(invoice) : null
            );
        } catch (BillingProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BillingProviderException("No se ha podido verificar el webhook de Stripe.", ex);
        }
    }

    private StripeCheckoutSessionSnapshot toCheckoutSessionSnapshot(Session session) {
        String country = null;
        BillingCustomerType customerType = BillingCustomerType.UNKNOWN;
        BillingTaxMode taxMode = BillingTaxMode.UNKNOWN;
        if (session.getCustomerDetails() != null) {
            if (session.getCustomerDetails().getAddress() != null) {
                country = normalizeCountry(session.getCustomerDetails().getAddress().getCountry());
            }
            List<Session.CustomerDetails.TaxId> taxIds = session.getCustomerDetails().getTaxIds();
            customerType = taxIds != null && !taxIds.isEmpty() ? BillingCustomerType.BUSINESS : BillingCustomerType.INDIVIDUAL;
            taxMode = resolveTaxMode(country, customerType);
        }
        return new StripeCheckoutSessionSnapshot(
            session.getId(),
            session.getCustomer(),
            session.getSubscription(),
            session.getPaymentStatus(),
            nullToEmpty(session.getMetadata()),
            country,
            customerType,
            taxMode
        );
    }

    private StripeSubscriptionSnapshot toSubscriptionSnapshot(Subscription subscription) {
        SubscriptionItem item = firstSubscriptionItem(subscription);
        int quantity = item == null || item.getQuantity() == null ? 0 : Math.toIntExact(item.getQuantity());
        return new StripeSubscriptionSnapshot(
            subscription.getId(),
            subscription.getCustomer(),
            item == null ? null : item.getId(),
            subscription.getStatus(),
            quantity,
            item == null ? null : toOffsetDateTime(item.getCurrentPeriodStart()),
            item == null ? null : toOffsetDateTime(item.getCurrentPeriodEnd()),
            Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd()),
            nullToEmpty(subscription.getMetadata())
        );
    }

    private StripeInvoiceSnapshot toInvoiceSnapshot(Invoice invoice) {
        Invoice.TotalTax firstTax = firstTotalTax(invoice);
        return new StripeInvoiceSnapshot(
            invoice.getId(),
            invoice.getCustomer(),
            subscriptionId(invoice),
            invoice.getNumber(),
            invoice.getStatus(),
            invoice.getTotal(),
            invoice.getCurrency(),
            taxAmount(invoice),
            invoice.getCustomerAddress() == null ? null : normalizeCountry(invoice.getCustomerAddress().getCountry()),
            firstTax == null ? null : firstTax.getTaxBehavior(),
            invoice.getHostedInvoiceUrl(),
            invoice.getInvoicePdf(),
            toOffsetDateTime(invoice.getPeriodStart()),
            toOffsetDateTime(invoice.getPeriodEnd())
        );
    }

    private SubscriptionItem firstSubscriptionItem(Subscription subscription) {
        if (subscription.getItems() == null || subscription.getItems().getData() == null || subscription.getItems().getData().isEmpty()) {
            return null;
        }
        return subscription.getItems().getData().getFirst();
    }

    private Invoice.TotalTax firstTotalTax(Invoice invoice) {
        if (invoice.getTotalTaxes() == null || invoice.getTotalTaxes().isEmpty()) {
            return null;
        }
        return invoice.getTotalTaxes().getFirst();
    }

    private Long taxAmount(Invoice invoice) {
        if (invoice.getTotalTaxes() == null || invoice.getTotalTaxes().isEmpty()) {
            return 0L;
        }
        return invoice.getTotalTaxes().stream()
            .map(Invoice.TotalTax::getAmount)
            .filter(amount -> amount != null)
            .reduce(0L, Long::sum);
    }

    private String subscriptionId(Invoice invoice) {
        if (invoice.getParent() == null || invoice.getParent().getSubscriptionDetails() == null) {
            return null;
        }
        return invoice.getParent().getSubscriptionDetails().getSubscription();
    }

    private BillingTaxMode resolveTaxMode(String country, BillingCustomerType customerType) {
        if (country == null || customerType == BillingCustomerType.UNKNOWN) {
            return BillingTaxMode.UNKNOWN;
        }
        if (EU_COUNTRIES.contains(country) && customerType == BillingCustomerType.INDIVIDUAL) {
            return BillingTaxMode.OSS_EU_B2C;
        }
        if (EU_COUNTRIES.contains(country) && customerType == BillingCustomerType.BUSINESS && !"IE".equals(country)) {
            return BillingTaxMode.EU_B2B_REVERSE_CHARGE;
        }
        return BillingTaxMode.DOMESTIC_OR_OTHER;
    }

    private OffsetDateTime toOffsetDateTime(Long epochSeconds) {
        return epochSeconds == null ? null : OffsetDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC);
    }

    private Map<String, String> nullToEmpty(Map<String, String> metadata) {
        return metadata == null ? Map.of() : metadata;
    }

    private String normalizeCountry(String country) {
        return country == null || country.isBlank() ? null : country.trim().toUpperCase(Locale.ROOT);
    }

    private String baseUrl() {
        return properties.getPublicBaseUrl().replaceAll("/+$", "");
    }

    private void requireCheckoutConfiguration() {
        if (!properties.isCheckoutConfigured()) {
            throw new BillingConfigurationException("Falta configurar Stripe para poder contratar.");
        }
    }

    private void requireSecretKey() {
        if (properties.getSecretKey().isBlank()) {
            throw new BillingConfigurationException("Falta configurar la clave secreta de Stripe.");
        }
    }
}
