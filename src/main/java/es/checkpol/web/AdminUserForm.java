package es.checkpol.web;

import es.checkpol.domain.AppUserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUserForm(
    @NotBlank(message = "Escribe un nombre de usuario.")
    @Size(max = 80, message = "El usuario no puede superar los 80 caracteres.")
    String username,

    @NotBlank(message = "Escribe un nombre visible.")
    @Size(max = 120, message = "El nombre visible no puede superar los 120 caracteres.")
    String displayName,

    @NotNull(message = "Selecciona el tipo de usuario.")
    AppUserRole role,

    @Size(max = 120, message = "La contraseña no puede superar los 120 caracteres.")
    String password,

    @Size(max = 10, message = "El código de arrendador no puede superar los 10 caracteres.")
    String sesArrendadorCode,

    @Size(max = 50, message = "El usuario SES no puede superar los 50 caracteres.")
    String sesWsUsername,

    @Size(max = 120, message = "La clave SES no puede superar los 120 caracteres.")
    String sesWsPassword,

    boolean active
) {

    public AdminUserForm() {
        this("", "", AppUserRole.OWNER, "", "", "", "", true);
    }

    public AdminUserForm(
        String username,
        String displayName,
        String password,
        String sesArrendadorCode,
        String sesWsUsername,
        String sesWsPassword,
        boolean active
    ) {
        this(username, displayName, AppUserRole.OWNER, password, sesArrendadorCode, sesWsUsername, sesWsPassword, active);
    }
}
