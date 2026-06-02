package es.checkpol.repository.billing;

import es.checkpol.domain.billing.StripeEventLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StripeEventLogRepository extends JpaRepository<StripeEventLog, Long> {

    Optional<StripeEventLog> findByStripeEventId(String stripeEventId);
}
