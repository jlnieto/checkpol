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
        order by b.checkInDate asc, b.id asc
        """)
    List<Booking> findAllForList();

    @Query("""
        select b
        from Booking b
        join fetch b.accommodation
        where b.id = :id
        """)
    Optional<Booking> findDetailById(Long id);

    @Query("""
        select b
        from Booking b
        join fetch b.accommodation
        where b.selfServiceToken = :token
        """)
    Optional<Booking> findBySelfServiceToken(String token);
}
