package es.checkpol.service;

import es.checkpol.domain.Accommodation;
import es.checkpol.domain.Booking;
import es.checkpol.domain.BookingChannel;
import es.checkpol.domain.DocumentType;
import es.checkpol.domain.GeneratedCommunication;
import es.checkpol.domain.Guest;
import es.checkpol.domain.GuestRelationship;
import es.checkpol.domain.GuestReviewStatus;
import es.checkpol.domain.PaymentType;
import es.checkpol.repository.AccommodationRepository;
import es.checkpol.repository.BookingRepository;
import es.checkpol.repository.GeneratedCommunicationRepository;
import es.checkpol.repository.GuestRepository;
import es.checkpol.web.BookingForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class BookingService {

    private static final Pattern ISO3_PATTERN = Pattern.compile("^[A-Za-z]{3}$");
    private static final Pattern NIF_PATTERN = Pattern.compile("^\\d{8}[A-Za-z]$");
    private static final Pattern NIE_PATTERN = Pattern.compile("^[XYZxyz]\\d{7}[A-Za-z]$");

    private final BookingRepository bookingRepository;
    private final AccommodationRepository accommodationRepository;
    private final GuestRepository guestRepository;
    private final GeneratedCommunicationRepository generatedCommunicationRepository;
    private final CurrentAppUserService currentAppUserService;

    public BookingService(
        BookingRepository bookingRepository,
        AccommodationRepository accommodationRepository,
        GuestRepository guestRepository,
        GeneratedCommunicationRepository generatedCommunicationRepository,
        CurrentAppUserService currentAppUserService
    ) {
        this.bookingRepository = bookingRepository;
        this.accommodationRepository = accommodationRepository;
        this.guestRepository = guestRepository;
        this.generatedCommunicationRepository = generatedCommunicationRepository;
        this.currentAppUserService = currentAppUserService;
    }

    @Transactional(readOnly = true)
    public List<BookingListItem> findAll() {
        List<BookingListItem> items = new ArrayList<>();
        Long currentUserId = currentAppUserService.requireCurrentUserId();
        for (Booking booking : bookingRepository.findAllForList(currentUserId)) {
            List<Guest> guests = guestRepository.findAllByBookingIdAndBookingOwnerIdOrderByIdAsc(booking.getId(), currentUserId);
            List<GeneratedCommunication> communications = generatedCommunicationRepository.findAllByBookingIdAndBookingOwnerIdOrderByGeneratedAtDesc(booking.getId(), currentUserId);
            int generatedCount = communications.size();
            ReadinessAssessment assessment = assessReadiness(booking, guests);
            items.add(new BookingListItem(
                booking,
                guests.size(),
                booking.getPersonCount() == null ? 0 : booking.getPersonCount(),
                assessment.readyForTravelerPart(),
                booking.getOwner() != null && booking.getOwner().hasSesWebServiceConfiguration(),
                calculateOperationalStatus(assessment, generatedCount),
                assessment.pendingReviewGuestCount(),
                assessment.guestCountMismatch(),
                assessment.blockingSummary(),
                buildCommunicationStatusSummary(communications.isEmpty() ? null : communications.getFirst())
            ));
        }
        return items;
    }

    @Transactional(readOnly = true)
    public List<BookingListItem> findAll(BookingFilter filter) {
        LocalDate today = LocalDate.now();
        return findAll().stream()
            .filter(item -> matchesFilter(item, filter, today))
            .toList();
    }

    @Transactional
    public Booking create(BookingForm form) {
        validateDates(form);
        var currentUser = currentAppUserService.requireCurrentUserEntity();
        Accommodation accommodation = accommodationRepository.findByIdAndOwnerId(form.accommodationId(), currentUser.getId())
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa vivienda."));

        Booking booking = new Booking(
            currentUser,
            accommodation,
            form.referenceCode().trim(),
            form.personCount(),
            form.contractDate(),
            BookingChannel.AIRBNB,
            form.checkInDate(),
            form.checkOutDate(),
            PaymentType.PLATF,
            null,
            "Airbnb",
            null,
            null
        );
        return bookingRepository.save(booking);
    }

    @Transactional(readOnly = true)
    public BookingForm getForm(Long id) {
        Booking booking = bookingRepository.findDetailById(id, currentAppUserService.requireCurrentUserId())
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa estancia."));
        return new BookingForm(
            booking.getAccommodation().getId(),
            booking.getReferenceCode(),
            booking.getPersonCount(),
            booking.getContractDate(),
            booking.getCheckInDate(),
            booking.getCheckOutDate()
        );
    }

    @Transactional
    public Booking update(Long id, BookingForm form) {
        validateDates(form);
        Long currentUserId = currentAppUserService.requireCurrentUserId();
        Booking booking = bookingRepository.findByIdAndOwnerId(id, currentUserId)
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa estancia."));
        Accommodation accommodation = accommodationRepository.findByIdAndOwnerId(form.accommodationId(), currentUserId)
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa vivienda."));
        booking.update(
            accommodation,
            form.referenceCode().trim(),
            form.personCount(),
            form.contractDate(),
            BookingChannel.AIRBNB,
            form.checkInDate(),
            form.checkOutDate(),
            PaymentType.PLATF,
            null,
            "Airbnb",
            null,
            null
        );
        return booking;
    }

    @Transactional(readOnly = true)
    public BookingDetails getDetails(Long id) {
        Long currentUserId = currentAppUserService.requireCurrentUserId();
        Booking booking = bookingRepository.findDetailById(id, currentUserId)
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa estancia."));
        List<Guest> guests = guestRepository.findAllByBookingIdAndBookingOwnerIdOrderByIdAsc(id, currentUserId);
        List<GeneratedCommunication> communications = generatedCommunicationRepository.findAllByBookingIdAndBookingOwnerIdOrderByGeneratedAtDesc(id, currentUserId);
        ReadinessAssessment assessment = assessReadiness(booking, guests);
        return new BookingDetails(
            booking,
            guests,
            guests.size(),
            booking.getPersonCount() == null ? 0 : booking.getPersonCount(),
            assessment.guestCountMismatch(),
            assessment.readyForTravelerPart(),
            booking.getOwner() != null && booking.getOwner().hasSesWebServiceConfiguration(),
            communications.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(communications.getFirst()),
            communications.size(),
            communications,
            booking.getSelfServiceToken() == null || booking.getSelfServiceExpiresAt() == null
                ? java.util.Optional.empty()
                : java.util.Optional.of(new SelfServiceAccess(booking.getSelfServiceToken(), booking.getSelfServiceExpiresAt())),
            calculateOperationalStatus(assessment, communications.size()),
            assessment.pendingReviewGuestCount(),
            guests.stream().filter(guest -> guest.getSubmissionSource() == es.checkpol.domain.GuestSubmissionSource.SELF_SERVICE).count(),
            assessment.blockedByBookingData(),
            assessment.blockedByGuestData(),
            assessment.blockedByPendingReview(),
            assessment.blockedByAddressExport(),
            assessment.blockingSummary(),
            assessment.blockingMessage(),
            assessment.blockingReasons()
        );
    }

    private void validateDates(BookingForm form) {
        if (!form.checkOutDate().isAfter(form.checkInDate())) {
            throw new IllegalArgumentException("La fecha de salida debe ir despues de la fecha de entrada.");
        }
        if (form.contractDate() != null && form.contractDate().isAfter(form.checkInDate())) {
            throw new IllegalArgumentException("La fecha en la que cerraste la estancia no puede ir despues de la entrada.");
        }
        if (form.checkInDate().isBefore(LocalDate.now().minusYears(1))) {
            throw new IllegalArgumentException("La fecha de entrada no puede ser de hace mas de un ano.");
        }
        if (form.checkOutDate().isAfter(LocalDate.now().plusYears(5))) {
            throw new IllegalArgumentException("La fecha de salida no puede ir más allá de cinco años desde hoy.");
        }
    }

    private boolean isReadyForTravelerPart(List<Guest> guests) {
        if (guests.isEmpty()) {
            return false;
        }
        return assessReadiness(guests.getFirst().getBooking(), guests).readyForTravelerPart();
    }

    private boolean hasRequiredBookingData(Booking booking) {
        return booking.getAccommodation() != null
            && !isBlank(booking.getAccommodation().getSesEstablishmentCode())
            && booking.getContractDate() != null
            && booking.getCheckInDate() != null
            && booking.getCheckOutDate() != null
            && booking.getPaymentType() != null;
    }

    private boolean hasRequiredTravelerPartData(Guest guest) {
        if (!hasRequiredTravelerCoreData(guest)) {
            return false;
        }
        return hasExportableAddress(guest);
    }

    private boolean isAdultOrSpanishOlderThan14(Guest guest) {
        if (guest.getBirthDate() == null) {
            return false;
        }
        LocalDate referenceDate = guest.getBooking().getCheckInDate();
        int age = referenceDate.getYear() - guest.getBirthDate().getYear();
        if (guest.getBirthDate().plusYears(age).isAfter(referenceDate)) {
            age--;
        }
        return age >= 18 || (age > 14 && "ESP".equalsIgnoreCase(guest.getNationality()));
    }
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean matchesFilter(BookingListItem item, BookingFilter filter, LocalDate today) {
        return switch (filter) {
            case ALL -> true;
            case INCOMPLETE -> item.operationalStatus() == BookingOperationalStatus.INCOMPLETE
                || item.operationalStatus() == BookingOperationalStatus.WAITING_GUESTS
                || item.operationalStatus() == BookingOperationalStatus.GUEST_COUNT_MISMATCH
                || item.operationalStatus() == BookingOperationalStatus.REVIEW_PENDING;
            case READY -> item.operationalStatus() == BookingOperationalStatus.READY_FOR_XML
                || item.operationalStatus() == BookingOperationalStatus.XML_GENERATED;
            case TODAY -> item.booking().getCheckInDate().isEqual(today);
            case UPCOMING -> item.booking().getCheckInDate().isAfter(today);
        };
    }

    private String buildCommunicationStatusSummary(GeneratedCommunication communication) {
        if (communication == null) {
            return null;
        }
        return switch (communication.getDispatchStatus()) {
            case XML_READY -> "XML preparado para descargar.";
            case SUBMITTED_TO_SES -> "Enviada a SES. Falta consultar el lote.";
            case SES_PROCESSED -> communication.getSesCommunicationCode() == null || communication.getSesCommunicationCode().isBlank()
                ? "SES la ha procesado correctamente."
                : "SES la ha procesado. Código: " + communication.getSesCommunicationCode() + ".";
            case SES_PROCESSING_ERROR -> communication.getSesProcessingErrorDescription() == null || communication.getSesProcessingErrorDescription().isBlank()
                ? "SES ha devuelto un error de procesamiento."
                : communication.getSesProcessingErrorDescription();
            case SES_CANCELLED -> "Comunicación anulada en SES.";
            case SUBMISSION_FAILED -> communication.getSesResponseDescription() == null || communication.getSesResponseDescription().isBlank()
                ? "El envío a SES no se pudo completar."
                : communication.getSesResponseDescription();
        };
    }

    private BookingOperationalStatus calculateOperationalStatus(ReadinessAssessment assessment, int generatedCount) {
        if (assessment.guestCount() == 0) {
            return BookingOperationalStatus.WAITING_GUESTS;
        }
        if (assessment.guestCountMismatch()) {
            return BookingOperationalStatus.GUEST_COUNT_MISMATCH;
        }
        if (assessment.blockedByPendingReview()) {
            return BookingOperationalStatus.REVIEW_PENDING;
        }
        if (assessment.readyForTravelerPart()) {
            if (generatedCount > 0) {
                return BookingOperationalStatus.XML_GENERATED;
            }
            return BookingOperationalStatus.READY_FOR_XML;
        }
        return BookingOperationalStatus.INCOMPLETE;
    }

    private ReadinessAssessment assessReadiness(Booking booking, List<Guest> guests) {
        long guestCount = guests.size();
        int expectedGuestCount = booking.getPersonCount() == null ? 0 : booking.getPersonCount();
        boolean bookingDataBlocked = !hasRequiredBookingData(booking);
        boolean guestCountMismatch = guestCount != expectedGuestCount;
        long pendingReviewGuestCount = guests.stream()
            .filter(guest -> guest.getReviewStatus() == GuestReviewStatus.PENDING_REVIEW)
            .count();
        boolean blockedByPendingReview = pendingReviewGuestCount > 0;
        boolean blockedByGuestData = guests.stream().anyMatch(guest -> !hasRequiredTravelerCoreData(guest));
        boolean blockedByAddressExport = guests.stream().anyMatch(guest -> !hasExportableAddress(guest));
        boolean ready = guestCount > 0
            && !bookingDataBlocked
            && !guestCountMismatch
            && !blockedByPendingReview
            && !blockedByGuestData
            && !blockedByAddressExport;

        List<String> blockingReasons = buildBlockingReasons(
            booking,
            guestCount,
            expectedGuestCount,
            pendingReviewGuestCount,
            bookingDataBlocked,
            guestCountMismatch,
            blockedByPendingReview,
            blockedByGuestData,
            blockedByAddressExport
        );

        return new ReadinessAssessment(
            ready,
            guestCount,
            expectedGuestCount,
            pendingReviewGuestCount,
            guestCountMismatch,
            bookingDataBlocked,
            blockedByGuestData,
            blockedByPendingReview,
            blockedByAddressExport,
            buildBlockingSummary(
                guestCount,
                expectedGuestCount,
                pendingReviewGuestCount,
                bookingDataBlocked,
                guestCountMismatch,
                blockedByPendingReview,
                blockedByGuestData,
                blockedByAddressExport
            ),
            blockingReasons.isEmpty() ? null : blockingReasons.getFirst(),
            blockingReasons
        );
    }

    private List<String> buildBlockingReasons(
        Booking booking,
        long guestCount,
        int expectedGuestCount,
        long pendingReviewGuestCount,
        boolean bookingDataBlocked,
        boolean guestCountMismatch,
        boolean blockedByPendingReview,
        boolean blockedByGuestData,
        boolean blockedByAddressExport
    ) {
        List<String> reasons = new ArrayList<>();

        if (bookingDataBlocked) {
            if (booking.getAccommodation() == null || isBlank(booking.getAccommodation().getSesEstablishmentCode())) {
                reasons.add("Falta el código SES de la vivienda. Añádelo en la ficha de la vivienda antes de generar el archivo.");
            } else {
                reasons.add("Faltan datos básicos de la estancia para generar el archivo. Revisa la fecha de contrato y el tipo de pago.");
            }
        }

        if (guestCountMismatch) {
            reasons.add("No se puede generar el XML porque el número de huéspedes registrados no coincide con el número de personas de la estancia. Ahora mismo hay "
                + guestCount + " huéspedes dados de alta y la estancia está configurada para " + expectedGuestCount
                + " personas. Revisa los huéspedes registrados y, si son correctos, ajusta el número de personas de la estancia para poder generar el XML.");
        }

        if (blockedByPendingReview) {
            reasons.add(pendingReviewGuestCount == 1
                ? "Todavía hay 1 huésped pendiente de revisión. Revísalo antes de generar el archivo."
                : "Todavía hay " + pendingReviewGuestCount + " huéspedes pendientes de revisión. Revísalos antes de generar el archivo.");
        }

        if (blockedByAddressExport) {
            reasons.add("Hay al menos una dirección en España sin un código de municipio exportable. Revisa la dirección y vuelve a seleccionar el municipio correcto antes de generar el archivo.");
        }

        if (blockedByGuestData) {
            reasons.add("Faltan datos obligatorios en uno o varios huéspedes. Completa el documento cuando corresponda, el parentesco de los menores y un dato de contacto antes de generar el archivo.");
        }

        if (reasons.isEmpty() && guestCount == 0) {
            reasons.add("Todavía no hay huéspedes registrados. Añade a las personas de la estancia antes de generar el archivo.");
        }

        return reasons;
    }

    private String buildBlockingSummary(
        long guestCount,
        int expectedGuestCount,
        long pendingReviewGuestCount,
        boolean bookingDataBlocked,
        boolean guestCountMismatch,
        boolean blockedByPendingReview,
        boolean blockedByGuestData,
        boolean blockedByAddressExport
    ) {
        if (guestCount == 0) {
            return "Falta añadir a las personas de la estancia";
        }
        if (guestCountMismatch) {
            return guestCount + " huéspedes registrados · " + expectedGuestCount + " personas esperadas";
        }
        if (blockedByPendingReview) {
            return pendingReviewGuestCount == 1
                ? "1 huésped pendiente de revisión"
                : pendingReviewGuestCount + " huéspedes pendientes de revisión";
        }
        if (blockedByAddressExport) {
            return "Hay una dirección sin código de municipio";
        }
        if (blockedByGuestData) {
            return "Faltan datos obligatorios de viajeros";
        }
        if (bookingDataBlocked) {
            return "Faltan datos básicos de la estancia";
        }
        return "Lista para descargar el archivo SES";
    }

    private boolean hasRequiredTravelerCoreData(Guest guest) {
        if (!guest.hasMinimumDataForTravelerPart()) {
            return false;
        }
        if (!isBlank(guest.getNationality()) && !ISO3_PATTERN.matcher(guest.getNationality().trim()).matches()) {
            return false;
        }
        if (isMinorAtCheckIn(guest) && (isBlank(guest.getRelationship()) || !GuestRelationship.isValidCode(guest.getRelationship().trim().toUpperCase()))) {
            return false;
        }

        boolean documentRequired = isAdultOrSpanishOlderThan14(guest);
        if (!documentRequired) {
            return true;
        }
        if (guest.getDocumentType() == null || isBlank(guest.getDocumentNumber())) {
            return false;
        }
        if (guest.getDocumentType() == DocumentType.NIF) {
            return NIF_PATTERN.matcher(guest.getDocumentNumber().trim()).matches()
                && hasValidNifLetter(guest.getDocumentNumber().trim())
                && !isBlank(guest.getLastName2())
                && !isBlank(guest.getDocumentSupport());
        }
        if (guest.getDocumentType() == DocumentType.NIE) {
            return NIE_PATTERN.matcher(guest.getDocumentNumber().trim()).matches()
                && hasValidNieLetter(guest.getDocumentNumber().trim())
                && !isBlank(guest.getDocumentSupport());
        }
        return true;
    }

    private boolean hasExportableAddress(Guest guest) {
        if (guest.getAddress() == null || isBlank(guest.getAddressLine()) || isBlank(guest.getPostalCode()) || isBlank(guest.getCountry())) {
            return false;
        }
        if ("ESP".equalsIgnoreCase(guest.getCountry())) {
            return !isBlank(guest.getMunicipalityCode());
        }
        return !isBlank(guest.getMunicipalityName());
    }

    private boolean isMinorAtCheckIn(Guest guest) {
        if (guest.getBirthDate() == null) {
            return false;
        }
        LocalDate referenceDate = guest.getBooking().getCheckInDate();
        int age = referenceDate.getYear() - guest.getBirthDate().getYear();
        if (guest.getBirthDate().plusYears(age).isAfter(referenceDate)) {
            age--;
        }
        return age < 18;
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

    private record ReadinessAssessment(
        boolean readyForTravelerPart,
        long guestCount,
        int expectedGuestCount,
        long pendingReviewGuestCount,
        boolean guestCountMismatch,
        boolean blockedByBookingData,
        boolean blockedByGuestData,
        boolean blockedByPendingReview,
        boolean blockedByAddressExport,
        String blockingSummary,
        String blockingMessage,
        List<String> blockingReasons
    ) {
    }
}
