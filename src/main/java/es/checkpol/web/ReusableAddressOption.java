package es.checkpol.web;

public record ReusableAddressOption(
    String addressLine,
    String addressComplement,
    String municipalityCode,
    String municipalityName,
    String postalCode,
    String country
) {

    public String displayLine1() {
        if (addressComplement == null || addressComplement.isBlank()) {
            return addressLine;
        }
        return addressLine + ", " + addressComplement;
    }

    public String displayLine2() {
        return postalCode + " · " + municipalityName + " · " + country;
    }
}
