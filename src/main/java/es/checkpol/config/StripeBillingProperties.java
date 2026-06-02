package es.checkpol.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "checkpol.billing.stripe")
public class StripeBillingProperties {

    private String secretKey = "";
    private String publishableKey = "";
    private String webhookSecret = "";
    private String priceCheckpolEsencial = "";
    private String customerPortalConfigurationId = "";
    private String publicBaseUrl = "http://localhost:8080";
    private int signupExpirationMinutes = 60;
    private int gracePeriodDays = 7;

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = clean(secretKey);
    }

    public String getPublishableKey() {
        return publishableKey;
    }

    public void setPublishableKey(String publishableKey) {
        this.publishableKey = clean(publishableKey);
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = clean(webhookSecret);
    }

    public String getPriceCheckpolEsencial() {
        return priceCheckpolEsencial;
    }

    public void setPriceCheckpolEsencial(String priceCheckpolEsencial) {
        this.priceCheckpolEsencial = clean(priceCheckpolEsencial);
    }

    public String getCustomerPortalConfigurationId() {
        return customerPortalConfigurationId;
    }

    public void setCustomerPortalConfigurationId(String customerPortalConfigurationId) {
        this.customerPortalConfigurationId = clean(customerPortalConfigurationId);
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = clean(publicBaseUrl);
    }

    public int getSignupExpirationMinutes() {
        return signupExpirationMinutes;
    }

    public void setSignupExpirationMinutes(int signupExpirationMinutes) {
        this.signupExpirationMinutes = signupExpirationMinutes;
    }

    public int getGracePeriodDays() {
        return gracePeriodDays;
    }

    public void setGracePeriodDays(int gracePeriodDays) {
        this.gracePeriodDays = gracePeriodDays;
    }

    public boolean isCheckoutConfigured() {
        return hasText(secretKey) && hasText(publishableKey) && hasText(priceCheckpolEsencial) && hasText(publicBaseUrl);
    }

    public boolean isWebhookConfigured() {
        return hasText(secretKey) && hasText(webhookSecret);
    }

    public boolean isPortalConfigured() {
        return hasText(secretKey);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
