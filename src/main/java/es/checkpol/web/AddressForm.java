package es.checkpol.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressForm(
    @NotBlank(message = "Indica la direccion.")
    @Size(max = 120, message = "La direccion no puede superar los 120 caracteres.")
    String addressLine,

    @Size(max = 120, message = "El complemento no puede superar los 120 caracteres.")
    String addressComplement,

    @NotBlank(message = "Indica el codigo postal.")
    @Size(max = 12, message = "El codigo postal no puede superar los 12 caracteres.")
    String postalCode,

    @NotBlank(message = "Indica el municipio o ciudad.")
    @Size(max = 80, message = "El municipio no puede superar los 80 caracteres.")
    String municipalityName,

    @NotBlank(message = "Indica el pais con 3 letras, por ejemplo ESP.")
    @Size(min = 3, max = 3, message = "El pais debe tener 3 letras.")
    String country
) {

    public AddressForm() {
        this("", "", "", "", "ESP");
    }
}
