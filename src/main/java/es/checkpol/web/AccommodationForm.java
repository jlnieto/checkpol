package es.checkpol.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AccommodationForm(
    @NotBlank(message = "Indica un nombre para la vivienda.")
    @Size(max = 120, message = "El nombre no puede superar los 120 caracteres.")
    String name,

    String sesEstablishmentCode,

    @Size(max = 40, message = "El numero de registro no puede superar los 40 caracteres.")
    String registrationNumber,

    @Min(value = 1, message = "Indica al menos 1 habitacion.")
    @Max(value = 50, message = "El numero de habitaciones no puede superar 50.")
    Integer roomCount
) {

    public AccommodationForm() {
        this("", "", "", null);
    }
}
