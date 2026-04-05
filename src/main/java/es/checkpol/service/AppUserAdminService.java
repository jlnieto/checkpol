package es.checkpol.service;

import es.checkpol.domain.AppUser;
import es.checkpol.domain.AppUserRole;
import es.checkpol.repository.AppUserRepository;
import es.checkpol.web.AdminUserForm;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AppUserAdminService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppUserSesService appUserSesService;

    public AppUserAdminService(
        AppUserRepository appUserRepository,
        PasswordEncoder passwordEncoder,
        AppUserSesService appUserSesService
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.appUserSesService = appUserSesService;
    }

    @Transactional(readOnly = true)
    public List<AdminUserSummary> findAllUsers() {
        return appUserRepository.findAllByOrderByRoleAscDisplayNameAscUsernameAsc().stream()
            .map(user -> new AdminUserSummary(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.isActive()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public long countOwners() {
        return appUserRepository.countByRole(AppUserRole.OWNER);
    }

    @Transactional
    public AppUser createOwner(AdminUserForm form) {
        validateCreate(form);
        if (appUserRepository.existsByUsername(form.username().trim())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese nombre.");
        }
        OffsetDateTime now = OffsetDateTime.now();
        AppUser owner = new AppUser(
            form.username().trim(),
            passwordEncoder.encode(form.password().trim()),
            form.displayName().trim(),
            AppUserRole.OWNER,
            form.active(),
            now,
            now
        );
        appUserSesService.updateSesConfiguration(owner, form.sesArrendadorCode(), form.sesWsUsername(), form.sesWsPassword());
        return appUserRepository.save(owner);
    }

    @Transactional(readOnly = true)
    public AdminUserForm getOwnerForm(Long userId) {
        AppUser user = getOwner(userId);
        return new AdminUserForm(
            user.getUsername(),
            user.getDisplayName(),
            "",
            user.getSesArrendadorCode() == null ? "" : user.getSesArrendadorCode(),
            user.getSesWsUsername() == null ? "" : user.getSesWsUsername(),
            "",
            user.isActive()
        );
    }

    @Transactional(readOnly = true)
    public boolean ownerHasStoredSesPassword(Long userId) {
        AppUser user = getOwner(userId);
        return user.getSesWsPasswordEncrypted() != null && !user.getSesWsPasswordEncrypted().isBlank();
    }

    @Transactional(readOnly = true)
    public AppUser getOwnerEntity(Long userId) {
        return getOwner(userId);
    }

    @Transactional
    public AppUser updateOwner(Long userId, AdminUserForm form) {
        validateUpdate(form);
        AppUser user = getOwner(userId);
        String newUsername = form.username().trim();
        if (!user.getUsername().equals(newUsername) && appUserRepository.existsByUsername(newUsername)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese nombre.");
        }
        user.updateUsername(newUsername);
        user.updateOwner(form.displayName().trim(), form.active());
        appUserSesService.updateSesConfiguration(user, form.sesArrendadorCode(), form.sesWsUsername(), form.sesWsPassword());
        if (form.password() != null && !form.password().isBlank()) {
            user.updatePasswordHash(passwordEncoder.encode(form.password().trim()));
        }
        return user;
    }

    @Transactional
    public SesConnectionTestResult testOwnerSesConnection(Long userId) {
        return appUserSesService.testAndStore(getOwner(userId));
    }

    private AppUser getOwner(Long userId) {
        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado ese usuario."));
        if (user.getRole() != AppUserRole.OWNER) {
            throw new IllegalArgumentException("Solo puedes editar usuarios propietarios.");
        }
        return user;
    }

    private void validateCreate(AdminUserForm form) {
        validateCommon(form);
        if (form.password() == null || form.password().isBlank()) {
            throw new IllegalArgumentException("Escribe una contraseña inicial.");
        }
    }

    private void validateUpdate(AdminUserForm form) {
        validateCommon(form);
    }

    private void validateCommon(AdminUserForm form) {
        if (form.username() == null || form.username().isBlank()) {
            throw new IllegalArgumentException("Escribe un nombre de usuario.");
        }
        if (form.displayName() == null || form.displayName().isBlank()) {
            throw new IllegalArgumentException("Escribe un nombre visible.");
        }
    }

}
