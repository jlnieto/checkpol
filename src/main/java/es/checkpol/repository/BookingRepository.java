package es.checkpol.repository;

import es.checkpol.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
        select b
        from Booking b
        join fetch b.accommodation
        where b.owner.id = :ownerId
          and b.archivedAt is null
        order by b.checkInDate asc, b.id asc
        """)
    List<Booking> findAllForList(Long ownerId);

    @Query("""
        select b
        from Booking b
        join fetch b.accommodation
        where b.owner.id = :ownerId
          and b.archivedAt is not null
        order by b.archivedAt desc, b.id desc
        """)
    List<Booking> findAllArchivedForList(Long ownerId);

    long countByOwnerIdAndArchivedAtIsNotNull(Long ownerId);

    @Query("""
        select b
        from Booking b
        join fetch b.accommodation
        where b.id = :id
          and b.owner.id = :ownerId
        """)
    Optional<Booking> findDetailById(Long id, Long ownerId);

    Optional<Booking> findByIdAndOwnerId(Long id, Long ownerId);

    @Query("""
        select b
        from Booking b
        join fetch b.accommodation
        where b.selfServiceToken = :token
        """)
    Optional<Booking> findBySelfServiceToken(String token);
}
