package es.checkpol.service;

import es.checkpol.domain.AppUser;
import es.checkpol.domain.AppUserRole;
import es.checkpol.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;

@Configuration
public class AppUserBootstrapService {

    @Bean
    ApplicationRunner bootstrapSuperAdmin(
        AppUserRepository appUserRepository,
        PasswordEncoder passwordEncoder,
        @Value("${checkpol.security.bootstrap-admin.username}") String username,
        @Value("${checkpol.security.bootstrap-admin.password}") String password,
        @Value("${checkpol.security.bootstrap-admin.display-name}") String displayName
    ) {
        return args -> {
            if (appUserRepository.countByRole(AppUserRole.SUPER_ADMIN) > 0) {
                return;
            }

            appUserRepository.findByUsername(username).ifPresent(existing -> {
                throw new IllegalStateException("El usuario bootstrap del super admin ya existe con un rol incompatible.");
            });

            OffsetDateTime now = OffsetDateTime.now();
            appUserRepository.save(new AppUser(
                username.trim(),
                passwordEncoder.encode(password),
                displayName.trim(),
                AppUserRole.SUPER_ADMIN,
                true,
                now,
                now
            ));
        };
    }
}
