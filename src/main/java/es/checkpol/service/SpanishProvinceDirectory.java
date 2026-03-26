package es.checkpol.service;

import java.util.Map;

final class SpanishProvinceDirectory {

    private static final Map<String, ProvinceInfo> PROVINCES = Map.ofEntries(
        Map.entry("01", new ProvinceInfo("01", "Alava")),
        Map.entry("02", new ProvinceInfo("02", "Albacete")),
        Map.entry("03", new ProvinceInfo("03", "Alicante")),
        Map.entry("04", new ProvinceInfo("04", "Almeria")),
        Map.entry("05", new ProvinceInfo("05", "Avila")),
        Map.entry("06", new ProvinceInfo("06", "Badajoz")),
        Map.entry("07", new ProvinceInfo("07", "Illes Balears")),
        Map.entry("08", new ProvinceInfo("08", "Barcelona")),
        Map.entry("09", new ProvinceInfo("09", "Burgos")),
        Map.entry("10", new ProvinceInfo("10", "Caceres")),
        Map.entry("11", new ProvinceInfo("11", "Cadiz")),
        Map.entry("12", new ProvinceInfo("12", "Castellon")),
        Map.entry("13", new ProvinceInfo("13", "Ciudad Real")),
        Map.entry("14", new ProvinceInfo("14", "Cordoba")),
        Map.entry("15", new ProvinceInfo("15", "A Coruna")),
        Map.entry("16", new ProvinceInfo("16", "Cuenca")),
        Map.entry("17", new ProvinceInfo("17", "Girona")),
        Map.entry("18", new ProvinceInfo("18", "Granada")),
        Map.entry("19", new ProvinceInfo("19", "Guadalajara")),
        Map.entry("20", new ProvinceInfo("20", "Guipuzcoa")),
        Map.entry("21", new ProvinceInfo("21", "Huelva")),
        Map.entry("22", new ProvinceInfo("22", "Huesca")),
        Map.entry("23", new ProvinceInfo("23", "Jaen")),
        Map.entry("24", new ProvinceInfo("24", "Leon")),
        Map.entry("25", new ProvinceInfo("25", "Lleida")),
        Map.entry("26", new ProvinceInfo("26", "La Rioja")),
        Map.entry("27", new ProvinceInfo("27", "Lugo")),
        Map.entry("28", new ProvinceInfo("28", "Madrid")),
        Map.entry("29", new ProvinceInfo("29", "Malaga")),
        Map.entry("30", new ProvinceInfo("30", "Murcia")),
        Map.entry("31", new ProvinceInfo("31", "Navarra")),
        Map.entry("32", new ProvinceInfo("32", "Ourense")),
        Map.entry("33", new ProvinceInfo("33", "Asturias")),
        Map.entry("34", new ProvinceInfo("34", "Palencia")),
        Map.entry("35", new ProvinceInfo("35", "Las Palmas")),
        Map.entry("36", new ProvinceInfo("36", "Pontevedra")),
        Map.entry("37", new ProvinceInfo("37", "Salamanca")),
        Map.entry("38", new ProvinceInfo("38", "Santa Cruz de Tenerife")),
        Map.entry("39", new ProvinceInfo("39", "Cantabria")),
        Map.entry("40", new ProvinceInfo("40", "Segovia")),
        Map.entry("41", new ProvinceInfo("41", "Sevilla")),
        Map.entry("42", new ProvinceInfo("42", "Soria")),
        Map.entry("43", new ProvinceInfo("43", "Tarragona")),
        Map.entry("44", new ProvinceInfo("44", "Teruel")),
        Map.entry("45", new ProvinceInfo("45", "Toledo")),
        Map.entry("46", new ProvinceInfo("46", "Valencia")),
        Map.entry("47", new ProvinceInfo("47", "Valladolid")),
        Map.entry("48", new ProvinceInfo("48", "Vizcaya")),
        Map.entry("49", new ProvinceInfo("49", "Zamora")),
        Map.entry("50", new ProvinceInfo("50", "Zaragoza")),
        Map.entry("51", new ProvinceInfo("51", "Ceuta")),
        Map.entry("52", new ProvinceInfo("52", "Melilla"))
    );

    private SpanishProvinceDirectory() {
    }

    static ProvinceInfo findByPostalCode(String postalCode) {
        if (postalCode == null || postalCode.length() < 2) {
            return null;
        }
        return PROVINCES.get(postalCode.substring(0, 2));
    }

    static ProvinceInfo findByPrefix(String postalCodePrefix) {
        if (postalCodePrefix == null || postalCodePrefix.length() != 2) {
            return null;
        }
        return PROVINCES.get(postalCodePrefix);
    }

    record ProvinceInfo(String code, String queryName) {
    }
}
