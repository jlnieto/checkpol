package es.checkpol.web;

import es.checkpol.domain.GuestRelationship;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

public final class GuestFormOptions {

    private static final Locale SPANISH = Locale.forLanguageTag("es-ES");
    private static final List<CountryOption> COUNTRIES = buildCountries();

    private GuestFormOptions() {
    }

    public static List<CountryOption> countries() {
        return COUNTRIES;
    }

    public static GuestRelationship[] relationships() {
        return GuestRelationship.values();
    }

    private static List<CountryOption> buildCountries() {
        return Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2)
            .stream()
            .map(GuestFormOptions::toCountryOption)
            .filter(option -> option != null)
            .sorted(Comparator.comparing(CountryOption::label))
            .toList();
    }

    private static CountryOption toCountryOption(String alpha2) {
        try {
            Locale locale = new Locale.Builder().setRegion(alpha2).build();
            return new CountryOption(locale.getISO3Country(), locale.getDisplayCountry(SPANISH));
        } catch (MissingResourceException exception) {
            return null;
        }
    }
}
