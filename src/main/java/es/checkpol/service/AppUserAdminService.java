package es.checkpol.service;

import es.checkpol.domain.AppUser;
import es.checkpol.domain.AppUserRole;
import es.checkpol.repository.AppUserRepository;
import es.checkpol.web.AdminPasswordForm;
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
        return createUser(new AdminUserForm(
            form.username(),
            form.displayName(),
            AppUserRole.OWNER,
            form.password(),
            form.sesArrendadorCode(),
            form.sesWsUsername(),
            form.sesWsPassword(),
            form.active()
        ));
    }

    @Transactional
    public AppUser createUser(AdminUserForm form) {
        validateCreate(form);
        if (appUserRepository.existsByUsername(form.username().trim())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese nombre.");
        }
        AppUserRole role = requireRole(form.role());
        OffsetDateTime now = OffsetDateTime.now();
        AppUser user = new AppUser(
            form.username().trim(),
            passwordEncoder.encode(form.password().trim()),
            form.displayName().trim(),
            role,
            form.active(),
            now,
            now
        );
        if (role == AppUserRole.OWNER) {
            appUserSesService.updateSesConfiguration(user, form.sesArrendadorCode(), form.sesWsUsername(), form.sesWsPassword());
        }
        return appUserRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AdminUserForm getOwnerForm(Long userId) {
        return getUserForm(getOwner(userId));
    }

    @Transactional(readOnly = true)
    public AdminUserForm getUserForm(Long userId) {
        return getUserForm(getUser(userId));
    }

    private AdminUserForm getUserForm(AppUser user) {
        return new AdminUserForm(
            user.getUsername(),
            user.getDisplayName(),
            user.getRole(),
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

    @Transactional(readOnly = true)
    public AppUser getUserEntity(Long userId) {
        return getUser(userId);
    }

    @Transactional
    public AppUser updateOwner(Long userId, AdminUserForm form) {
        getOwner(userId);
        return updateUser(userId, new AdminUserForm(
            form.username(),
            form.displayName(),
            AppUserRole.OWNER,
            form.password(),
            form.sesArrendadorCode(),
            form.sesWsUsername(),
            form.sesWsPassword(),
            form.active()
        ));
    }

    @Transactional
    public AppUser updateUser(Long userId, AdminUserForm form) {
        validateUpdate(form);
        AppUser user = getUser(userId);
        String newUsername = form.username().trim();
        if (!user.getUsername().equals(newUsername) && appUserRepository.existsByUsername(newUsername)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese nombre.");
        }
        ensureSuperAdminCanBeDeactivated(user, form.active());
        user.updateUsername(newUsername);
        user.updateProfile(form.displayName().trim(), form.active());
        if (user.getRole() == AppUserRole.OWNER) {
            appUserSesService.updateSesConfiguration(user, form.sesArrendadorCode(), form.sesWsUsername(), form.sesWsPassword());
        } else {
            user.clearSesWsConfiguration();
        }
        if (form.password() != null && !form.password().isBlank()) {
            user.updatePasswordHash(passwordEncoder.encode(form.password().trim()));
        }
        return user;
    }

    @Transactional
    public AppUser changePassword(Long userId, AdminPasswordForm form, Long currentAdminId, String currentAdminUsername) {
        AppUser user = getUser(userId);
        validatePasswordForm(form, isCurrentUser(user, currentAdminId, currentAdminUsername));
        if (isCurrentUser(user, currentAdminId, currentAdminUsername) && !passwordEncoder.matches(form.currentPassword().trim(), user.getPasswordHash())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }
        user.updatePasswordHash(passwordEncoder.encode(form.newPassword().trim()));
        return user;
    }

    @Transactional
    public SesConnectionTestResult testOwnerSesConnection(Long userId) {
        return appUserSesService.testAndStore(getOwner(userId));
    }

    private AppUser getOwner(Long userId) {
        AppUser user = getUser(userId);
        if (user.getRole() != AppUserRole.OWNER) {
            throw new IllegalArgumentException("Solo puedes editar usuarios propietarios.");
        }
        return user;
    }

    private AppUser getUser(Long userId) {
        return appUserRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado ese usuario."));
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
        requireRole(form.role());
    }

    private AppUserRole requireRole(AppUserRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Selecciona el tipo de usuario.");
        }
        return role;
    }

    private void ensureSuperAdminCanBeDeactivated(AppUser user, boolean requestedActive) {
        if (user.getRole() != AppUserRole.SUPER_ADMIN || requestedActive || !user.isActive()) {
            return;
        }
        if (appUserRepository.countByRoleAndActiveTrue(AppUserRole.SUPER_ADMIN) <= 1) {
            throw new IllegalArgumentException("No puedes desactivar el último superadmin activo.");
        }
    }

    private boolean isCurrentUser(AppUser user, Long currentAdminId, String currentAdminUsername) {
        return (currentAdminId != null && currentAdminId.equals(user.getId()))
            || (currentAdminUsername != null && currentAdminUsername.equals(user.getUsername()));
    }

    private void validatePasswordForm(AdminPasswordForm form, boolean currentUser) {
        if (currentUser && (form.currentPassword() == null || form.currentPassword().isBlank())) {
            throw new IllegalArgumentException("Escribe tu contraseña actual.");
        }
        if (form.newPassword() == null || form.newPassword().isBlank()) {
            throw new IllegalArgumentException("Escribe la nueva contraseña.");
        }
        if (form.confirmPassword() == null || form.confirmPassword().isBlank()) {
            throw new IllegalArgumentException("Confirma la nueva contraseña.");
        }
        if (!form.newPassword().equals(form.confirmPassword())) {
            throw new IllegalArgumentException("La nueva contraseña y la confirmación no coinciden.");
        }
    }

}
