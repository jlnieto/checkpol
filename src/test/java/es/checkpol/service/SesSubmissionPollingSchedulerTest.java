package es.checkpol.service;

import es.checkpol.domain.Accommodation;
import es.checkpol.domain.AppUser;
import es.checkpol.domain.AppUserRole;
import es.checkpol.domain.Booking;
import es.checkpol.domain.BookingChannel;
import es.checkpol.domain.CommunicationDispatchStatus;
import es.checkpol.domain.GeneratedCommunication;
import es.checkpol.domain.PaymentType;
import es.checkpol.repository.GeneratedCommunicationRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SesSubmissionPollingSchedulerTest {

    @Test
    void refreshesPendingSesSubmissions() {
        GeneratedCommunicationRepository repository = mock(GeneratedCommunicationRepository.class);
        SesCommunicationGateway gateway = mock(SesCommunicationGateway.class);
        SesSubmissionPollingScheduler scheduler = new SesSubmissionPollingScheduler(repository, gateway);

        AppUser owner = new AppUser("owner", "hash", "Owner", AppUserRole.OWNER, true, OffsetDateTime.now(), OffsetDateTime.now());
        owner.updateSesWsConfiguration("A123456789", "ws-user", "encrypted");
        Booking booking = new Booking(
            owner,
            new Accommodation(owner, "Casa Olivo", "H123456789", "VT-123"),
            "ABC123",
            1,
            LocalDate.now(),
            BookingChannel.DIRECT,
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(2),
            PaymentType.EFECT,
            LocalDate.now(),
            null,
            null,
            null
        );
        GeneratedCommunication communication = new GeneratedCommunication(booking, 1, OffsetDateTime.now(), "<xml/>");
        communication.registerSesSubmission(OffsetDateTime.now(), "lote-1", 0, "ok");

        when(repository.findTop25ByDispatchStatusOrderBySubmittedAtAsc(CommunicationDispatchStatus.SUBMITTED_TO_SES))
            .thenReturn(List.of(communication));
        when(gateway.queryLoteStatus(owner, "lote-1"))
            .thenReturn(new SesLoteStatusResult(0, "ok", "lote-1", 0, "Procesado", "com-1", null, null, OffsetDateTime.now()));

        scheduler.refreshPendingSesSubmissions();

        verify(gateway).queryLoteStatus(owner, "lote-1");
        assertEquals(CommunicationDispatchStatus.SES_PROCESSED, communication.getDispatchStatus());
        assertEquals("com-1", communication.getSesCommunicationCode());
    }
}
