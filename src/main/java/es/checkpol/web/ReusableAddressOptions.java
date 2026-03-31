package es.checkpol.web;

import es.checkpol.domain.Guest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ReusableAddressOptions {

    private ReusableAddressOptions() {
    }

    public static List<ReusableAddressOption> fromGuests(List<Guest> guests) {
        Map<String, ReusableAddressOption> options = new LinkedHashMap<>();

        for (Guest guest : guests) {
            if (!hasReusableAddress(guest)) {
                continue;
            }

            ReusableAddressOption option = new ReusableAddressOption(
                guest.getAddressLine().trim(),
                normalize(guest.getAddressComplement()),
                normalize(guest.getMunicipalityCode()),
                municipalityName(guest),
                guest.getPostalCode().trim(),
                guest.getCountry().trim().toUpperCase(Locale.ROOT)
            );
            options.putIfAbsent(key(option), option);
        }

        return List.copyOf(options.values());
    }

    private static boolean hasReusableAddress(Guest guest) {
        return hasText(guest.getAddressLine())
            && hasText(guest.getPostalCode())
            && hasText(guest.getCountry())
            && hasText(guest.getMunicipalityName());
    }

    private static String municipalityName(Guest guest) {
        return guest.getMunicipalityName().trim();
    }

    private static String key(ReusableAddressOption option) {
        return normalizeKey(option.addressLine())
            + "|" + normalizeKey(option.addressComplement())
            + "|" + normalizeKey(option.municipalityCode())
            + "|" + normalizeKey(option.municipalityName())
            + "|" + normalizeKey(option.postalCode())
            + "|" + normalizeKey(option.country());
    }

    private static String normalizeKey(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
