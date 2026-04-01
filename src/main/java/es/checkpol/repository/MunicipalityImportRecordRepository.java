package es.checkpol.repository;

import es.checkpol.domain.MunicipalityImportRecord;
import es.checkpol.domain.MunicipalityImportOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MunicipalityImportRecordRepository extends JpaRepository<MunicipalityImportRecord, Long> {

    List<MunicipalityImportRecord> findTop10ByOrderByCreatedAtDesc();

    Optional<MunicipalityImportRecord> findTopByOperationTypeOrderByCreatedAtDesc(MunicipalityImportOperation operationType);

    List<MunicipalityImportRecord> findTop10ByOperationTypeOrderByCreatedAtDesc(MunicipalityImportOperation operationType);
}
