package es.checkpol.domain;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum GuestRelationship {
    AB("Abuelo/a"),
    BA("Bisabuelo/a"),
    BN("Bisnieto/a"),
    CY("Conyuge"),
    CD("Cunado/a"),
    HR("Hermano/a"),
    HJ("Hijo/a"),
    PM("Padre o madre"),
    NI("Nieto/a"),
    SB("Sobrino/a"),
    SG("Suegro/a"),
    TI("Tio/a"),
    YN("Yerno o nuera"),
    TU("Tutor/a"),
    OT("Otro");

    private static final Set<String> CODES = Arrays.stream(values())
        .map(GuestRelationship::name)
        .collect(Collectors.toUnmodifiableSet());

    private final String label;

    GuestRelationship(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static boolean isValidCode(String code) {
        return CODES.contains(code);
    }
}
