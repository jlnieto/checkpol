package es.checkpol.web;

import jakarta.servlet.RequestDispatcher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class AccessDeniedController {

    @RequestMapping("/access-denied")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String accessDenied(Authentication authentication, jakarta.servlet.http.HttpServletRequest request, Model model) {
        AccessDeniedContent content = buildContent(authentication, forwardedPath(request));
        model.addAttribute("title", content.title());
        model.addAttribute("detail", content.detail());
        model.addAttribute("actionLabel", content.actionLabel());
        model.addAttribute("actionUrl", content.actionUrl());
        model.addAttribute("showLogout", authentication != null && authentication.isAuthenticated());
        return "access-denied";
    }

    private AccessDeniedContent buildContent(Authentication authentication, String requestedPath) {
        if (hasAuthority(authentication, "ROLE_OWNER")) {
            return new AccessDeniedContent(
                "Página no disponible",
                "La página solicitada no está disponible. Puedes seguir trabajando con tus estancias, huéspedes y XML desde el área privada.",
                "Volver a estancias",
                "/bookings"
            );
        }
        if (hasAuthority(authentication, "ROLE_SUPER_ADMIN")) {
            return new AccessDeniedContent(
                "Esta pantalla no pertenece al área admin",
                "La ruta " + requestedPath + " está reservada para cuentas owner. Desde administración puedes seguir gestionando usuarios y catálogo oficial.",
                "Volver al panel admin",
                "/admin"
            );
        }
        return new AccessDeniedContent(
            "No tienes acceso a esta pantalla",
            "La ruta " + requestedPath + " no está disponible para la sesión actual.",
            "Volver al acceso",
            "/login"
        );
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null
            && authentication.getAuthorities().stream().anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }

    private String forwardedPath(jakarta.servlet.http.HttpServletRequest request) {
        Object forwardedPath = request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
        if (forwardedPath instanceof String path && !path.isBlank()) {
            return path;
        }
        return "/";
    }

    private record AccessDeniedContent(
        String title,
        String detail,
        String actionLabel,
        String actionUrl
    ) {
    }
}
