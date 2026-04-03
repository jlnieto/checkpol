package es.checkpol.repository;

import es.checkpol.domain.AdminSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminSettingRepository extends JpaRepository<AdminSetting, String> {
}
