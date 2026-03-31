package es.checkpol.service;

import es.checkpol.domain.AppUser;
import es.checkpol.repository.AppUserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentAppUserService {

    private final AppUserRepository appUserRepository;

    public CurrentAppUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public AppUserPrincipal requireAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("No hay una sesión autenticada.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUserPrincipal appUserPrincipal) {
            return appUserPrincipal;
        }
        throw new IllegalStateException("La sesión actual no pertenece a un usuario persistido.");
    }

    public Long requireCurrentUserId() {
        return requireAuthenticatedUser().getId();
    }

    public AppUser requireCurrentUserEntity() {
        AppUserPrincipal principal = requireAuthenticatedUser();
        return appUserRepository.findById(principal.getId())
            .orElseThrow(() -> new IllegalStateException("No he encontrado el usuario autenticado en la base de datos."));
    }
}
