package es.checkpol.service;

import es.checkpol.domain.Accommodation;
import es.checkpol.domain.Address;
import es.checkpol.domain.AppUser;
import es.checkpol.domain.AppUserRole;
import es.checkpol.domain.Booking;
import es.checkpol.domain.BookingChannel;
import es.checkpol.domain.MunicipalityCatalogEntry;
import es.checkpol.domain.PaymentType;
import es.checkpol.repository.AddressRepository;
import es.checkpol.repository.BookingRepository;
import es.checkpol.web.AddressForm;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AddressServiceTest {

    private final AddressRepository addressRepository = Mockito.mock(AddressRepository.class);
    private final BookingRepository bookingRepository = Mockito.mock(BookingRepository.class);
    private final MunicipalityCatalogService municipalityCatalogService = Mockito.mock(MunicipalityCatalogService.class);
    private final CurrentAppUserService currentAppUserService = Mockito.mock(CurrentAppUserService.class);
    private final AddressService addressService = new AddressService(
        addressRepository,
        bookingRepository,
        municipalityCatalogService,
        currentAppUserService
    );

    @Test
    void createsSpanishAddressFromCatalogSelection() {
        Booking booking = sampleBooking();
        when(currentAppUserService.requireCurrentUserId()).thenReturn(7L);
        when(bookingRepository.findByIdAndOwnerId(1L, 7L)).thenReturn(Optional.of(booking));
        when(municipalityCatalogService.hasSpanishCatalogData()).thenReturn(true);
        when(municipalityCatalogService.findSpanishMunicipalityByPostalCodeAndCode("28001", "28079"))
            .thenReturn(Optional.of(entry("28", "Madrid", "28079", "Madrid")));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        addressService.create(1L, new AddressForm("28079", "Calle Mayor 1", "Piso 2B", "28001", "", "ESP"));

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        Mockito.verify(addressRepository).save(captor.capture());
        Address saved = captor.getValue();
        assertEquals("Calle Mayor 1", saved.getAddressLine());
        assertEquals("Piso 2B", saved.getAddressComplement());
        assertEquals("28079", saved.getMunicipalityCode());
        assertEquals("Madrid", saved.getMunicipalityName());
        assertEquals("28001", saved.getPostalCode());
        assertEquals("ESP", saved.getCountry());
    }

    @Test
    void prefersMunicipalityCodeWhenItComesFromForm() {
        Booking booking = sampleBooking();
        when(currentAppUserService.requireCurrentUserId()).thenReturn(7L);
        when(bookingRepository.findByIdAndOwnerId(1L, 7L)).thenReturn(Optional.of(booking));
        when(municipalityCatalogService.hasSpanishCatalogData()).thenReturn(true);
        when(municipalityCatalogService.findSpanishMunicipalityByPostalCodeAndCode("28001", "28079"))
            .thenReturn(Optional.of(entry("28", "Madrid", "28079", "Madrid")));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        addressService.create(1L, new AddressForm("28079", "Calle Mayor 1", "Piso 2B", "28001", "", "ESP"));

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        Mockito.verify(addressRepository).save(captor.capture());
        Address saved = captor.getValue();
        assertEquals("28079", saved.getMunicipalityCode());
        assertEquals("Madrid", saved.getMunicipalityName());
    }

    @Test
    void rejectsUnknownMunicipalityCodeWhenLocalCatalogIsLoaded() {
        Booking booking = sampleBooking();
        when(currentAppUserService.requireCurrentUserId()).thenReturn(7L);
        when(bookingRepository.findByIdAndOwnerId(1L, 7L)).thenReturn(Optional.of(booking));
        when(municipalityCatalogService.hasSpanishCatalogData()).thenReturn(true);
        when(municipalityCatalogService.findSpanishMunicipalityByPostalCodeAndCode("28001", "99999")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> addressService.create(1L, new AddressForm("99999", "Calle Mayor 1", "Piso 2B", "28001", "", "ESP"))
        );

        assertEquals("El municipio seleccionado no corresponde a ese código postal.", exception.getMessage());
    }

    @Test
    void rejectsSpanishAddressWhenCatalogIsNotLoaded() {
        Booking booking = sampleBooking();
        when(currentAppUserService.requireCurrentUserId()).thenReturn(7L);
        when(bookingRepository.findByIdAndOwnerId(1L, 7L)).thenReturn(Optional.of(booking));
        when(municipalityCatalogService.hasSpanishCatalogData()).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> addressService.create(1L, new AddressForm("28079", "Calle Mayor 1", "Piso 2B", "28001", "", "ESP"))
        );

        assertEquals("El catálogo de municipios de España no está cargado.", exception.getMessage());
    }

    @Test
    void keepsNonSpanishMunicipalityAsFreeText() {
        Booking booking = sampleBooking();
        when(currentAppUserService.requireCurrentUserId()).thenReturn(7L);
        when(bookingRepository.findByIdAndOwnerId(1L, 7L)).thenReturn(Optional.of(booking));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Address saved = addressService.create(1L, new AddressForm("", "Rue de Rivoli 10", "", "75001", "Paris", "FRA"));

        assertEquals(null, saved.getMunicipalityCode());
        assertEquals("Paris", saved.getMunicipalityName());
        assertEquals("FRA", saved.getCountry());
    }

    private MunicipalityCatalogEntry entry(String provinceCode, String provinceName, String municipalityCode, String municipalityName) {
        return new MunicipalityCatalogEntry(
            "ESP",
            provinceCode,
            provinceName,
            municipalityCode,
            municipalityName,
            municipalityName.toLowerCase(),
            true,
            "test",
            "v1",
            OffsetDateTime.now(),
            OffsetDateTime.now()
        );
    }

    private Booking sampleBooking() {
        AppUser owner = new AppUser("owner", "hash", "Owner", AppUserRole.OWNER, true, OffsetDateTime.now(), OffsetDateTime.now());
        return new Booking(
            owner,
            new Accommodation(owner, "Casa Olivo", "H123456789", "VT-123"),
            "ABC123",
            2,
            LocalDate.now(),
            BookingChannel.DIRECT,
            LocalDate.now().plusDays(2),
            LocalDate.now().plusDays(4),
            PaymentType.EFECT,
            LocalDate.now(),
            null,
            null,
            null
        );
    }
}
