package es.checkpol.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "ses_arrendador_code", length = 10)
    private String sesArrendadorCode;

    @Column(name = "ses_ws_username", length = 50)
    private String sesWsUsername;

    @Column(name = "ses_ws_password_encrypted", length = 500)
    private String sesWsPasswordEncrypted;

    @Enumerated(EnumType.STRING)
    @Column(name = "ses_connection_test_status", length = 40)
    private SesConnectionTestStatus sesConnectionTestStatus;

    @Column(name = "ses_connection_tested_at")
    private OffsetDateTime sesConnectionTestedAt;

    @Column(name = "ses_connection_owner_message", length = 300)
    private String sesConnectionOwnerMessage;

    @Column(name = "ses_connection_admin_message", length = 1500)
    private String sesConnectionAdminMessage;

    @Column(name = "ses_connection_test_endpoint", length = 300)
    private String sesConnectionTestEndpoint;

    @Column(name = "ses_connection_test_http_status")
    private Integer sesConnectionTestHttpStatus;

    @Column(name = "ses_connection_test_error_type", length = 80)
    private String sesConnectionTestErrorType;

    @Column(name = "ses_connection_test_raw_detail")
    private String sesConnectionTestRawDetail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppUserRole role;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected AppUser() {
    }

    public AppUser(
        String username,
        String passwordHash,
        String displayName,
        AppUserRole role,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
    ) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public AppUser(
        String username,
        String passwordHash,
        String displayName,
        String sesArrendadorCode,
        String sesWsUsername,
        String sesWsPasswordEncrypted,
        AppUserRole role,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
    ) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.sesArrendadorCode = sesArrendadorCode;
        this.sesWsUsername = sesWsUsername;
        this.sesWsPasswordEncrypted = sesWsPasswordEncrypted;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSesArrendadorCode() {
        return sesArrendadorCode;
    }

    public String getSesWsUsername() {
        return sesWsUsername;
    }

    public String getSesWsPasswordEncrypted() {
        return sesWsPasswordEncrypted;
    }

    public SesConnectionTestStatus getSesConnectionTestStatus() {
        return sesConnectionTestStatus;
    }

    public OffsetDateTime getSesConnectionTestedAt() {
        return sesConnectionTestedAt;
    }

    public String getSesConnectionOwnerMessage() {
        return sesConnectionOwnerMessage;
    }

    public String getSesConnectionAdminMessage() {
        return sesConnectionAdminMessage;
    }

    public String getSesConnectionTestEndpoint() {
        return sesConnectionTestEndpoint;
    }

    public Integer getSesConnectionTestHttpStatus() {
        return sesConnectionTestHttpStatus;
    }

    public String getSesConnectionTestErrorType() {
        return sesConnectionTestErrorType;
    }

    public String getSesConnectionTestRawDetail() {
        return sesConnectionTestRawDetail;
    }

    public AppUserRole getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateProfile(String displayName, boolean active) {
        this.displayName = displayName;
        this.active = active;
        touch();
    }

    public void updateOwner(String displayName, boolean active) {
        updateProfile(displayName, active);
    }

    public void updateSesWsConfiguration(String sesArrendadorCode, String sesWsUsername, String sesWsPasswordEncrypted) {
        this.sesArrendadorCode = sesArrendadorCode;
        this.sesWsUsername = sesWsUsername;
        this.sesWsPasswordEncrypted = sesWsPasswordEncrypted;
        clearSesConnectionTest();
        touch();
    }

    public void clearSesWsConfiguration() {
        this.sesArrendadorCode = null;
        this.sesWsUsername = null;
        this.sesWsPasswordEncrypted = null;
        clearSesConnectionTest();
        touch();
    }

    public boolean hasSesWebServiceConfiguration() {
        return sesArrendadorCode != null && !sesArrendadorCode.isBlank()
            && sesWsUsername != null && !sesWsUsername.isBlank()
            && sesWsPasswordEncrypted != null && !sesWsPasswordEncrypted.isBlank();
    }

    public void updateUsername(String username) {
        this.username = username;
        touch();
    }

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        touch();
    }

    public void registerSesConnectionTest(
        SesConnectionTestStatus status,
        OffsetDateTime testedAt,
        String ownerMessage,
        String adminMessage,
        String endpoint,
        Integer httpStatus,
        String errorType,
        String rawDetail
    ) {
        this.sesConnectionTestStatus = status;
        this.sesConnectionTestedAt = testedAt;
        this.sesConnectionOwnerMessage = ownerMessage;
        this.sesConnectionAdminMessage = adminMessage;
        this.sesConnectionTestEndpoint = endpoint;
        this.sesConnectionTestHttpStatus = httpStatus;
        this.sesConnectionTestErrorType = errorType;
        this.sesConnectionTestRawDetail = rawDetail;
        touch();
    }

    public void clearSesConnectionTest() {
        this.sesConnectionTestStatus = null;
        this.sesConnectionTestedAt = null;
        this.sesConnectionOwnerMessage = null;
        this.sesConnectionAdminMessage = null;
        this.sesConnectionTestEndpoint = null;
        this.sesConnectionTestHttpStatus = null;
        this.sesConnectionTestErrorType = null;
        this.sesConnectionTestRawDetail = null;
    }

    private void touch() {
        this.updatedAt = OffsetDateTime.now();
    }
}
