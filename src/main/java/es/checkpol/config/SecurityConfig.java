package es.checkpol.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationSuccessHandler authenticationSuccessHandler) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/guest-access/**", "/login", "/error", "/app.css", "/styles.css", "/css/**", "/wizard-form.js", "/address-form.js", "/municipality-catalog/**").permitAll()
                .requestMatchers("/admin/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/").authenticated()
                .requestMatchers("/bookings/**", "/accommodations/**", "/guests/**").hasRole("OWNER")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(authenticationSuccessHandler)
                .permitAll()
            )
            .logout(logout -> logout.logoutSuccessUrl("/login?logout"));

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> response.sendRedirect(defaultTargetUrl(authentication));
    }

    private String defaultTargetUrl(Authentication authentication) {
        if (authentication != null && authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()))) {
            return "/admin";
        }
        return "/bookings";
    }
}
