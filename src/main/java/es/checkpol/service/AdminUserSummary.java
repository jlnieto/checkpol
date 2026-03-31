package es.checkpol.service;

import es.checkpol.domain.AppUserRole;

public record AdminUserSummary(
    Long id,
    String username,
    String displayName,
    AppUserRole role,
    boolean active
) {
}
