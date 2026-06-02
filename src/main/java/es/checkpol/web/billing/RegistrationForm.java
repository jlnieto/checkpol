package es.checkpol.web.billing;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrationForm(
    @NotBlank(message = "Escribe tu email.")
    @Email(message = "Escribe un email válido.")
    @Size(max = 160, message = "El email es demasiado largo.")
    String email,

    @NotBlank(message = "Escribe una contraseña.")
    @Size(min = 8, max = 80, message = "La contraseña debe tener entre 8 y 80 caracteres.")
    String password,

    @NotNull(message = "Indica cuántas viviendas quieres contratar.")
    @Min(value = 1, message = "Contrata al menos una vivienda.")
    @Max(value = 50, message = "Para más de 50 viviendas, escríbenos.")
    Integer accommodationQuantity
) {

    public RegistrationForm() {
        this("", "", 1);
    }
}
