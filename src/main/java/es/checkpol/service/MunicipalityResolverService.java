package es.checkpol.service;

import es.checkpol.domain.MunicipalityResolutionRule;
import es.checkpol.domain.MunicipalityResolutionStatus;
import es.checkpol.repository.MunicipalityResolutionRuleRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class MunicipalityResolverService {

    private static final String DEFAULT_FORCED_CODE = "28079";
    private static final String DEFAULT_FORCED_NAME = "Madrid";

    private final MunicipalityResolutionRuleRepository ruleRepository;
    private final MunicipalityLookupClient municipalityLookupClient;

    public MunicipalityResolverService(
        MunicipalityResolutionRuleRepository ruleRepository,
        MunicipalityLookupClient municipalityLookupClient
    ) {
        this.ruleRepository = ruleRepository;
        this.municipalityLookupClient = municipalityLookupClient;
    }

    public MunicipalityResolution resolve(String countryCode, String postalCode, String municipalityCode, String municipalityQuery) {
        String normalizedCountryCode = normalizeCountry(countryCode);
        String queryLabel = label(municipalityQuery);
        String queryNormalized = normalizeText(queryLabel);
        String postalCodePrefix = postalCodePrefix(postalCode);

        if (!"ESP".equals(normalizedCountryCode)) {
            return new MunicipalityResolution(
                blankToNull(municipalityCode),
                null,
                MunicipalityResolutionStatus.NOT_REQUIRED,
                null,
                queryNormalized,
                queryLabel,
                postalCodePrefix
            );
        }

        if (municipalityCode != null && municipalityCode.matches("\\d{5}")) {
            return new MunicipalityResolution(
                municipalityCode,
                queryLabel,
                MunicipalityResolutionStatus.EXACT,
                "Codigo de municipio informado directamente.",
                queryNormalized,
                queryLabel,
                postalCodePrefix
            );
        }

        Optional<MunicipalityResolutionRule> learnedRule = findLearnedRule(normalizedCountryCode, postalCodePrefix, queryNormalized);
        if (learnedRule.isPresent()) {
            MunicipalityResolutionRule rule = learnedRule.get();
            return new MunicipalityResolution(
                rule.getMunicipalityCode(),
                rule.getMunicipalityName(),
                MunicipalityResolutionStatus.LEARNED,
                "Codigo recuperado de una correccion ya aprendida.",
                queryNormalized,
                queryLabel,
                postalCodePrefix
            );
        }

        List<MunicipalityCandidate> rankedCandidates = rankCandidates(queryLabel, postalCodePrefix);
        if (!rankedCandidates.isEmpty()) {
            MunicipalityCandidate candidate = rankedCandidates.getFirst();
            MunicipalityResolutionStatus status = isExactMatch(candidate, queryNormalized, postalCodePrefix)
                ? MunicipalityResolutionStatus.EXACT
                : MunicipalityResolutionStatus.APPROXIMATED;
            String note = status == MunicipalityResolutionStatus.EXACT
                ? "Municipio resuelto automaticamente."
                : "Municipio aproximado automaticamente a partir del texto indicado.";
            return new MunicipalityResolution(
                candidate.municipalityCode(),
                candidate.municipalityName(),
                status,
                note,
                queryNormalized,
                queryLabel,
                postalCodePrefix
            );
        }

        MunicipalityCandidate provinceFallback = findProvinceFallback(postalCodePrefix);
        if (provinceFallback != null) {
            return new MunicipalityResolution(
                provinceFallback.municipalityCode(),
                provinceFallback.municipalityName(),
                MunicipalityResolutionStatus.PROVINCE_FALLBACK,
                "Sin coincidencia clara. Se ha asignado un municipio de la provincia para no bloquear el flujo.",
                queryNormalized,
                queryLabel,
                postalCodePrefix
            );
        }

        return new MunicipalityResolution(
            DEFAULT_FORCED_CODE,
            DEFAULT_FORCED_NAME,
            MunicipalityResolutionStatus.FORCED_FALLBACK,
            "No se ha podido resolver el municipio. Se ha asignado un codigo de fallback y queda pendiente de revision.",
            queryNormalized,
            queryLabel,
            postalCodePrefix
        );
    }

    private Optional<MunicipalityResolutionRule> findLearnedRule(String countryCode, String postalCodePrefix, String queryNormalized) {
        if (queryNormalized == null) {
            return Optional.empty();
        }
        if (postalCodePrefix != null) {
            Optional<MunicipalityResolutionRule> prefixed = ruleRepository
                .findFirstByCountryCodeAndPostalCodePrefixAndMunicipalityQueryNormalized(countryCode, postalCodePrefix, queryNormalized);
            if (prefixed.isPresent()) {
                return prefixed;
            }
        }
        return ruleRepository.findFirstByCountryCodeAndPostalCodePrefixIsNullAndMunicipalityQueryNormalized(countryCode, queryNormalized);
    }

    private List<MunicipalityCandidate> rankCandidates(String queryLabel, String postalCodePrefix) {
        if (queryLabel == null) {
            return List.of();
        }
        String queryNormalized = normalizeText(queryLabel);
        if (queryNormalized == null) {
            return List.of();
        }
        Map<String, RankedCandidate> uniqueByMunicipality = new LinkedHashMap<>();
        for (MunicipalityCandidate candidate : municipalityLookupClient.search(queryLabel)) {
            if (candidate.municipalityCode() == null || candidate.municipalityCode().isBlank()) {
                continue;
            }
            int score = candidateScore(candidate, queryNormalized, postalCodePrefix);
            RankedCandidate ranked = new RankedCandidate(candidate, score);
            RankedCandidate current = uniqueByMunicipality.get(candidate.municipalityCode());
            if (current == null || ranked.score() > current.score()) {
                uniqueByMunicipality.put(candidate.municipalityCode(), ranked);
            }
        }
        return uniqueByMunicipality.values().stream()
            .sorted(Comparator.comparingInt(RankedCandidate::score).reversed())
            .map(RankedCandidate::candidate)
            .toList();
    }

    private MunicipalityCandidate findProvinceFallback(String postalCodePrefix) {
        SpanishProvinceDirectory.ProvinceInfo province = SpanishProvinceDirectory.findByPrefix(postalCodePrefix);
        if (province == null) {
            return null;
        }
        return municipalityLookupClient.search(province.queryName()).stream()
            .filter(candidate -> candidate.municipalityCode() != null && !candidate.municipalityCode().isBlank())
            .filter(candidate -> province.code().equals(candidate.provinceCode()))
            .max(Comparator.comparingInt(candidate -> candidateScore(candidate, normalizeText(province.queryName()), province.code())))
            .orElse(null);
    }

    private boolean isExactMatch(MunicipalityCandidate candidate, String queryNormalized, String postalCodePrefix) {
        return normalizeText(candidate.municipalityName()).equals(queryNormalized)
            && (postalCodePrefix == null || postalCodePrefix.equals(candidate.provinceCode()));
    }

    private int candidateScore(MunicipalityCandidate candidate, String queryNormalized, String postalCodePrefix) {
        int score = 0;
        String candidateNormalized = normalizeText(candidate.municipalityName());
        if (postalCodePrefix != null && postalCodePrefix.equals(candidate.provinceCode())) {
            score += 100;
        }
        if (candidateNormalized.equals(queryNormalized)) {
            score += 90;
        } else if (candidateNormalized.startsWith(queryNormalized) || queryNormalized.startsWith(candidateNormalized)) {
            score += 45;
        } else if (candidateNormalized.contains(queryNormalized) || queryNormalized.contains(candidateNormalized)) {
            score += 25;
        }
        int distance = levenshtein(queryNormalized, candidateNormalized);
        score += Math.max(0, 40 - (distance * 8));
        if ("Municipio".equalsIgnoreCase(candidate.type())) {
            score += 30;
        } else if ("poblacion".equalsIgnoreCase(candidate.type())) {
            score += 20;
        } else {
            score += 5;
        }
        if (candidate.postalCode() != null && postalCodePrefix != null && candidate.postalCode().contains(postalCodePrefix)) {
            score += 10;
        }
        return score;
    }

    static String normalizeCountry(String countryCode) {
        return countryCode == null ? "" : countryCode.trim().toUpperCase(Locale.ROOT);
    }

    static String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .replaceAll("[^A-Za-z0-9 ]", " ")
            .toLowerCase(Locale.ROOT)
            .replaceAll("\\s+", " ")
            .trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String postalCodePrefix(String postalCode) {
        if (postalCode == null || postalCode.length() < 2) {
            return null;
        }
        return postalCode.substring(0, 2);
    }

    private static String label(String municipalityQuery) {
        String value = blankToNull(municipalityQuery);
        return value == null ? null : value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static int levenshtein(String left, String right) {
        if (left == null || right == null) {
            return Integer.MAX_VALUE / 4;
        }
        int[] costs = new int[right.length() + 1];
        for (int index = 0; index <= right.length(); index++) {
            costs[index] = index;
        }
        for (int row = 1; row <= left.length(); row++) {
            costs[0] = row;
            int northwest = row - 1;
            for (int column = 1; column <= right.length(); column++) {
                int north = costs[column];
                int west = costs[column - 1];
                int replacement = northwest + (left.charAt(row - 1) == right.charAt(column - 1) ? 0 : 1);
                costs[column] = Math.min(Math.min(north + 1, west + 1), replacement);
                northwest = north;
            }
        }
        return costs[right.length()];
    }

    private record RankedCandidate(MunicipalityCandidate candidate, int score) {
    }
}
