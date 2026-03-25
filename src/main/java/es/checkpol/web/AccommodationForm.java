package es.checkpol.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccommodationForm(
    @NotBlank(message = "Indica un nombre para la vivienda.")
    @Size(max = 120, message = "El nombre no puede superar los 120 caracteres.")
    String name,

    @NotBlank(message = "Indica el codigo SES de la vivienda.")
    @Size(max = 10, message = "El codigo SES no puede superar los 10 caracteres.")
    String sesEstablishmentCode,

    @Size(max = 40, message = "El numero de registro no puede superar los 40 caracteres.")
    String registrationNumber
) {

    public AccommodationForm() {
        this("", "", "");
    }
}
