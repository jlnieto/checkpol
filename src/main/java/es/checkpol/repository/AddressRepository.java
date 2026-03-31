package es.checkpol.repository;

import es.checkpol.domain.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findAllByBookingIdOrderByIdAsc(Long bookingId);

    List<Address> findAllByBookingIdAndBookingOwnerIdOrderByIdAsc(Long bookingId, Long ownerId);

    Optional<Address> findByIdAndBookingId(Long id, Long bookingId);

    Optional<Address> findByIdAndBookingIdAndBookingOwnerId(Long id, Long bookingId, Long ownerId);
}
