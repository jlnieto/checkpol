package es.checkpol.service;

import es.checkpol.domain.Address;
import es.checkpol.domain.Booking;
import es.checkpol.domain.MunicipalityResolutionStatus;
import es.checkpol.repository.AddressRepository;
import es.checkpol.repository.BookingRepository;
import es.checkpol.web.AddressForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class AddressService {

    private static final Pattern ISO3_PATTERN = Pattern.compile("^[A-Za-z]{3}$");
    private static final Pattern SPANISH_POSTAL_CODE_PATTERN = Pattern.compile("^\\d{5}$");

    private final AddressRepository addressRepository;
    private final BookingRepository bookingRepository;
    private final MunicipalityResolverService municipalityResolverService;

    public AddressService(
        AddressRepository addressRepository,
        BookingRepository bookingRepository,
        MunicipalityResolverService municipalityResolverService
    ) {
        this.addressRepository = addressRepository;
        this.bookingRepository = bookingRepository;
        this.municipalityResolverService = municipalityResolverService;
    }

    @Transactional(readOnly = true)
    public List<Address> findByBookingId(Long bookingId) {
        return addressRepository.findAllByBookingIdOrderByIdAsc(bookingId);
    }

    @Transactional(readOnly = true)
    public Address getByIdAndBookingId(Long addressId, Long bookingId) {
        return addressRepository.findByIdAndBookingId(addressId, bookingId)
            .orElseThrow(() -> new IllegalArgumentException("La direccion seleccionada no pertenece a esta estancia."));
    }

    @Transactional
    public Address create(Long bookingId, AddressForm form) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa estancia."));

        validate(form);
        MunicipalityResolution municipalityResolution = municipalityResolverService.resolve(
            form.country(),
            form.postalCode(),
            null,
            form.municipalityName()
        );

        Address address = new Address(
            booking,
            form.addressLine().trim(),
            normalize(form.addressComplement()),
            municipalityResolution.municipalityCode(),
            form.municipalityName().trim(),
            municipalityResolution.municipalityResolvedName(),
            municipalityResolution.status() == null ? MunicipalityResolutionStatus.EXACT : municipalityResolution.status(),
            municipalityResolution.note(),
            form.postalCode().trim(),
            form.country().trim().toUpperCase()
        );
        return addressRepository.save(address);
    }

    private void validate(AddressForm form) {
        if (!ISO3_PATTERN.matcher(form.country().trim()).matches()) {
            throw new IllegalArgumentException("El pais debe escribirse con 3 letras, por ejemplo ESP.");
        }
        if (form.municipalityName() == null || form.municipalityName().isBlank()) {
            throw new IllegalArgumentException("Escribe la ciudad o municipio.");
        }
        if ("ESP".equalsIgnoreCase(form.country()) && !SPANISH_POSTAL_CODE_PATTERN.matcher(form.postalCode().trim()).matches()) {
            throw new IllegalArgumentException("Si el pais es ESP, el codigo postal debe tener 5 numeros.");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
