package es.checkpol.web;

import jakarta.validation.constraints.Size;

public record AdminPasswordForm(
    @Size(max = 120, message = "La contraseña actual no puede superar los 120 caracteres.")
    String currentPassword,

    @Size(max = 120, message = "La nueva contraseña no puede superar los 120 caracteres.")
    String newPassword,

    @Size(max = 120, message = "La confirmación no puede superar los 120 caracteres.")
    String confirmPassword
) {

    public AdminPasswordForm() {
        this("", "", "");
    }
}
