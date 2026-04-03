package es.checkpol.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "admin_settings")
public class AdminSetting {

    @Id
    @Column(name = "setting_key", nullable = false, length = 120)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, columnDefinition = "text")
    private String settingValue;

    @Column(name = "updated_by_username", nullable = false, length = 80)
    private String updatedByUsername;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected AdminSetting() {
    }

    public AdminSetting(String settingKey, String settingValue, String updatedByUsername, OffsetDateTime updatedAt) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.updatedByUsername = updatedByUsername;
        this.updatedAt = updatedAt;
    }

    public String getSettingKey() {
        return settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public String getUpdatedByUsername() {
        return updatedByUsername;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(String settingValue, String updatedByUsername) {
        this.settingValue = settingValue;
        this.updatedByUsername = updatedByUsername;
        this.updatedAt = OffsetDateTime.now();
    }
}
