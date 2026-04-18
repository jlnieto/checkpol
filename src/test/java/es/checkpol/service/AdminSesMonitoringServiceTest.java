package es.checkpol.service;

import es.checkpol.domain.Accommodation;
import es.checkpol.domain.AppUser;
import es.checkpol.domain.AppUserRole;
import es.checkpol.domain.Booking;
import es.checkpol.domain.BookingChannel;
import es.checkpol.domain.CommunicationDispatchMode;
import es.checkpol.domain.CommunicationDispatchStatus;
import es.checkpol.domain.GeneratedCommunication;
import es.checkpol.domain.PaymentType;
import es.checkpol.infrastructure.ses.SesWsSslContextFactory;
import es.checkpol.repository.AppUserRepository;
import es.checkpol.repository.GeneratedCommunicationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.DefaultResourceLoader;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminSesMonitoringServiceTest {

    private final GeneratedCommunicationRepository generatedCommunicationRepository = mock(GeneratedCommunicationRepository.class);
    private final AppUserRepository appUserRepository = mock(AppUserRepository.class);
    private final SesCommunicationGateway sesCommunicationGateway = mock(SesCommunicationGateway.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<SesSubmissionPollingScheduler> pollingSchedulerProvider = mock(ObjectProvider.class);

    @Test
    void buildsSesDashboardSummary() {
        when(appUserRepository.findAllByRoleOrderByDisplayNameAscUsernameAsc(AppUserRole.OWNER)).thenReturn(List.of(
            owner("joana", true, true),
            owner("mario", true, false),
            owner("ana", false, false)
        ));
        when(generatedCommunicationRepository.countByDispatchStatus(CommunicationDispatchStatus.SUBMITTED_TO_SES)).thenReturn(2L);
        when(generatedCommunicationRepository.countByDispatchStatusAndSesProblemReviewedAtIsNull(CommunicationDispatchStatus.SUBMISSION_FAILED)).thenReturn(1L);
        when(generatedCommunicationRepository.countByDispatchStatusAndSesProblemReviewedAtIsNull(CommunicationDispatchStatus.SES_PROCESSING_ERROR)).thenReturn(2L);
        when(generatedCommunicationRepository.countByDispatchStatusAndSesProblemReviewedAtIsNull(CommunicationDispatchStatus.SES_RESPONSE_NEEDS_REVIEW)).thenReturn(1L);
        when(pollingSchedulerProvider.getIfAvailable()).thenReturn(new SesSubmissionPollingScheduler(generatedCommunicationRepository, sesCommunicationGateway));

        AdminSesMonitoringService service = new AdminSesMonitoringService(
            generatedCommunicationRepository,
            appUserRepository,
            sesCommunicationGateway,
            new DefaultResourceLoader(),
            new SesWsSslContextFactory(),
            pollingSchedulerProvider,
            "https://pre-ses",
            "",
            "",
            "PKCS12",
            60000
        );

        AdminSesMonitoringService.SesDashboardSummary summary = service.getDashboardSummary();

        assertEquals(2, summary.pendingCount());
        assertEquals(1, summary.submissionErrorCount());
        assertEquals(3, summary.processingErrorCount());
        assertEquals(1, summary.ownersWithoutReadyWsCount());
        assertEquals("Parcial", summary.technicalHealth().overallLabel());
    }

    @Test
    void refreshesCommunicationFromAdmin() {
        AppUser owner = owner("joana", true, true);
        GeneratedCommunication communication = submittedCommunication(owner);
        when(generatedCommunicationRepository.findAdminDetailById(5L)).thenReturn(Optional.of(communication));
        when(sesCommunicationGateway.queryLoteStatus(owner, "L-123")).thenReturn(new SesLoteStatusResult(
            0,
            "OK",
            "L-123",
            1,
            "Procesado",
            "COM-99",
            null,
            null,
            OffsetDateTime.now()
        ));
        when(pollingSchedulerProvider.getIfAvailable()).thenReturn(new SesSubmissionPollingScheduler(generatedCommunicationRepository, sesCommunicationGateway));

        AdminSesMonitoringService service = new AdminSesMonitoringService(
            generatedCommunicationRepository,
            appUserRepository,
            sesCommunicationGateway,
            new DefaultResourceLoader(),
            new SesWsSslContextFactory(),
            pollingSchedulerProvider,
            "https://pre-ses",
            "",
            "",
            "PKCS12",
            60000
        );

        SesLoteStatusResult result = service.refreshCommunication(5L);

        assertEquals("COM-99", result.communicationCode());
        assertEquals(CommunicationDispatchStatus.SES_PROCESSED, communication.getDispatchStatus());
        assertEquals("COM-99", communication.getSesCommunicationCode());
    }

    @Test
    void retriesFailedCommunicationFromAdmin() {
        AppUser owner = owner("joana", true, true);
        GeneratedCommunication source = failedCommunication(owner);
        when(generatedCommunicationRepository.findAdminDetailById(5L)).thenReturn(Optional.of(source));
        when(generatedCommunicationRepository.findFirstByBookingIdOrderByVersionDesc(source.getBooking().getId())).thenReturn(Optional.of(source));
        when(sesCommunicationGateway.submitTravelerPart(owner, source.getXmlContent())).thenReturn(new SesSubmissionResult(0, "OK", "L-999"));
        when(pollingSchedulerProvider.getIfAvailable()).thenReturn(new SesSubmissionPollingScheduler(generatedCommunicationRepository, sesCommunicationGateway));

        AdminSesMonitoringService service = new AdminSesMonitoringService(
            generatedCommunicationRepository,
            appUserRepository,
            sesCommunicationGateway,
            new DefaultResourceLoader(),
            new SesWsSslContextFactory(),
            pollingSchedulerProvider,
            "https://pre-ses",
            "",
            "",
            "PKCS12",
            60000
        );

        GeneratedCommunication retry = service.retryCommunication(5L);

        assertNotNull(retry);
        assertEquals(2, retry.getVersion());
        assertEquals(CommunicationDispatchStatus.SUBMITTED_TO_SES, retry.getDispatchStatus());
        assertEquals("L-999", retry.getSesLoteCode());
    }

    @Test
    void doesNotOfferRetryForDuplicateSubmissionFailure() {
        AppUser owner = owner("joana", true, true);
        GeneratedCommunication source = duplicateFailedCommunication(owner);
        when(generatedCommunicationRepository.findTop100ByDispatchStatusInAndSesProblemReviewedAtIsNullOrderByGeneratedAtDesc(List.of(
            CommunicationDispatchStatus.SES_RESPONSE_NEEDS_REVIEW,
            CommunicationDispatchStatus.SUBMISSION_FAILED,
            CommunicationDispatchStatus.SES_PROCESSING_ERROR
        ))).thenReturn(List.of(source));
        when(pollingSchedulerProvider.getIfAvailable()).thenReturn(new SesSubmissionPollingScheduler(generatedCommunicationRepository, sesCommunicationGateway));

        AdminSesMonitoringService service = new AdminSesMonitoringService(
            generatedCommunicationRepository,
            appUserRepository,
            sesCommunicationGateway,
            new DefaultResourceLoader(),
            new SesWsSslContextFactory(),
            pollingSchedulerProvider,
            "https://pre-ses",
            "",
            "",
            "PKCS12",
            60000
        );

        List<AdminSesMonitoringService.SesCommunicationRow> rows = service.findRecentCommunications(null, true);

        assertEquals(1, rows.size());
        assertFalse(rows.getFirst().canRetry());
    }

    @Test
    void marksSesProblemAsReviewed() {
        AppUser owner = owner("joana", true, true);
        GeneratedCommunication source = failedCommunication(owner);
        when(generatedCommunicationRepository.findAdminDetailById(5L)).thenReturn(Optional.of(source));
        when(pollingSchedulerProvider.getIfAvailable()).thenReturn(new SesSubmissionPollingScheduler(generatedCommunicationRepository, sesCommunicationGateway));

        AdminSesMonitoringService service = new AdminSesMonitoringService(
            generatedCommunicationRepository,
            appUserRepository,
            sesCommunicationGateway,
            new DefaultResourceLoader(),
            new SesWsSslContextFactory(),
            pollingSchedulerProvider,
            "https://pre-ses",
            "",
            "",
            "PKCS12",
            60000
        );

        service.markCommunicationProblemReviewed(5L, "admin");

        assertNotNull(source.getSesProblemReviewedAt());
        assertEquals("admin", source.getSesProblemReviewedBy());
        assertFalse(source.canMarkSesProblemReviewed());
    }

    @Test
    void rejectsAdminRetryForDuplicateSubmissionFailure() {
        AppUser owner = owner("joana", true, true);
        GeneratedCommunication source = duplicateFailedCommunication(owner);
        when(generatedCommunicationRepository.findAdminDetailById(5L)).thenReturn(Optional.of(source));
        when(pollingSchedulerProvider.getIfAvailable()).thenReturn(new SesSubmissionPollingScheduler(generatedCommunicationRepository, sesCommunicationGateway));

        AdminSesMonitoringService service = new AdminSesMonitoringService(
            generatedCommunicationRepository,
            appUserRepository,
            sesCommunicationGateway,
            new DefaultResourceLoader(),
            new SesWsSslContextFactory(),
            pollingSchedulerProvider,
            "https://pre-ses",
            "",
            "",
            "PKCS12",
            60000
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.retryCommunication(5L));

        assertEquals("No reintentes este envío desde admin: SES ya lo ha marcado como lote duplicado. Corrige la estancia o los huéspedes antes de generar un nuevo parte.", exception.getMessage());
        verify(sesCommunicationGateway, never()).submitTravelerPart(owner, source.getXmlContent());
    }

    private AppUser owner(String username, boolean active, boolean withWsConfiguration) {
        OffsetDateTime now = OffsetDateTime.parse("2026-04-04T10:00:00Z");
        return withWsConfiguration
            ? new AppUser(username, "hash", "Owner " + username, "A123456789", username + "-ws", "encrypted", AppUserRole.OWNER, active, now, now)
            : new AppUser(username, "hash", "Owner " + username, AppUserRole.OWNER, active, now, now);
    }

    private GeneratedCommunication submittedCommunication(AppUser owner) {
        Booking booking = booking(owner);
        GeneratedCommunication communication = new GeneratedCommunication(
            booking,
            1,
            OffsetDateTime.parse("2026-04-04T10:00:00Z"),
            "<xml/>",
            CommunicationDispatchMode.SES_WEB_SERVICE,
            CommunicationDispatchStatus.XML_READY
        );
        communication.registerSesSubmission(OffsetDateTime.parse("2026-04-04T10:05:00Z"), "L-123", 0, "OK");
        return communication;
    }

    private GeneratedCommunication failedCommunication(AppUser owner) {
        Booking booking = booking(owner);
        GeneratedCommunication communication = new GeneratedCommunication(
            booking,
            1,
            OffsetDateTime.parse("2026-04-04T10:00:00Z"),
            "<xml/>",
            CommunicationDispatchMode.SES_WEB_SERVICE,
            CommunicationDispatchStatus.XML_READY
        );
        communication.registerFailedSesSubmission(OffsetDateTime.parse("2026-04-04T10:05:00Z"), null, "timeout");
        return communication;
    }

    private GeneratedCommunication duplicateFailedCommunication(AppUser owner) {
        Booking booking = booking(owner);
        GeneratedCommunication communication = new GeneratedCommunication(
            booking,
            1,
            OffsetDateTime.parse("2026-04-04T10:00:00Z"),
            "<xml/>",
            CommunicationDispatchMode.SES_WEB_SERVICE,
            CommunicationDispatchStatus.XML_READY
        );
        communication.registerFailedSesSubmission(OffsetDateTime.parse("2026-04-04T10:05:00Z"), 10121, "Lote duplicado");
        return communication;
    }

    private Booking booking(AppUser owner) {
        Accommodation accommodation = new Accommodation(owner, "Apartamento Centro", "ES12345678", "VT-1");
        return new Booking(
            owner,
            accommodation,
            "CHK-1",
            2,
            LocalDate.of(2026, 4, 4),
            BookingChannel.DIRECT,
            LocalDate.of(2026, 4, 10),
            LocalDate.of(2026, 4, 12),
            PaymentType.EFECT,
            LocalDate.of(2026, 4, 4),
            "EFECTIVO",
            "Joana",
            null
        );
    }
}
