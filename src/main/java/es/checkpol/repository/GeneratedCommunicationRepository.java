package es.checkpol.repository;

import es.checkpol.domain.GeneratedCommunication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GeneratedCommunicationRepository extends JpaRepository<GeneratedCommunication, Long> {

    List<GeneratedCommunication> findAllByBookingIdOrderByGeneratedAtDesc(Long bookingId);

    Optional<GeneratedCommunication> findFirstByBookingIdOrderByGeneratedAtDesc(Long bookingId);

    Optional<GeneratedCommunication> findFirstByBookingIdOrderByVersionDesc(Long bookingId);

    Optional<GeneratedCommunication> findByIdAndBookingId(Long id, Long bookingId);
}
