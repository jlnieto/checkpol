package es.checkpol.service;

import es.checkpol.domain.Booking;
import es.checkpol.domain.DocumentType;
import es.checkpol.domain.Guest;
import es.checkpol.domain.GuestReviewStatus;
import es.checkpol.domain.GuestSubmissionSource;
import es.checkpol.repository.BookingRepository;
import es.checkpol.repository.GuestRepository;
import es.checkpol.web.GuestForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class GuestService {

    private static final Pattern ISO3_PATTERN = Pattern.compile("^[A-Za-z]{3}$");
    private static final Pattern MUNICIPALITY_CODE_PATTERN = Pattern.compile("^\\d{5}$");
    private static final Pattern SPANISH_POSTAL_CODE_PATTERN = Pattern.compile("^\\d{5}$");
    private static final Pattern NIF_PATTERN = Pattern.compile("^\\d{8}[A-Za-z]$");
    private static final Pattern NIE_PATTERN = Pattern.compile("^[XYZxyz]\\d{7}[A-Za-z]$");
    private static final Set<String> RELATIONSHIP_CODES = Set.of("AB","BA","BN","CY","CD","HR","HJ","PM","NI","SB","SG","TI","YN","TU","OT");

    private final GuestRepository guestRepository;
    private final BookingRepository bookingRepository;

    public GuestService(GuestRepository guestRepository, BookingRepository bookingRepository) {
        this.guestRepository = guestRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public List<Guest> findByBookingId(Long bookingId) {
        return guestRepository.findAllByBookingIdOrderByIdAsc(bookingId);
    }

    @Transactional
    public Guest create(Long bookingId, GuestForm form) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa estancia."));

        validateMunicipality(form);
        validateContact(form);
        validateDocumentSupport(form);
        validateCountryCodes(form);
        validateDocumentData(form, booking);
        validateMinorRelationship(form);
        validateRelationshipCode(form);

        Guest guest = new Guest(
            booking,
            form.firstName().trim(),
            form.lastName1().trim(),
            normalize(form.lastName2()),
            form.documentType(),
            normalize(form.documentNumber()),
            normalize(form.documentSupport()),
            form.birthDate(),
            normalizeUpper(form.nationality()),
            form.sex(),
            form.addressLine().trim(),
            normalize(form.addressComplement()),
            normalize(form.municipalityCode()),
            normalize(form.municipalityName()),
            form.postalCode().trim(),
            form.country().trim().toUpperCase(),
            normalize(form.phone()),
            normalize(form.phone2()),
            normalize(form.email()),
            normalize(form.relationship()),
            GuestSubmissionSource.MANUAL,
            GuestReviewStatus.REVIEWED,
            null
        );
        return guestRepository.save(guest);
    }

    @Transactional(readOnly = true)
    public GuestForm getForm(Long guestId) {
        Guest guest = guestRepository.findById(guestId)
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa persona."));
        return new GuestForm(
            guest.getFirstName(),
            guest.getLastName1(),
            guest.getLastName2() == null ? "" : guest.getLastName2(),
            guest.getDocumentType(),
            guest.getDocumentNumber() == null ? "" : guest.getDocumentNumber(),
            guest.getDocumentSupport() == null ? "" : guest.getDocumentSupport(),
            guest.getBirthDate(),
            guest.getNationality() == null ? "" : guest.getNationality(),
            guest.getSex(),
            guest.getAddressLine(),
            guest.getAddressComplement() == null ? "" : guest.getAddressComplement(),
            guest.getMunicipalityCode() == null ? "" : guest.getMunicipalityCode(),
            guest.getMunicipalityName() == null ? "" : guest.getMunicipalityName(),
            guest.getPostalCode(),
            guest.getCountry(),
            guest.getPhone() == null ? "" : guest.getPhone(),
            guest.getPhone2() == null ? "" : guest.getPhone2(),
            guest.getEmail() == null ? "" : guest.getEmail(),
            guest.getRelationship() == null ? "" : guest.getRelationship()
        );
    }

    @Transactional
    public Guest update(Long guestId, GuestForm form) {
        Guest guest = guestRepository.findById(guestId)
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa persona."));

        validateMunicipality(form);
        validateContact(form);
        validateDocumentSupport(form);
        validateCountryCodes(form);
        validateDocumentData(form, guest.getBooking());
        validateMinorRelationship(form);
        validateRelationshipCode(form);
        guest.update(
            form.firstName().trim(),
            form.lastName1().trim(),
            normalize(form.lastName2()),
            form.documentType(),
            normalize(form.documentNumber()),
            normalize(form.documentSupport()),
            form.birthDate(),
            normalizeUpper(form.nationality()),
            form.sex(),
            form.addressLine().trim(),
            normalize(form.addressComplement()),
            normalize(form.municipalityCode()),
            normalize(form.municipalityName()),
            form.postalCode().trim(),
            form.country().trim().toUpperCase(),
            normalize(form.phone()),
            normalize(form.phone2()),
            normalize(form.email()),
            normalize(form.relationship())
        );
        guest.markReviewed();
        return guest;
    }

    @Transactional
    public Guest createFromSelfService(Long bookingId, GuestForm form) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa estancia."));

        validateMunicipality(form);
        validateContact(form);
        validateDocumentSupport(form);
        validateCountryCodes(form);
        validateDocumentData(form, booking);
        validateMinorRelationship(form);
        validateRelationshipCode(form);

        Guest guest = new Guest(
            booking,
            form.firstName().trim(),
            form.lastName1().trim(),
            normalize(form.lastName2()),
            form.documentType(),
            normalize(form.documentNumber()),
            normalize(form.documentSupport()),
            form.birthDate(),
            normalizeUpper(form.nationality()),
            form.sex(),
            form.addressLine().trim(),
            normalize(form.addressComplement()),
            normalize(form.municipalityCode()),
            normalize(form.municipalityName()),
            form.postalCode().trim(),
            form.country().trim().toUpperCase(),
            normalize(form.phone()),
            normalize(form.phone2()),
            normalize(form.email()),
            normalize(form.relationship()),
            GuestSubmissionSource.SELF_SERVICE,
            GuestReviewStatus.PENDING_REVIEW,
            OffsetDateTime.now()
        );
        return guestRepository.save(guest);
    }

    @Transactional
    public Guest updateFromSelfService(Long guestId, GuestForm form) {
        Guest guest = guestRepository.findById(guestId)
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa persona."));

        validateMunicipality(form);
        validateContact(form);
        validateDocumentSupport(form);
        validateCountryCodes(form);
        validateDocumentData(form, guest.getBooking());
        validateMinorRelationship(form);
        validateRelationshipCode(form);

        guest.update(
            form.firstName().trim(),
            form.lastName1().trim(),
            normalize(form.lastName2()),
            form.documentType(),
            normalize(form.documentNumber()),
            normalize(form.documentSupport()),
            form.birthDate(),
            normalizeUpper(form.nationality()),
            form.sex(),
            form.addressLine().trim(),
            normalize(form.addressComplement()),
            normalize(form.municipalityCode()),
            normalize(form.municipalityName()),
            form.postalCode().trim(),
            form.country().trim().toUpperCase(),
            normalize(form.phone()),
            normalize(form.phone2()),
            normalize(form.email()),
            normalize(form.relationship())
        );
        return guest;
    }

    @Transactional
    public void markReviewed(Long guestId) {
        Guest guest = guestRepository.findById(guestId)
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa persona."));
        guest.markReviewed();
    }

    private void validateMunicipality(GuestForm form) {
        if ("ESP".equalsIgnoreCase(form.country())) {
            if (isBlank(form.municipalityCode())) {
                throw new IllegalArgumentException("Si el pais es ESP, indica el codigo del municipio.");
            }
            if (!MUNICIPALITY_CODE_PATTERN.matcher(form.municipalityCode().trim()).matches()) {
                throw new IllegalArgumentException("El codigo del municipio debe tener 5 numeros.");
            }
            if (!SPANISH_POSTAL_CODE_PATTERN.matcher(form.postalCode().trim()).matches()) {
                throw new IllegalArgumentException("Si el pais es ESP, el codigo postal debe tener 5 numeros.");
            }
        } else if (isBlank(form.municipalityName())) {
            throw new IllegalArgumentException("Si el pais no es ESP, escribe la ciudad o municipio.");
        }
    }

    private void validateContact(GuestForm form) {
        if (isBlank(form.phone()) && isBlank(form.phone2()) && isBlank(form.email())) {
            throw new IllegalArgumentException("Escribe al menos un telefono o un correo.");
        }
    }

    private void validateDocumentSupport(GuestForm form) {
        if ((form.documentType() == DocumentType.NIF || form.documentType() == DocumentType.NIE)
            && isBlank(form.documentSupport())) {
            throw new IllegalArgumentException("Si el documento es NIF o NIE, indica tambien el numero de soporte.");
        }
    }

    private void validateCountryCodes(GuestForm form) {
        if (!ISO3_PATTERN.matcher(form.country().trim()).matches()) {
            throw new IllegalArgumentException("El pais debe escribirse con 3 letras, por ejemplo ESP.");
        }
        if (!isBlank(form.nationality()) && !ISO3_PATTERN.matcher(form.nationality().trim()).matches()) {
            throw new IllegalArgumentException("La nacionalidad debe escribirse con 3 letras, por ejemplo ESP.");
        }
    }

    private void validateDocumentData(GuestForm form, Booking booking) {
        boolean hasType = form.documentType() != null;
        boolean hasNumber = !isBlank(form.documentNumber());
        boolean documentRequired = isDocumentRequired(form, booking.getCheckInDate());

        if (documentRequired && (!hasType || !hasNumber)) {
            throw new IllegalArgumentException("A esta persona le falta el documento.");
        }

        if (hasType != hasNumber) {
            throw new IllegalArgumentException("Rellena juntos el tipo y el numero del documento.");
        }

        if (form.documentType() == DocumentType.NIF) {
            if (!NIF_PATTERN.matcher(form.documentNumber().trim()).matches() || !hasValidNifLetter(form.documentNumber().trim())) {
                throw new IllegalArgumentException("El NIF no tiene un formato valido.");
            }
            if (isBlank(form.lastName2())) {
                throw new IllegalArgumentException("Si usas NIF, escribe tambien el segundo apellido.");
            }
        }

        if (form.documentType() == DocumentType.NIE) {
            if (!NIE_PATTERN.matcher(form.documentNumber().trim()).matches() || !hasValidNieLetter(form.documentNumber().trim())) {
                throw new IllegalArgumentException("El NIE no tiene un formato valido.");
            }
        }
    }

    private void validateMinorRelationship(GuestForm form) {
        if (form.birthDate() == null) {
            return;
        }
        if (form.birthDate().plusYears(18).isAfter(LocalDate.now()) && isBlank(form.relationship())) {
            throw new IllegalArgumentException("Si es menor de edad, indica el parentesco.");
        }
    }

    private void validateRelationshipCode(GuestForm form) {
        if (!isBlank(form.relationship()) && !RELATIONSHIP_CODES.contains(form.relationship().trim().toUpperCase())) {
            throw new IllegalArgumentException("El parentesco no tiene un codigo valido.");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeUpper(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isDocumentRequired(GuestForm form, LocalDate checkInDate) {
        int age = calculateAgeAt(form.birthDate(), checkInDate);
        return age >= 18 || (age > 14 && "ESP".equalsIgnoreCase(form.nationality()));
    }

    private int calculateAgeAt(LocalDate birthDate, LocalDate referenceDate) {
        int age = referenceDate.getYear() - birthDate.getYear();
        if (birthDate.plusYears(age).isAfter(referenceDate)) {
            age--;
        }
        return age;
    }

    private boolean hasValidNifLetter(String nif) {
        String letters = "TRWAGMYFPDXBNJZSQVHLCKE";
        int number = Integer.parseInt(nif.substring(0, 8));
        char expected = letters.charAt(number % 23);
        return Character.toUpperCase(nif.charAt(8)) == expected;
    }

    private boolean hasValidNieLetter(String nie) {
        char prefix = Character.toUpperCase(nie.charAt(0));
        String mappedPrefix = switch (prefix) {
            case 'X' -> "0";
            case 'Y' -> "1";
            case 'Z' -> "2";
            default -> "";
        };
        String nifEquivalent = mappedPrefix + nie.substring(1);
        return hasValidNifLetter(nifEquivalent);
    }
}
