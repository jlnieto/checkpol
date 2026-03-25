package es.checkpol.service;

import es.checkpol.domain.Accommodation;
import es.checkpol.domain.Booking;
import es.checkpol.domain.Guest;
import es.checkpol.domain.DocumentType;
import es.checkpol.domain.GeneratedCommunication;
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

    private static final Pattern CARD_EXPIRY_PATTERN = Pattern.compile("^(0[1-9]|1[0-2])/\\d{4}$");

    private final BookingRepository bookingRepository;
    private final AccommodationRepository accommodationRepository;
    private final GuestRepository guestRepository;
    private final GeneratedCommunicationRepository generatedCommunicationRepository;

    public BookingService(
        BookingRepository bookingRepository,
        AccommodationRepository accommodationRepository,
        GuestRepository guestRepository,
        GeneratedCommunicationRepository generatedCommunicationRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.accommodationRepository = accommodationRepository;
        this.guestRepository = guestRepository;
        this.generatedCommunicationRepository = generatedCommunicationRepository;
    }

    @Transactional(readOnly = true)
    public List<BookingListItem> findAll() {
        List<BookingListItem> items = new ArrayList<>();
        for (Booking booking : bookingRepository.findAllForList()) {
            List<Guest> guests = guestRepository.findAllByBookingIdOrderByIdAsc(booking.getId());
            int generatedCount = generatedCommunicationRepository.findAllByBookingIdOrderByGeneratedAtDesc(booking.getId()).size();
            items.add(new BookingListItem(
                booking,
                guests.size(),
                isReadyForTravelerPart(guests),
                calculateOperationalStatus(guests, generatedCount)
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
        Accommodation accommodation = accommodationRepository.findById(form.accommodationId())
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa vivienda."));

        Booking booking = new Booking(
            accommodation,
            form.referenceCode().trim(),
            form.contractDate(),
            form.channel(),
            form.checkInDate(),
            form.checkOutDate(),
            form.paymentType(),
            form.paymentDate(),
            normalize(form.paymentMethod()),
            normalize(form.paymentHolder()),
            normalize(form.cardExpiry())
        );
        return bookingRepository.save(booking);
    }

    @Transactional(readOnly = true)
    public BookingForm getForm(Long id) {
        Booking booking = bookingRepository.findDetailById(id)
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa estancia."));
        return new BookingForm(
            booking.getAccommodation().getId(),
            booking.getChannel(),
            booking.getReferenceCode(),
            booking.getContractDate(),
            booking.getCheckInDate(),
            booking.getCheckOutDate(),
            booking.getPaymentType(),
            booking.getPaymentDate(),
            booking.getPaymentMethod() == null ? "" : booking.getPaymentMethod(),
            booking.getPaymentHolder() == null ? "" : booking.getPaymentHolder(),
            booking.getCardExpiry() == null ? "" : booking.getCardExpiry()
        );
    }

    @Transactional
    public Booking update(Long id, BookingForm form) {
        validateDates(form);
        Booking booking = bookingRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa estancia."));
        Accommodation accommodation = accommodationRepository.findById(form.accommodationId())
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa vivienda."));
        booking.update(
            accommodation,
            form.referenceCode().trim(),
            form.contractDate(),
            form.channel(),
            form.checkInDate(),
            form.checkOutDate(),
            form.paymentType(),
            form.paymentDate(),
            normalize(form.paymentMethod()),
            normalize(form.paymentHolder()),
            normalize(form.cardExpiry())
        );
        return booking;
    }

    @Transactional(readOnly = true)
    public BookingDetails getDetails(Long id) {
        Booking booking = bookingRepository.findDetailById(id)
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa estancia."));
        List<Guest> guests = guestRepository.findAllByBookingIdOrderByIdAsc(id);
        List<GeneratedCommunication> communications = generatedCommunicationRepository.findAllByBookingIdOrderByGeneratedAtDesc(id);
        return new BookingDetails(
            booking,
            guests,
            isReadyForTravelerPart(guests),
            communications.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(communications.getFirst()),
            communications.size(),
            communications,
            booking.getSelfServiceToken() == null || booking.getSelfServiceExpiresAt() == null
                ? java.util.Optional.empty()
                : java.util.Optional.of(new SelfServiceAccess(booking.getSelfServiceToken(), booking.getSelfServiceExpiresAt())),
            calculateOperationalStatus(guests, communications.size()),
            guests.stream().filter(guest -> guest.getReviewStatus() == es.checkpol.domain.GuestReviewStatus.PENDING_REVIEW).count()
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
            throw new IllegalArgumentException("La fecha de salida no puede ir mas alla de cinco anos desde hoy.");
        }
        if (form.paymentType() == es.checkpol.domain.PaymentType.TARJT && isBlank(form.cardExpiry())) {
            throw new IllegalArgumentException("Si pagas con tarjeta, indica tambien la caducidad.");
        }
        if (!isBlank(form.cardExpiry()) && !CARD_EXPIRY_PATTERN.matcher(form.cardExpiry().trim()).matches()) {
            throw new IllegalArgumentException("La caducidad debe escribirse como MM/AAAA.");
        }
    }

    private boolean isReadyForTravelerPart(List<Guest> guests) {
        if (guests.isEmpty()) {
            return false;
        }
        Booking booking = guests.getFirst().getBooking();
        if (!hasRequiredBookingData(booking)) {
            return false;
        }
        return guests.stream().allMatch(this::hasRequiredTravelerPartData);
    }

    private boolean hasRequiredBookingData(Booking booking) {
        return booking.getAccommodation() != null
            && !isBlank(booking.getAccommodation().getSesEstablishmentCode())
            && booking.getContractDate() != null
            && booking.getPaymentType() != null;
    }

    private boolean hasRequiredTravelerPartData(Guest guest) {
        if (!guest.hasMinimumDataForTravelerPart()) {
            return false;
        }

        boolean documentRequired = isAdultOrSpanishOlderThan14(guest);
        if (!documentRequired) {
            return true;
        }

        if (guest.getDocumentType() == null || isBlank(guest.getDocumentNumber())) {
            return false;
        }
        if ((guest.getDocumentType() == DocumentType.NIF || guest.getDocumentType() == DocumentType.NIE)
            && isBlank(guest.getDocumentSupport())) {
            return false;
        }
        return guest.getDocumentType() != DocumentType.NIF || !isBlank(guest.getLastName2());
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

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean matchesFilter(BookingListItem item, BookingFilter filter, LocalDate today) {
        return switch (filter) {
            case ALL -> true;
            case INCOMPLETE -> item.operationalStatus() == BookingOperationalStatus.INCOMPLETE
                || item.operationalStatus() == BookingOperationalStatus.WAITING_GUESTS
                || item.operationalStatus() == BookingOperationalStatus.REVIEW_PENDING;
            case READY -> item.operationalStatus() == BookingOperationalStatus.READY_FOR_XML
                || item.operationalStatus() == BookingOperationalStatus.XML_GENERATED;
            case TODAY -> item.booking().getCheckInDate().isEqual(today);
            case UPCOMING -> item.booking().getCheckInDate().isAfter(today);
        };
    }

    private BookingOperationalStatus calculateOperationalStatus(List<Guest> guests, int generatedCount) {
        if (guests.isEmpty()) {
            return BookingOperationalStatus.WAITING_GUESTS;
        }
        if (guests.stream().anyMatch(guest -> guest.getReviewStatus() == es.checkpol.domain.GuestReviewStatus.PENDING_REVIEW)) {
            return BookingOperationalStatus.REVIEW_PENDING;
        }
        if (generatedCount > 0) {
            return BookingOperationalStatus.XML_GENERATED;
        }
        if (isReadyForTravelerPart(guests)) {
            return BookingOperationalStatus.READY_FOR_XML;
        }
        return BookingOperationalStatus.INCOMPLETE;
    }
}
