package es.checkpol.web;

import es.checkpol.service.MunicipalityCatalogService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MunicipalityCatalogController {

    private final MunicipalityCatalogService municipalityCatalogService;

    public MunicipalityCatalogController(MunicipalityCatalogService municipalityCatalogService) {
        this.municipalityCatalogService = municipalityCatalogService;
    }

    @GetMapping(value = "/municipality-catalog/spanish-municipalities", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<MunicipalityOptionResponse> listSpanishMunicipalitiesByPostalCode(@RequestParam("postalCode") String postalCode) {
        return municipalityCatalogService.findSpanishOptionsByPostalCode(postalCode).stream()
            .map(option -> new MunicipalityOptionResponse(
                option.municipalityCode(),
                option.municipalityName(),
                option.provinceCode(),
                option.provinceName()
            ))
            .toList();
    }

    public record MunicipalityOptionResponse(
        String municipalityCode,
        String municipalityName,
        String provinceCode,
        String provinceName
    ) {
    }
}
