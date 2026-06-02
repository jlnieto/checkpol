package es.checkpol.repository.billing;

import es.checkpol.domain.billing.BillingAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingAccountRepository extends JpaRepository<BillingAccount, Long> {

    Optional<BillingAccount> findByOwnerId(Long ownerId);

    Optional<BillingAccount> findByStripeCustomerId(String stripeCustomerId);

    Optional<BillingAccount> findByStripeSubscriptionId(String stripeSubscriptionId);
}
