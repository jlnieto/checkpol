package es.checkpol.repository;

import es.checkpol.domain.Accommodation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {

    List<Accommodation> findAllByOwnerIdOrderByNameAsc(Long ownerId);

    Optional<Accommodation> findByIdAndOwnerId(Long id, Long ownerId);
}
