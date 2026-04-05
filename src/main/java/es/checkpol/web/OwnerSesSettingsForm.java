package es.checkpol.web;

import jakarta.validation.constraints.Size;

public record OwnerSesSettingsForm(
    @Size(max = 10, message = "El código de entidad no puede superar los 10 caracteres.")
    String sesArrendadorCode,

    @Size(max = 50, message = "El usuario SES no puede superar los 50 caracteres.")
    String sesWsUsername,

    @Size(max = 120, message = "La clave SES no puede superar los 120 caracteres.")
    String sesWsPassword
) {

    public OwnerSesSettingsForm() {
        this("", "", "");
    }
}
