package es.checkpol.service;

import es.checkpol.domain.Booking;
import es.checkpol.domain.DocumentType;
import es.checkpol.domain.Guest;
import es.checkpol.domain.GuestRelationship;
import es.checkpol.domain.GuestReviewStatus;
import es.checkpol.domain.GuestSubmissionSource;
import es.checkpol.domain.Address;
import es.checkpol.repository.AddressRepository;
import es.checkpol.repository.BookingRepository;
import es.checkpol.repository.GuestRepository;
import es.checkpol.web.GuestForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class GuestService {

    private static final Pattern ISO3_PATTERN = Pattern.compile("^[A-Za-z]{3}$");
    private static final Pattern NIF_PATTERN = Pattern.compile("^\\d{8}[A-Za-z]$");
    private static final Pattern NIE_PATTERN = Pattern.compile("^[XYZxyz]\\d{7}[A-Za-z]$");
    private static final Pattern INTERNATIONAL_PHONE_PATTERN = Pattern.compile("^\\+\\d[\\d\\s().-]{5,19}$");
    private final AddressRepository addressRepository;
    private final GuestRepository guestRepository;
    private final BookingRepository bookingRepository;
    private final CurrentAppUserService currentAppUserService;

    public GuestService(
        AddressRepository addressRepository,
        GuestRepository guestRepository,
        BookingRepository bookingRepository,
        CurrentAppUserService currentAppUserService
    ) {
        this.addressRepository = addressRepository;
        this.guestRepository = guestRepository;
        this.bookingRepository = bookingRepository;
        this.currentAppUserService = currentAppUserService;
    }

    @Transactional(readOnly = true)
    public List<Guest> findByBookingId(Long bookingId) {
        return guestRepository.findAllByBookingIdAndBookingOwnerIdOrderByIdAsc(bookingId, currentAppUserService.requireCurrentUserId());
    }

    @Transactional
    public Guest create(Long bookingId, GuestForm form) {
        Booking booking = bookingRepository.findByIdAndOwnerId(bookingId, currentAppUserService.requireCurrentUserId())
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa estancia."));

        validateContact(form);
        validateDocumentSupport(form);
        validateNationality(form);
        validateDocumentData(form, booking);
        validateSex(form);
        validateMinorRelationship(form, booking.getCheckInDate());
        validateRelationshipCode(form);
        validatePhoneNumbers(form);
        Address address = getBookingAddress(booking.getId(), form.addressId());

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
            address,
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
        Guest guest = guestRepository.findByIdAndBookingOwnerId(guestId, currentAppUserService.requireCurrentUserId())
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
            guest.getAddressId(),
            guest.getPhone() == null ? "" : guest.getPhone(),
            guest.getPhone2() == null ? "" : guest.getPhone2(),
            guest.getEmail() == null ? "" : guest.getEmail(),
            guest.getRelationship() == null ? "" : guest.getRelationship()
        );
    }

    @Transactional
    public Guest update(Long guestId, GuestForm form) {
        Guest guest = guestRepository.findByIdAndBookingOwnerId(guestId, currentAppUserService.requireCurrentUserId())
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa persona."));

        validateContact(form);
        validateDocumentSupport(form);
        validateNationality(form);
        validateDocumentData(form, guest.getBooking());
        validateSex(form);
        validateMinorRelationship(form, guest.getBooking().getCheckInDate());
        validateRelationshipCode(form);
        validatePhoneNumbers(form);
        Address address = getBookingAddress(guest.getBooking().getId(), form.addressId());
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
            address,
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

        validateContact(form);
        validateDocumentSupport(form);
        validateNationality(form);
        validateDocumentData(form, booking);
        validateSex(form);
        validateMinorRelationship(form, booking.getCheckInDate());
        validateRelationshipCode(form);
        validatePhoneNumbers(form);
        Address address = getBookingAddress(booking.getId(), form.addressId());

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
            address,
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

        validateContact(form);
        validateDocumentSupport(form);
        validateNationality(form);
        validateDocumentData(form, guest.getBooking());
        validateSex(form);
        validateMinorRelationship(form, guest.getBooking().getCheckInDate());
        validateRelationshipCode(form);
        validatePhoneNumbers(form);
        Address address = getBookingAddress(guest.getBooking().getId(), form.addressId());

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
            address,
            normalize(form.phone()),
            normalize(form.phone2()),
            normalize(form.email()),
            normalize(form.relationship())
        );
        return guest;
    }

    @Transactional
    public void markReviewed(Long guestId) {
        Guest guest = guestRepository.findByIdAndBookingOwnerId(guestId, currentAppUserService.requireCurrentUserId())
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa persona."));
        guest.markReviewed();
    }

    @Transactional
    public void assignAddress(Long guestId, Long addressId) {
        Guest guest = guestRepository.findByIdAndBookingOwnerId(guestId, currentAppUserService.requireCurrentUserId())
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa persona."));
        Address address = getBookingAddress(guest.getBooking().getId(), addressId);
        guest.update(
            guest.getFirstName(),
            guest.getLastName1(),
            guest.getLastName2(),
            guest.getDocumentType(),
            guest.getDocumentNumber(),
            guest.getDocumentSupport(),
            guest.getBirthDate(),
            guest.getNationality(),
            guest.getSex(),
            address,
            guest.getPhone(),
            guest.getPhone2(),
            guest.getEmail(),
            guest.getRelationship()
        );
    }

    private void validateContact(GuestForm form) {
        if (isBlank(form.phone()) && isBlank(form.phone2()) && isBlank(form.email())) {
            throw new IllegalArgumentException("Escribe al menos un telefono o un correo.");
        }
    }

    private void validateSex(GuestForm form) {
        if (form.sex() == null) {
            throw new IllegalArgumentException("Selecciona el sexo.");
        }
    }

    private void validateDocumentSupport(GuestForm form) {
        if ((form.documentType() == DocumentType.NIF || form.documentType() == DocumentType.NIE)
            && isBlank(form.documentSupport())) {
            throw new IllegalArgumentException("Si el documento es DNI o NIE, indica tambien el numero de soporte.");
        }
    }

    private void validateNationality(GuestForm form) {
        if (isBlank(form.nationality())) {
            throw new IllegalArgumentException("Selecciona la nacionalidad.");
        }
        if (!ISO3_PATTERN.matcher(form.nationality().trim()).matches()) {
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
                throw new IllegalArgumentException("El DNI no tiene un formato valido.");
            }
            if (isBlank(form.lastName2())) {
                throw new IllegalArgumentException("Si usas DNI, escribe tambien el segundo apellido.");
            }
        }

        if (form.documentType() == DocumentType.NIE) {
            if (!NIE_PATTERN.matcher(form.documentNumber().trim()).matches() || !hasValidNieLetter(form.documentNumber().trim())) {
                throw new IllegalArgumentException("El NIE no tiene un formato valido.");
            }
        }
    }

    private void validatePhoneNumbers(GuestForm form) {
        if (!isBlank(form.phone()) && !INTERNATIONAL_PHONE_PATTERN.matcher(form.phone().trim()).matches()) {
            throw new IllegalArgumentException("El telefono debe incluir prefijo internacional. Ejemplo: +34 600 123 123.");
        }
        if (!isBlank(form.phone2()) && !INTERNATIONAL_PHONE_PATTERN.matcher(form.phone2().trim()).matches()) {
            throw new IllegalArgumentException("El segundo telefono debe incluir prefijo internacional. Ejemplo: +34 600 123 123.");
        }
    }

    private void validateMinorRelationship(GuestForm form, LocalDate checkInDate) {
        if (form.birthDate() == null) {
            return;
        }
        if (calculateAgeAt(form.birthDate(), checkInDate) < 18 && isBlank(form.relationship())) {
            throw new IllegalArgumentException("Si es menor de edad, indica el parentesco.");
        }
    }

    private void validateRelationshipCode(GuestForm form) {
        if (!isBlank(form.relationship()) && !GuestRelationship.isValidCode(form.relationship().trim().toUpperCase())) {
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

    private Address getBookingAddress(Long bookingId, Long addressId) {
        if (addressId == null) {
            throw new IllegalArgumentException("Selecciona una direccion.");
        }
        return addressRepository.findByIdAndBookingIdAndBookingOwnerId(addressId, bookingId, currentAppUserService.requireCurrentUserId())
            .orElseThrow(() -> new IllegalArgumentException("La direccion seleccionada no pertenece a esta estancia."));
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
