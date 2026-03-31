package es.checkpol.repository;

import es.checkpol.domain.Guest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuestRepository extends JpaRepository<Guest, Long> {

    List<Guest> findAllByBookingIdOrderByIdAsc(Long bookingId);

    List<Guest> findAllByBookingIdAndBookingOwnerIdOrderByIdAsc(Long bookingId, Long ownerId);

    long countByBookingId(Long bookingId);

    java.util.Optional<Guest> findByIdAndBookingOwnerId(Long id, Long ownerId);
}
