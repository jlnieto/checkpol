package es.checkpol;

import es.checkpol.domain.Accommodation;
import es.checkpol.domain.Address;
import es.checkpol.domain.AppUser;
import es.checkpol.domain.AppUserRole;
import es.checkpol.domain.Booking;
import es.checkpol.domain.BookingChannel;
import es.checkpol.domain.DocumentType;
import es.checkpol.domain.Guest;
import es.checkpol.domain.GuestReviewStatus;
import es.checkpol.domain.GuestSex;
import es.checkpol.domain.GuestSubmissionSource;
import es.checkpol.domain.MunicipalityCatalogEntry;
import es.checkpol.domain.PaymentType;
import es.checkpol.domain.PostalCodeMunicipalityMapping;
import es.checkpol.repository.AccommodationRepository;
import es.checkpol.repository.AddressRepository;
import es.checkpol.repository.AppUserRepository;
import es.checkpol.repository.BookingRepository;
import es.checkpol.repository.GeneratedCommunicationRepository;
import es.checkpol.repository.GuestRepository;
import es.checkpol.repository.MunicipalityCatalogEntryRepository;
import es.checkpol.repository.MunicipalityImportRecordRepository;
import es.checkpol.repository.PostalCodeMunicipalityMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicGuestSelfServiceFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private AccommodationRepository accommodationRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private MunicipalityCatalogEntryRepository municipalityCatalogEntryRepository;

    @Autowired
    private PostalCodeMunicipalityMappingRepository postalCodeMunicipalityMappingRepository;

    @Autowired
    private MunicipalityImportRecordRepository municipalityImportRecordRepository;

    @Autowired
    private GeneratedCommunicationRepository generatedCommunicationRepository;

    @BeforeEach
    void cleanDatabase() {
        generatedCommunicationRepository.deleteAll();
        guestRepository.deleteAll();
        addressRepository.deleteAll();
        bookingRepository.deleteAll();
        accommodationRepository.deleteAll();
        municipalityImportRecordRepository.deleteAll();
        postalCodeMunicipalityMappingRepository.deleteAll();
        municipalityCatalogEntryRepository.deleteAll();
        appUserRepository.deleteAll();
    }

    @Test
    void publicFlowCreatesSpanishAddressAndGuestWithRealSecurityAndPersistence() throws Exception {
        Booking booking = seedBookingWithPublicAccess("public-token-1");
        seedSpanishMunicipalityCatalog("28001", "28079", "Madrid", "28");

        mockMvc.perform(get("/guest-access/public-token-1/addresses/new").param("slot", "1"))
            .andExpect(status().isOk())
            .andExpect(view().name("public/address-form"));

        mockMvc.perform(get("/municipality-catalog/spanish-municipalities").param("postalCode", "28001"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""
                [
                  {
                    "municipalityCode": "28079",
                    "municipalityName": "Madrid",
                    "provinceCode": "28",
                    "provinceName": "Madrid"
                  }
                ]
                """));

        mockMvc.perform(post("/guest-access/public-token-1/addresses")
                .with(csrf())
                .param("slot", "1")
                .param("addressLine", "Calle Mayor 1")
                .param("addressComplement", "Piso 2B")
                .param("postalCode", "28001")
                .param("municipalityCode", "28079")
                .param("country", "ESP"))
            .andExpect(status().is3xxRedirection());

        List<Address> addresses = addressRepository.findAllByBookingIdOrderByIdAsc(booking.getId());
        assertEquals(1, addresses.size());
        Address address = addresses.getFirst();
        assertEquals("28079", address.getMunicipalityCode());
        assertEquals("Madrid", address.getMunicipalityName());
        assertEquals("28001", address.getPostalCode());
        assertEquals("ESP", address.getCountry());

        mockMvc.perform(post("/guest-access/public-token-1/guests")
                .with(csrf())
                .param("slot", "1")
                .param("firstName", "Ana")
                .param("lastName1", "Lopez")
                .param("lastName2", "Martin")
                .param("birthDate", "1990-05-01")
                .param("nationality", "ESP")
                .param("documentType", DocumentType.NIF.name())
                .param("documentNumber", "00000000T")
                .param("documentSupport", "SUP123")
                .param("sex", GuestSex.M.name())
                .param("addressId", address.getId().toString())
                .param("phone", "+34 600000000"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/guest-access/public-token-1"));

        List<Guest> guests = guestRepository.findAllByBookingIdOrderByIdAsc(booking.getId());
        assertEquals(1, guests.size());
        Guest guest = guests.getFirst();
        assertNotNull(guest.getAddressId());
        assertEquals(address.getId(), guest.getAddressId());
        assertEquals(GuestSubmissionSource.SELF_SERVICE, guest.getSubmissionSource());
        assertEquals(GuestReviewStatus.PENDING_REVIEW, guest.getReviewStatus());

        mockMvc.perform(get("/guest-access/public-token-1/guests/" + guest.getId() + "/edit"))
            .andExpect(status().isOk())
            .andExpect(view().name("public/guest-form"));
    }

    private Booking seedBookingWithPublicAccess(String token) {
        OffsetDateTime now = OffsetDateTime.now();
        AppUser owner = appUserRepository.save(new AppUser(
            "owner-int",
            "hash",
            "Owner Integration",
            AppUserRole.OWNER,
            true,
            now,
            now
        ));
        Accommodation accommodation = accommodationRepository.save(new Accommodation(
            owner,
            "Casa Olivo",
            "H123456789",
            "VT-123"
        ));
        Booking booking = bookingRepository.save(new Booking(
            owner,
            accommodation,
            "ABC123",
            1,
            LocalDate.now(),
            BookingChannel.DIRECT,
            LocalDate.now().plusDays(2),
            LocalDate.now().plusDays(4),
            PaymentType.EFECT,
            LocalDate.now(),
            null,
            null,
            null
        ));
        booking.updateSelfServiceAccess(token, OffsetDateTime.now().plusDays(2));
        return bookingRepository.save(booking);
    }

    private void seedSpanishMunicipalityCatalog(String postalCode, String municipalityCode, String municipalityName, String provinceCode) {
        OffsetDateTime now = OffsetDateTime.now();
        municipalityCatalogEntryRepository.save(new MunicipalityCatalogEntry(
            "ESP",
            provinceCode,
            municipalityName,
            municipalityCode,
            municipalityName,
            municipalityName.toLowerCase(),
            true,
            "test",
            "v1",
            now,
            now
        ));
        postalCodeMunicipalityMappingRepository.save(new PostalCodeMunicipalityMapping(
            postalCode,
            municipalityCode,
            true,
            "test",
            "v1",
            now,
            now
        ));
    }
}
