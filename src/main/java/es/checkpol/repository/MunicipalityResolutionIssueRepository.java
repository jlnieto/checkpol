package es.checkpol.repository;

import es.checkpol.domain.MunicipalityIssueStatus;
import es.checkpol.domain.MunicipalityResolutionIssue;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MunicipalityResolutionIssueRepository extends JpaRepository<MunicipalityResolutionIssue, Long> {

    Optional<MunicipalityResolutionIssue> findFirstByGuestIdAndIssueStatus(Long guestId, MunicipalityIssueStatus issueStatus);

    @EntityGraph(attributePaths = {"guest", "guest.booking", "guest.booking.accommodation"})
    List<MunicipalityResolutionIssue> findAllByIssueStatusOrderByCreatedAtDesc(MunicipalityIssueStatus issueStatus);

    @EntityGraph(attributePaths = {"guest", "guest.booking", "guest.booking.accommodation"})
    Optional<MunicipalityResolutionIssue> findById(Long id);
}
