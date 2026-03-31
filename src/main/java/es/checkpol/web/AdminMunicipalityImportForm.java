package es.checkpol.web;

import jakarta.validation.constraints.NotBlank;

public record AdminMunicipalityImportForm(
    @NotBlank(message = "Indica la URL del fichero oficial de municipios.")
    String municipalitiesUrl,
    @NotBlank(message = "Indica la URL del callejero oficial o del CSV postal equivalente.")
    String postalMappingsUrl,
    @NotBlank(message = "Indica el origen de la carga.")
    String source,
    @NotBlank(message = "Indica una versión trazable.")
    String sourceVersion
) {
}
