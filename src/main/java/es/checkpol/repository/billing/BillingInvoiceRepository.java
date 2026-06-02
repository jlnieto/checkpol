package es.checkpol.repository.billing;

import es.checkpol.domain.billing.BillingAccount;
import es.checkpol.domain.billing.BillingInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingInvoiceRepository extends JpaRepository<BillingInvoice, Long> {

    Optional<BillingInvoice> findByStripeInvoiceId(String stripeInvoiceId);

    List<BillingInvoice> findTop5ByBillingAccountOrderByCreatedAtDesc(BillingAccount billingAccount);
}
