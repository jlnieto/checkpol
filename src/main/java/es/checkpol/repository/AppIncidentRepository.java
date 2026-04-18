package es.checkpol.repository;

import es.checkpol.domain.AppIncident;
import es.checkpol.domain.AppIncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AppIncidentRepository extends JpaRepository<AppIncident, Long> {

    long countByStatus(AppIncidentStatus status);

    long countByStatusIn(Collection<AppIncidentStatus> statuses);

    List<AppIncident> findTop100ByStatusOrderByCreatedAtDesc(AppIncidentStatus status);

    List<AppIncident> findTop100ByStatusInOrderByCreatedAtDesc(Collection<AppIncidentStatus> statuses);

    List<AppIncident> findTop100ByOrderByCreatedAtDesc();
}
