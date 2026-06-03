package es.checkpol.repository;

import es.checkpol.domain.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findAllByBookingIdOrderByIdAsc(Long bookingId);

    List<Address> findAllByBookingIdAndBookingOwnerIdOrderByIdAsc(Long bookingId, Long ownerId);

    Optional<Address> findByIdAndBookingId(Long id, Long bookingId);

    Optional<Address> findByIdAndBookingIdAndBookingOwnerId(Long id, Long bookingId, Long ownerId);

    @Modifying
    @Query("""
        delete from Address address
        where address.booking.id = :bookingId
          and address.booking.owner.id = :ownerId
        """)
    int deleteAllByBookingIdAndOwnerId(Long bookingId, Long ownerId);
}
