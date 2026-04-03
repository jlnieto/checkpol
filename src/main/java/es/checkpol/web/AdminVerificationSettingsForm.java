package es.checkpol.web;

import jakarta.validation.constraints.NotBlank;

public record AdminVerificationSettingsForm(
    boolean enabled,
    @NotBlank(message = "Indica la expresión cron.")
    String cron,
    @NotBlank(message = "Indica la zona horaria.")
    String zone,
    @NotBlank(message = "Indica el usuario técnico.")
    String triggeredByUsername
) {
}
