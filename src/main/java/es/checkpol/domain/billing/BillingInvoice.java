package es.checkpol.domain.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "billing_invoices")
public class BillingInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billing_account_id", nullable = false)
    private BillingAccount billingAccount;

    @Column(name = "stripe_invoice_id", nullable = false, unique = true, length = 120)
    private String stripeInvoiceId;

    @Column(name = "stripe_invoice_number", length = 120)
    private String stripeInvoiceNumber;

    @Column(length = 40)
    private String status;

    @Column(name = "total_amount")
    private Long totalAmount;

    @Column(length = 10)
    private String currency;

    @Column(name = "tax_amount")
    private Long taxAmount;

    @Column(name = "tax_country", length = 2)
    private String taxCountry;

    @Column(name = "tax_behavior", length = 40)
    private String taxBehavior;

    @Column(name = "hosted_invoice_url")
    private String hostedInvoiceUrl;

    @Column(name = "invoice_pdf_url")
    private String invoicePdfUrl;

    @Column(name = "period_start")
    private OffsetDateTime periodStart;

    @Column(name = "period_end")
    private OffsetDateTime periodEnd;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected BillingInvoice() {
    }

    public BillingInvoice(BillingAccount billingAccount, String stripeInvoiceId, OffsetDateTime createdAt) {
        this.billingAccount = billingAccount;
        this.stripeInvoiceId = stripeInvoiceId;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public BillingAccount getBillingAccount() {
        return billingAccount;
    }

    public String getStripeInvoiceId() {
        return stripeInvoiceId;
    }

    public String getStripeInvoiceNumber() {
        return stripeInvoiceNumber;
    }

    public String getStatus() {
        return status;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public Long getTaxAmount() {
        return taxAmount;
    }

    public String getTaxCountry() {
        return taxCountry;
    }

    public String getTaxBehavior() {
        return taxBehavior;
    }

    public String getHostedInvoiceUrl() {
        return hostedInvoiceUrl;
    }

    public String getInvoicePdfUrl() {
        return invoicePdfUrl;
    }

    public OffsetDateTime getPeriodStart() {
        return periodStart;
    }

    public OffsetDateTime getPeriodEnd() {
        return periodEnd;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void sync(
        String stripeInvoiceNumber,
        String status,
        Long totalAmount,
        String currency,
        Long taxAmount,
        String taxCountry,
        String taxBehavior,
        String hostedInvoiceUrl,
        String invoicePdfUrl,
        OffsetDateTime periodStart,
        OffsetDateTime periodEnd
    ) {
        this.stripeInvoiceNumber = stripeInvoiceNumber;
        this.status = status;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.taxAmount = taxAmount;
        this.taxCountry = taxCountry;
        this.taxBehavior = taxBehavior;
        this.hostedInvoiceUrl = hostedInvoiceUrl;
        this.invoicePdfUrl = invoicePdfUrl;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        touch();
    }

    private void touch() {
        this.updatedAt = OffsetDateTime.now();
    }
}
