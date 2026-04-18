package es.checkpol.service;

import es.checkpol.domain.AppUser;
import es.checkpol.domain.AppUserRole;
import es.checkpol.repository.AppUserRepository;
import es.checkpol.web.AdminPasswordForm;
import es.checkpol.web.AdminUserForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserAdminServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private AppUserSesService appUserSesService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AppUserAdminService service;

    @BeforeEach
    void setUp() {
        service = new AppUserAdminService(appUserRepository, passwordEncoder, appUserSesService);
    }

    @Test
    void createsSuperAdminWithoutSesConfiguration() {
        when(appUserRepository.existsByUsername("ana")).thenReturn(false);
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser user = service.createUser(new AdminUserForm(
            "ana",
            "Ana Admin",
            AppUserRole.SUPER_ADMIN,
            "secreta",
            "SES",
            "ws-user",
            "ws-pass",
            true
        ));

        assertEquals(AppUserRole.SUPER_ADMIN, user.getRole());
        assertEquals("ana", user.getUsername());
        assertTrue(passwordEncoder.matches("secreta", user.getPasswordHash()));
        verifyNoInteractions(appUserSesService);
    }

    @Test
    void requiresCurrentPasswordWhenChangingOwnPassword() {
        AppUser admin = superAdmin(1L, "admin", "actual", true);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(admin));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            service.changePassword(1L, new AdminPasswordForm("", "nueva", "nueva"), 1L, "admin")
        );

        assertEquals("Escribe tu contraseña actual.", exception.getMessage());
    }

    @Test
    void rejectsWrongCurrentPasswordWhenChangingOwnPassword() {
        AppUser admin = superAdmin(1L, "admin", "actual", true);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(admin));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            service.changePassword(1L, new AdminPasswordForm("incorrecta", "nueva", "nueva"), 1L, "admin")
        );

        assertEquals("La contraseña actual no es correcta.", exception.getMessage());
    }

    @Test
    void changesAnotherUsersPasswordWithoutPreviousPassword() {
        AppUser owner = owner(2L, "joana", "anterior", true);
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(owner));

        service.changePassword(2L, new AdminPasswordForm("", "nueva", "nueva"), 1L, "admin");

        assertTrue(passwordEncoder.matches("nueva", owner.getPasswordHash()));
    }

    @Test
    void blocksDeactivatingLastActiveSuperAdmin() {
        AppUser admin = superAdmin(1L, "admin", "actual", true);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(appUserRepository.countByRoleAndActiveTrue(AppUserRole.SUPER_ADMIN)).thenReturn(1L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            service.updateUser(1L, new AdminUserForm(
                "admin",
                "Administrador",
                AppUserRole.SUPER_ADMIN,
                "",
                "",
                "",
                "",
                false
            ))
        );

        assertEquals("No puedes desactivar el último superadmin activo.", exception.getMessage());
    }

    private AppUser superAdmin(Long id, String username, String rawPassword, boolean active) {
        return user(id, username, rawPassword, AppUserRole.SUPER_ADMIN, active);
    }

    private AppUser owner(Long id, String username, String rawPassword, boolean active) {
        return user(id, username, rawPassword, AppUserRole.OWNER, active);
    }

    private AppUser user(Long id, String username, String rawPassword, AppUserRole role, boolean active) {
        OffsetDateTime now = OffsetDateTime.parse("2026-04-04T10:00:00Z");
        AppUser user = new AppUser(username, passwordEncoder.encode(rawPassword), username, role, active, now, now);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
