package es.checkpol.service;

import es.checkpol.domain.AppUser;
import org.springframework.stereotype.Service;

@Service
public class AppUserSesService {

    private final SesCredentialCipher sesCredentialCipher;
    private final SesConnectionTestingService sesConnectionTestingService;

    public AppUserSesService(
        SesCredentialCipher sesCredentialCipher,
        SesConnectionTestingService sesConnectionTestingService
    ) {
        this.sesCredentialCipher = sesCredentialCipher;
        this.sesConnectionTestingService = sesConnectionTestingService;
    }

    public void updateSesConfiguration(AppUser user, String sesArrendadorCode, String sesWsUsername, String sesWsPassword) {
        String arrendadorCode = normalizeOptional(sesArrendadorCode);
        String wsUsername = normalizeOptional(sesWsUsername);
        String wsPassword = normalizeOptional(sesWsPassword);

        if (arrendadorCode == null && wsUsername == null && wsPassword == null) {
            user.clearSesWsConfiguration();
            return;
        }

        user.updateSesWsConfiguration(
            arrendadorCode,
            wsUsername,
            resolveEncryptedSesPassword(wsPassword, user.getSesWsPasswordEncrypted())
        );
    }

    public SesConnectionTestResult testAndStore(AppUser user) {
        return sesConnectionTestingService.testAndStore(user);
    }

    private String resolveEncryptedSesPassword(String wsPassword, String currentEncryptedPassword) {
        if (wsPassword != null) {
            return sesCredentialCipher.encrypt(wsPassword);
        }
        if (currentEncryptedPassword != null && !currentEncryptedPassword.isBlank()) {
            return currentEncryptedPassword;
        }
        return null;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
