package es.checkpol.service;

import es.checkpol.domain.Accommodation;
import es.checkpol.domain.Address;
import es.checkpol.domain.Booking;
import es.checkpol.domain.BookingChannel;
import es.checkpol.domain.MunicipalityResolutionStatus;
import es.checkpol.domain.PaymentType;
import es.checkpol.repository.AddressRepository;
import es.checkpol.repository.BookingRepository;
import es.checkpol.web.AddressForm;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AddressServiceTest {

    private final AddressRepository addressRepository = Mockito.mock(AddressRepository.class);
    private final BookingRepository bookingRepository = Mockito.mock(BookingRepository.class);
    private final MunicipalityResolverService municipalityResolverService = Mockito.mock(MunicipalityResolverService.class);
    private final AddressService addressService = new AddressService(
        addressRepository,
        bookingRepository,
        municipalityResolverService
    );

    @Test
    void createsAddressWithComplementAndAutomaticMunicipalityResolution() {
        Booking booking = sampleBooking();
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(municipalityResolverService.resolve("ESP", "28001", null, "Madrid"))
            .thenReturn(new MunicipalityResolution(
                "28079",
                "Madrid",
                MunicipalityResolutionStatus.EXACT,
                "Municipio resuelto automaticamente.",
                null,
                "Madrid",
                "28"
            ));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        addressService.create(1L, new AddressForm("Calle Mayor 1", "Piso 2B", "28001", "Madrid", "ESP"));

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

    private Booking sampleBooking() {
        return new Booking(
            new Accommodation("Casa Olivo", "H123456789", "VT-123"),
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
