package es.checkpol.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUserForm(
    @NotBlank(message = "Escribe un nombre de usuario.")
    @Size(max = 80, message = "El usuario no puede superar los 80 caracteres.")
    String username,

    @NotBlank(message = "Escribe un nombre visible.")
    @Size(max = 120, message = "El nombre visible no puede superar los 120 caracteres.")
    String displayName,

    @Size(max = 120, message = "La contraseña no puede superar los 120 caracteres.")
    String password,

    boolean active
) {

    public AdminUserForm() {
        this("", "", "", true);
    }
}
