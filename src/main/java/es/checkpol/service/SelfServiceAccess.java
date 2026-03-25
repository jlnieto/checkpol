package es.checkpol.service;

import java.time.OffsetDateTime;

public record SelfServiceAccess(
    String token,
    OffsetDateTime expiresAt
) {
}
