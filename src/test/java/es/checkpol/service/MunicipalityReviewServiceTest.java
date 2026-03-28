package es.checkpol.service;

import es.checkpol.domain.Accommodation;
import es.checkpol.domain.Address;
import es.checkpol.domain.Booking;
import es.checkpol.domain.BookingChannel;
import es.checkpol.domain.Guest;
import es.checkpol.domain.GuestReviewStatus;
import es.checkpol.domain.GuestSex;
import es.checkpol.domain.GuestSubmissionSource;
import es.checkpol.domain.MunicipalityIssueStatus;
import es.checkpol.domain.MunicipalityResolutionIssue;
import es.checkpol.domain.MunicipalityResolutionStatus;
import es.checkpol.domain.PaymentType;
import es.checkpol.repository.GuestRepository;
import es.checkpol.repository.MunicipalityResolutionIssueRepository;
import es.checkpol.repository.MunicipalityResolutionRuleRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MunicipalityReviewServiceTest {

    private final MunicipalityResolutionIssueRepository issueRepository = Mockito.mock(MunicipalityResolutionIssueRepository.class);
    private final MunicipalityResolutionRuleRepository ruleRepository = Mockito.mock(MunicipalityResolutionRuleRepository.class);
    private final GuestRepository guestRepository = Mockito.mock(GuestRepository.class);
    private final MunicipalityReviewService service = new MunicipalityReviewService(issueRepository, ruleRepository, guestRepository);

    @Test
    void derivesNormalizedMunicipalityQueryWhenResolutionDoesNotProvideIt() {
        Guest guest = sampleGuest();
        Mockito.when(issueRepository.findFirstByGuestIdAndIssueStatus(guest.getId(), MunicipalityIssueStatus.OPEN))
            .thenReturn(Optional.empty());

        MunicipalityResolution resolution = guest.getAddress().toMunicipalityResolution();

        service.registerAutomaticResolution(guest, resolution);

        ArgumentCaptor<MunicipalityResolutionIssue> issueCaptor = ArgumentCaptor.forClass(MunicipalityResolutionIssue.class);
        Mockito.verify(issueRepository).save(issueCaptor.capture());

        MunicipalityResolutionIssue savedIssue = issueCaptor.getValue();
        assertEquals("oliva", savedIssue.getMunicipalityQueryNormalized());
        assertEquals("Oliva", savedIssue.getMunicipalityQueryLabel());
    }

    private Guest sampleGuest() {
        Booking booking = new Booking(
            new Accommodation("Casa Olivo", "H123456789", "VT-123"),
            "ABC123",
            2,
            LocalDate.of(2026, 3, 25),
            BookingChannel.DIRECT,
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 4, 3),
            PaymentType.EFECT,
            LocalDate.of(2026, 3, 25),
            null,
            null,
            null
        );

        Address address = new Address(
            booking,
            "Camí Bassetes, 40",
            "Bajo",
            null,
            null,
            null,
            MunicipalityResolutionStatus.APPROXIMATED,
            "Municipio aproximado automaticamente a partir del texto indicado.",
            "35540",
            "ESP"
        );
        ReflectionTestUtils.setField(address, "id", 13L);
        ReflectionTestUtils.setField(address, "municipalityName", "Oliva");
        ReflectionTestUtils.setField(address, "municipalityCode", "35014");
        ReflectionTestUtils.setField(address, "municipalityResolvedName", "La Oliva");

        Guest guest = new Guest(
            booking,
            "Juan",
            "Perez",
            "Lopez",
            es.checkpol.domain.DocumentType.NIF,
            "12345678Z",
            "ABC123456",
            LocalDate.of(1990, 1, 1),
            "ESP",
            GuestSex.M,
            address,
            "+34 600123123",
            "",
            "juan@example.com",
            "",
            GuestSubmissionSource.SELF_SERVICE,
            GuestReviewStatus.PENDING_REVIEW,
            OffsetDateTime.now()
        );
        ReflectionTestUtils.setField(guest, "id", 10L);
        return guest;
    }
}
