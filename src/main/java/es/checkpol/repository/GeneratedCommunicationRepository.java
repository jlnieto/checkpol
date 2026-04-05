package es.checkpol.repository;

import es.checkpol.domain.GeneratedCommunication;
import es.checkpol.domain.CommunicationDispatchStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GeneratedCommunicationRepository extends JpaRepository<GeneratedCommunication, Long> {

    List<GeneratedCommunication> findAllByBookingIdAndBookingOwnerIdOrderByGeneratedAtDesc(Long bookingId, Long ownerId);

    Optional<GeneratedCommunication> findFirstByBookingIdAndBookingOwnerIdOrderByGeneratedAtDesc(Long bookingId, Long ownerId);

    Optional<GeneratedCommunication> findFirstByBookingIdAndBookingOwnerIdOrderByVersionDesc(Long bookingId, Long ownerId);

    Optional<GeneratedCommunication> findByIdAndBookingIdAndBookingOwnerId(Long id, Long bookingId, Long ownerId);

    List<GeneratedCommunication> findTop25ByDispatchStatusOrderBySubmittedAtAsc(CommunicationDispatchStatus dispatchStatus);

    Optional<GeneratedCommunication> findFirstByBookingIdOrderByVersionDesc(Long bookingId);

    long countByDispatchStatus(CommunicationDispatchStatus dispatchStatus);

    long countByDispatchStatusIn(List<CommunicationDispatchStatus> statuses);

    long countByBookingOwnerIdAndDispatchStatus(Long ownerId, CommunicationDispatchStatus dispatchStatus);

    long countByBookingOwnerIdAndDispatchStatusIn(Long ownerId, List<CommunicationDispatchStatus> statuses);

    Optional<GeneratedCommunication> findFirstByBookingOwnerIdOrderByGeneratedAtDesc(Long ownerId);

    Optional<GeneratedCommunication> findFirstByBookingOwnerIdAndDispatchStatusOrderBySubmittedAtDesc(Long ownerId, CommunicationDispatchStatus dispatchStatus);

    Optional<GeneratedCommunication> findFirstByBookingOwnerIdAndDispatchStatusInOrderByGeneratedAtDesc(Long ownerId, List<CommunicationDispatchStatus> statuses);

    @EntityGraph(attributePaths = {"booking", "booking.owner", "booking.accommodation"})
    List<GeneratedCommunication> findTop100ByOrderByGeneratedAtDesc();

    @EntityGraph(attributePaths = {"booking", "booking.owner", "booking.accommodation"})
    List<GeneratedCommunication> findTop100ByDispatchStatusOrderByGeneratedAtDesc(CommunicationDispatchStatus dispatchStatus);

    @EntityGraph(attributePaths = {"booking", "booking.owner", "booking.accommodation"})
    List<GeneratedCommunication> findTop100ByDispatchStatusInOrderByGeneratedAtDesc(List<CommunicationDispatchStatus> statuses);

    @Query("""
        select communication
        from GeneratedCommunication communication
        join fetch communication.booking booking
        join fetch booking.owner owner
        join fetch booking.accommodation accommodation
        where communication.id = :communicationId
        """)
    Optional<GeneratedCommunication> findAdminDetailById(Long communicationId);
}
