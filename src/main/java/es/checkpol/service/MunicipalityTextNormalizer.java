package es.checkpol.service;

import java.text.Normalizer;
import java.util.Locale;

public final class MunicipalityTextNormalizer {

    private MunicipalityTextNormalizer() {
    }

    public static String normalizeCountry(String countryCode) {
        return countryCode == null ? "" : countryCode.trim().toUpperCase(Locale.ROOT);
    }

    public static String normalizeText(String value) {
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

    public static String postalCodePrefix(String postalCode) {
        if (postalCode == null || postalCode.length() < 2) {
            return null;
        }
        return postalCode.substring(0, 2);
    }
}
