package es.checkpol.web;

import jakarta.validation.constraints.NotBlank;

public record AdminCatalogDefaultsForm(
    @NotBlank(message = "Indica el origen por defecto.")
    String source,
    @NotBlank(message = "Indica la URL por defecto del fichero oficial de municipios.")
    String municipalitiesUrl,
    @NotBlank(message = "Indica la URL por defecto del callejero oficial o del CSV postal equivalente.")
    String postalMappingsUrl
) {
}
