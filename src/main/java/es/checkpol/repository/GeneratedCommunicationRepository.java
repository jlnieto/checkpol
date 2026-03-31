package es.checkpol.repository;

import es.checkpol.domain.GeneratedCommunication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GeneratedCommunicationRepository extends JpaRepository<GeneratedCommunication, Long> {

    List<GeneratedCommunication> findAllByBookingIdAndBookingOwnerIdOrderByGeneratedAtDesc(Long bookingId, Long ownerId);

    Optional<GeneratedCommunication> findFirstByBookingIdAndBookingOwnerIdOrderByGeneratedAtDesc(Long bookingId, Long ownerId);

    Optional<GeneratedCommunication> findFirstByBookingIdAndBookingOwnerIdOrderByVersionDesc(Long bookingId, Long ownerId);

    Optional<GeneratedCommunication> findByIdAndBookingIdAndBookingOwnerId(Long id, Long bookingId, Long ownerId);
}
