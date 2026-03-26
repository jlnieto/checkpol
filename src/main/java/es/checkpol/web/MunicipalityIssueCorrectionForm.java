package es.checkpol.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MunicipalityIssueCorrectionForm(
    @NotBlank(message = "Indica el codigo de municipio.")
    @Pattern(regexp = "^\\d{5}$", message = "El codigo de municipio debe tener 5 numeros.")
    String municipalityCode,

    @NotBlank(message = "Indica el nombre del municipio.")
    @Size(max = 80, message = "El nombre del municipio no puede superar los 80 caracteres.")
    String municipalityName,

    @Size(max = 255, message = "La nota no puede superar los 255 caracteres.")
    String resolutionNote
) {

    public MunicipalityIssueCorrectionForm() {
        this("", "", "");
    }
}
