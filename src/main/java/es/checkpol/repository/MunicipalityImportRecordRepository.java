package es.checkpol.repository;

import es.checkpol.domain.MunicipalityImportRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MunicipalityImportRecordRepository extends JpaRepository<MunicipalityImportRecord, Long> {

    List<MunicipalityImportRecord> findTop10ByOrderByCreatedAtDesc();
}
