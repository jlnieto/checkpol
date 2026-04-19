package es.checkpol.web;

import org.springframework.stereotype.Controller;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return "public/home";
        }
        if (authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()))) {
            return "redirect:/admin";
        }
        return "redirect:/bookings";
    }
}
