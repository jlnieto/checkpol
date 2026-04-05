package es.checkpol.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressForm(
    @Size(max = 5, message = "El código de municipio no puede superar los 5 caracteres.")
    String municipalityCode,

    @NotBlank(message = "Indica la dirección.")
    @Size(max = 100, message = "La dirección no puede superar los 100 caracteres.")
    String addressLine,

    @Size(max = 100, message = "El complemento no puede superar los 100 caracteres.")
    String addressComplement,

    @NotBlank(message = "Indica el código postal.")
    @Size(max = 12, message = "El código postal no puede superar los 12 caracteres.")
    String postalCode,

    @Size(max = 80, message = "El municipio no puede superar los 80 caracteres.")
    String municipalityName,

    @NotBlank(message = "Indica el país con 3 letras, por ejemplo ESP.")
    @Size(min = 3, max = 3, message = "El país debe tener 3 letras.")
    String country
) {

    public AddressForm() {
        this("", "", "", "", "", "ESP");
    }
}
