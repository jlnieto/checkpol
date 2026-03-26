package es.checkpol.infrastructure.municipality;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.checkpol.service.MunicipalityCandidate;
import es.checkpol.service.MunicipalityLookupClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class CartoCiudadMunicipalityLookupClient implements MunicipalityLookupClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public CartoCiudadMunicipalityLookupClient(
        RestClient.Builder restClientBuilder,
        ObjectMapper objectMapper,
        @Value("${checkpol.municipality.cartociudad-base-url:https://www.cartociudad.es/geocoder/api/geocoder}") String baseUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public List<MunicipalityCandidate> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        try {
            String response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/candidates").queryParam("q", query).build())
                .retrieve()
                .body(String.class);
            return parse(response);
        } catch (Exception exception) {
            return List.of();
        }
    }

    private List<MunicipalityCandidate> parse(String response) throws Exception {
        if (response == null || response.isBlank()) {
            return List.of();
        }
        JsonNode root = objectMapper.readTree(response);
        if (!root.isArray()) {
            return List.of();
        }
        List<MunicipalityCandidate> candidates = new ArrayList<>();
        for (JsonNode node : root) {
            candidates.add(new MunicipalityCandidate(
                text(node, "muniCode"),
                text(node, "muni"),
                text(node, "provinceCode"),
                text(node, "province"),
                text(node, "postalCode"),
                text(node, "type")
            ));
        }
        return candidates;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
