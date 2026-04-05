package es.checkpol.service;

import es.checkpol.domain.Address;
import es.checkpol.domain.Booking;
import es.checkpol.domain.MunicipalityCatalogEntry;
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
    private final MunicipalityCatalogService municipalityCatalogService;
    private final CurrentAppUserService currentAppUserService;

    public AddressService(
        AddressRepository addressRepository,
        BookingRepository bookingRepository,
        MunicipalityCatalogService municipalityCatalogService,
        CurrentAppUserService currentAppUserService
    ) {
        this.addressRepository = addressRepository;
        this.bookingRepository = bookingRepository;
        this.municipalityCatalogService = municipalityCatalogService;
        this.currentAppUserService = currentAppUserService;
    }

    @Transactional(readOnly = true)
    public List<Address> findByBookingId(Long bookingId) {
        return addressRepository.findAllByBookingIdAndBookingOwnerIdOrderByIdAsc(bookingId, currentAppUserService.requireCurrentUserId());
    }

    @Transactional(readOnly = true)
    public Address getByIdAndBookingId(Long addressId, Long bookingId) {
        return addressRepository.findByIdAndBookingIdAndBookingOwnerId(addressId, bookingId, currentAppUserService.requireCurrentUserId())
            .orElseThrow(() -> new IllegalArgumentException("La dirección seleccionada no pertenece a esta estancia."));
    }

    @Transactional
    public Address create(Long bookingId, AddressForm form) {
        Booking booking = bookingRepository.findByIdAndOwnerId(bookingId, currentAppUserService.requireCurrentUserId())
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa estancia."));
        return createForBooking(booking, form);
    }

    @Transactional
    public Address createForSelfService(Long bookingId, AddressForm form) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa estancia."));
        return createForBooking(booking, form);
    }

    private Address createForBooking(Booking booking, AddressForm form) {
        validate(form);
        String normalizedCountry = form.country().trim().toUpperCase();
        String municipalityCode = null;
        String municipalityName;

        if ("ESP".equals(normalizedCountry)) {
            MunicipalityCatalogEntry municipality = resolveSpanishMunicipality(form);
            municipalityCode = municipality.getMunicipalityCode();
            municipalityName = municipality.getMunicipalityName();
        } else {
            municipalityName = form.municipalityName().trim();
        }

        Address address = new Address(
            booking,
            form.addressLine().trim(),
            normalize(form.addressComplement()),
            municipalityCode,
            municipalityName,
            form.postalCode().trim(),
            normalizedCountry
        );
        return addressRepository.save(address);
    }

    private void validate(AddressForm form) {
        if (!ISO3_PATTERN.matcher(form.country().trim()).matches()) {
            throw new IllegalArgumentException("El país debe escribirse con 3 letras, por ejemplo ESP.");
        }
        if ("ESP".equalsIgnoreCase(form.country()) && !SPANISH_POSTAL_CODE_PATTERN.matcher(form.postalCode().trim()).matches()) {
            throw new IllegalArgumentException("Si el país es ESP, el código postal debe tener 5 números.");
        }
        if ("ESP".equalsIgnoreCase(form.country())) {
            if (!municipalityCatalogService.hasSpanishCatalogData()) {
                throw new IllegalArgumentException("El catálogo de municipios de España no está cargado.");
            }
            if (form.municipalityCode() == null || form.municipalityCode().isBlank()) {
                throw new IllegalArgumentException("Selecciona un municipio para ese código postal.");
            }
            if (form.municipalityCode() != null && !form.municipalityCode().isBlank() && !form.municipalityCode().trim().matches("\\d{5}")) {
                throw new IllegalArgumentException("El código de municipio debe tener 5 números.");
            }
            return;
        }
        if (form.municipalityName() == null || form.municipalityName().isBlank()) {
            throw new IllegalArgumentException("Escribe la ciudad o municipio.");
        }
    }

    private MunicipalityCatalogEntry resolveSpanishMunicipality(AddressForm form) {
        return municipalityCatalogService
            .findSpanishMunicipalityByPostalCodeAndCode(form.postalCode().trim(), normalizeMunicipalityCode(form.municipalityCode()))
            .orElseThrow(() -> new IllegalArgumentException("El municipio seleccionado no corresponde a ese código postal."));
    }

    private String normalizeMunicipalityCode(String municipalityCode) {
        if (municipalityCode == null || municipalityCode.isBlank()) {
            return null;
        }
        return municipalityCode.trim();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
