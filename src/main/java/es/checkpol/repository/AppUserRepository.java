package es.checkpol.repository;

import es.checkpol.domain.AppUser;
import es.checkpol.domain.AppUserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByUsernameAndActiveTrue(String username);

    boolean existsByUsername(String username);

    long countByRole(AppUserRole role);

    List<AppUser> findAllByOrderByRoleAscDisplayNameAscUsernameAsc();

    List<AppUser> findAllByRoleOrderByDisplayNameAscUsernameAsc(AppUserRole role);
}
