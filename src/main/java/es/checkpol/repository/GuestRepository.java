package es.checkpol.repository;

import es.checkpol.domain.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GuestRepository extends JpaRepository<Guest, Long> {

    List<Guest> findAllByBookingIdOrderByIdAsc(Long bookingId);

    List<Guest> findAllByBookingIdAndBookingOwnerIdOrderByIdAsc(Long bookingId, Long ownerId);

    long countByBookingId(Long bookingId);

    Optional<Guest> findByIdAndBookingOwnerId(Long id, Long ownerId);

    Optional<Guest> findByIdAndBookingId(Long id, Long bookingId);

    @Modifying
    @Query("""
        delete from Guest guest
        where guest.booking.id = :bookingId
          and guest.booking.owner.id = :ownerId
        """)
    int deleteAllByBookingIdAndOwnerId(Long bookingId, Long ownerId);
}
