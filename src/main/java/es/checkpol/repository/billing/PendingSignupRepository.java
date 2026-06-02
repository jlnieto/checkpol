package es.checkpol.repository.billing;

import es.checkpol.domain.billing.PendingSignup;
import es.checkpol.domain.billing.PendingSignupStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PendingSignupRepository extends JpaRepository<PendingSignup, Long> {

    Optional<PendingSignup> findByToken(String token);

    Optional<PendingSignup> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);

    List<PendingSignup> findAllByEmailAndStatus(String email, PendingSignupStatus status);
}
