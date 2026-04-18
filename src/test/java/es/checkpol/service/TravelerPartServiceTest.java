package es.checkpol.service;

import es.checkpol.domain.Accommodation;
import es.checkpol.domain.AppUser;
import es.checkpol.domain.AppUserRole;
import es.checkpol.domain.Booking;
import es.checkpol.domain.BookingChannel;
import es.checkpol.domain.CommunicationDispatchStatus;
import es.checkpol.domain.GeneratedCommunication;
import es.checkpol.domain.Guest;
import es.checkpol.domain.PaymentType;
import es.checkpol.infrastructure.xml.TravelerPartXmlGenerator;
import es.checkpol.repository.GeneratedCommunicationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelerPartServiceTest {

    private final BookingService bookingService = Mockito.mock(BookingService.class);
    private final TravelerPartXmlGenerator xmlGenerator = Mockito.mock(TravelerPartXmlGenerator.class);
    private final GeneratedCommunicationRepository generatedCommunicationRepository = Mockito.mock(GeneratedCommunicationRepository.class);
    private final CurrentAppUserService currentAppUserService = Mockito.mock(CurrentAppUserService.class);
    private final SesCommunicationGateway sesCommunicationGateway = Mockito.mock(SesCommunicationGateway.class);
    private final TravelerPartService travelerPartService = new TravelerPartService(
        bookingService,
        xmlGenerator,
        generatedCommunicationRepository,
        currentAppUserService,
        sesCommunicationGateway
    );

    @Test
    void savesGeneratedCommunication() {
        BookingDetails details = sampleDetails(true);
        Mockito.when(bookingService.getDetails(1L)).thenReturn(details);
        Mockito.when(xmlGenerator.generate(details)).thenReturn("<xml/>");
        Mockito.when(currentAppUserService.requireCurrentUserId()).thenReturn(7L);
        Mockito.when(generatedCommunicationRepository.findFirstByBookingIdAndBookingOwnerIdOrderByVersionDesc(1L, 7L)).thenReturn(Optional.empty());

        travelerPartService.generateXml(1L);

        ArgumentCaptor<GeneratedCommunication> captor = ArgumentCaptor.forClass(GeneratedCommunication.class);
        Mockito.verify(generatedCommunicationRepository).save(captor.capture());
        assertTrue(captor.getValue().getXmlContent().contains("<xml/>"));
        assertTrue(captor.getValue().getVersion() == 1);
    }

    @Test
    void rejectsGenerationWhenBookingIsIncomplete() {
        Mockito.when(bookingService.getDetails(1L)).thenReturn(sampleDetails(false));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> travelerPartService.generateXml(1L));
        assertEquals("La estancia no esta lista.", exception.getMessage());
    }

    @Test
    void returnsStoredXmlForExistingCommunication() {
        GeneratedCommunication communication = new GeneratedCommunication(sampleDetails(true).booking(), 2, OffsetDateTime.now(), "<xml/>");
        Mockito.when(currentAppUserService.requireCurrentUserId()).thenReturn(7L);
        Mockito.when(generatedCommunicationRepository.findByIdAndBookingIdAndBookingOwnerId(9L, 1L, 7L))
            .thenReturn(Optional.of(communication));

        String xml = travelerPartService.getGeneratedXml(1L, 9L);

        assertTrue(xml.contains("<xml/>"));
        assertTrue(communication.getDownloadCount() == 1);
    }

    @Test
    void submitsTravelerPartWhenOwnerHasSesConfiguration() {
        BookingDetails details = sampleDetails(true);
        AppUser owner = details.booking().getOwner();
        owner.updateSesWsConfiguration("A123456789", "ws-user", "encrypted");
        Mockito.when(bookingService.getDetails(1L)).thenReturn(details);
        Mockito.when(xmlGenerator.generate(details)).thenReturn("<xml/>");
        Mockito.when(currentAppUserService.requireCurrentUserId()).thenReturn(7L);
        Mockito.when(currentAppUserService.requireCurrentUserEntity()).thenReturn(owner);
        Mockito.when(generatedCommunicationRepository.findFirstByBookingIdAndBookingOwnerIdOrderByVersionDesc(1L, 7L)).thenReturn(Optional.empty());
        Mockito.when(generatedCommunicationRepository.save(Mockito.any(GeneratedCommunication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(sesCommunicationGateway.submitTravelerPart(owner, "<xml/>"))
            .thenReturn(new SesSubmissionResult(0, "ok", "lote-1"));

        SesSubmissionResult result = travelerPartService.submitTravelerPart(1L);

        assertEquals("lote-1", result.loteCode());
        ArgumentCaptor<GeneratedCommunication> captor = ArgumentCaptor.forClass(GeneratedCommunication.class);
        Mockito.verify(generatedCommunicationRepository).save(captor.capture());
        assertEquals(CommunicationDispatchStatus.SUBMITTED_TO_SES, captor.getValue().getDispatchStatus());
        assertEquals("lote-1", captor.getValue().getSesLoteCode());
    }

    @Test
    void marksSubmissionForReviewWhenSesReturnsOkWithoutLote() {
        BookingDetails details = sampleDetails(true);
        AppUser owner = details.booking().getOwner();
        owner.updateSesWsConfiguration("A123456789", "ws-user", "encrypted");
        Mockito.when(bookingService.getDetails(1L)).thenReturn(details);
        Mockito.when(xmlGenerator.generate(details)).thenReturn("<xml/>");
        Mockito.when(currentAppUserService.requireCurrentUserId()).thenReturn(7L);
        Mockito.when(currentAppUserService.requireCurrentUserEntity()).thenReturn(owner);
        Mockito.when(generatedCommunicationRepository.findFirstByBookingIdAndBookingOwnerIdOrderByVersionDesc(1L, 7L)).thenReturn(Optional.empty());
        Mockito.when(generatedCommunicationRepository.save(Mockito.any(GeneratedCommunication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(sesCommunicationGateway.submitTravelerPart(owner, "<xml/>"))
            .thenReturn(new SesSubmissionResult(0, "ok", null, "<soap>ok-sin-lote</soap>"));

        SesSubmissionResult result = travelerPartService.submitTravelerPart(1L);

        assertEquals(0, result.responseCode());
        ArgumentCaptor<GeneratedCommunication> captor = ArgumentCaptor.forClass(GeneratedCommunication.class);
        Mockito.verify(generatedCommunicationRepository).save(captor.capture());
        assertEquals(CommunicationDispatchStatus.SES_RESPONSE_NEEDS_REVIEW, captor.getValue().getDispatchStatus());
        assertEquals("<soap>ok-sin-lote</soap>", captor.getValue().getSesSubmissionRawResponse());
    }

    @Test
    void storesRawResponseWhenSubmissionParserFails() {
        BookingDetails details = sampleDetails(true);
        AppUser owner = details.booking().getOwner();
        owner.updateSesWsConfiguration("A123456789", "ws-user", "encrypted");
        Mockito.when(bookingService.getDetails(1L)).thenReturn(details);
        Mockito.when(xmlGenerator.generate(details)).thenReturn("<xml/>");
        Mockito.when(currentAppUserService.requireCurrentUserId()).thenReturn(7L);
        Mockito.when(currentAppUserService.requireCurrentUserEntity()).thenReturn(owner);
        Mockito.when(generatedCommunicationRepository.findFirstByBookingIdAndBookingOwnerIdOrderByVersionDesc(1L, 7L)).thenReturn(Optional.empty());
        Mockito.when(generatedCommunicationRepository.save(Mockito.any(GeneratedCommunication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(sesCommunicationGateway.submitTravelerPart(owner, "<xml/>"))
            .thenThrow(new SesCommunicationException("No he podido interpretar la respuesta de envío de SES.", null, "<soap>respuesta-rara</soap>"));

        SesSubmissionResult result = travelerPartService.submitTravelerPart(1L);

        assertEquals(-1, result.responseCode());
        ArgumentCaptor<GeneratedCommunication> captor = ArgumentCaptor.forClass(GeneratedCommunication.class);
        Mockito.verify(generatedCommunicationRepository).save(captor.capture());
        assertEquals(CommunicationDispatchStatus.SUBMISSION_FAILED, captor.getValue().getDispatchStatus());
        assertEquals("<soap>respuesta-rara</soap>", captor.getValue().getSesSubmissionRawResponse());
    }

    @Test
    void blocksIdenticalSesResubmissionAfterCancellation() {
        BookingDetails details = sampleDetails(true);
        AppUser owner = details.booking().getOwner();
        owner.updateSesWsConfiguration("A123456789", "ws-user", "encrypted");
        GeneratedCommunication previous = new GeneratedCommunication(details.booking(), 1, OffsetDateTime.now(), "<xml/>");
        previous.registerSesSubmission(OffsetDateTime.now(), "lote-1", 0, "ok");
        previous.registerSesCancellation(OffsetDateTime.now(), 0, "Anulada");
        Mockito.when(bookingService.getDetails(1L)).thenReturn(details);
        Mockito.when(xmlGenerator.generate(details)).thenReturn("<xml/>");
        Mockito.when(currentAppUserService.requireCurrentUserId()).thenReturn(7L);
        Mockito.when(currentAppUserService.requireCurrentUserEntity()).thenReturn(owner);
        Mockito.when(generatedCommunicationRepository.findFirstByBookingIdAndBookingOwnerIdOrderByGeneratedAtDesc(1L, 7L))
            .thenReturn(Optional.of(previous));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> travelerPartService.submitTravelerPart(1L));

        assertEquals("SES no acepta reenviar exactamente el mismo archivo. Cambia la estancia o algún huésped antes de presentarlo de nuevo.", exception.getMessage());
        Mockito.verify(sesCommunicationGateway, Mockito.never()).submitTravelerPart(Mockito.any(), Mockito.anyString());
        Mockito.verify(generatedCommunicationRepository, Mockito.never()).save(Mockito.any(GeneratedCommunication.class));
    }

    @Test
    void blocksRepeatedSesDuplicateFailureWithSameXml() {
        BookingDetails details = sampleDetails(true);
        AppUser owner = details.booking().getOwner();
        owner.updateSesWsConfiguration("A123456789", "ws-user", "encrypted");
        GeneratedCommunication previous = new GeneratedCommunication(details.booking(), 2, OffsetDateTime.now(), "<xml/>");
        previous.registerFailedSesSubmission(OffsetDateTime.now(), 10121, "Lote duplicado");
        Mockito.when(bookingService.getDetails(1L)).thenReturn(details);
        Mockito.when(xmlGenerator.generate(details)).thenReturn("<xml/>");
        Mockito.when(currentAppUserService.requireCurrentUserId()).thenReturn(7L);
        Mockito.when(currentAppUserService.requireCurrentUserEntity()).thenReturn(owner);
        Mockito.when(generatedCommunicationRepository.findFirstByBookingIdAndBookingOwnerIdOrderByGeneratedAtDesc(1L, 7L))
            .thenReturn(Optional.of(previous));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> travelerPartService.submitTravelerPart(1L));

        assertTrue(exception.getMessage().contains("SES no acepta reenviar exactamente el mismo archivo"));
        Mockito.verify(sesCommunicationGateway, Mockito.never()).submitTravelerPart(Mockito.any(), Mockito.anyString());
        Mockito.verify(generatedCommunicationRepository, Mockito.never()).save(Mockito.any(GeneratedCommunication.class));
    }

    @Test
    void refreshesSesSubmissionStatusFromLote() {
        BookingDetails details = sampleDetails(true);
        AppUser owner = details.booking().getOwner();
        owner.updateSesWsConfiguration("A123456789", "ws-user", "encrypted");
        GeneratedCommunication communication = new GeneratedCommunication(details.booking(), 1, OffsetDateTime.now(), "<xml/>");
        communication.registerSesSubmission(OffsetDateTime.now(), "lote-1", 0, "ok");
        Mockito.when(currentAppUserService.requireCurrentUserEntity()).thenReturn(owner);
        Mockito.when(currentAppUserService.requireCurrentUserId()).thenReturn(7L);
        Mockito.when(generatedCommunicationRepository.findByIdAndBookingIdAndBookingOwnerId(9L, 1L, 7L))
            .thenReturn(Optional.of(communication));
        Mockito.when(sesCommunicationGateway.queryLoteStatus(owner, "lote-1"))
            .thenReturn(new SesLoteStatusResult(0, "ok", "lote-1", 0, "Procesado", "com-1", null, null, OffsetDateTime.now()));

        SesLoteStatusResult result = travelerPartService.refreshSesSubmissionStatus(1L, 9L);

        assertEquals("com-1", result.communicationCode());
        assertEquals(CommunicationDispatchStatus.SES_PROCESSED, communication.getDispatchStatus());
        assertEquals("com-1", communication.getSesCommunicationCode());
    }

    @Test
    void marksStatusResponseForReviewWhenSesReturnsNonZeroCode() {
        BookingDetails details = sampleDetails(true);
        AppUser owner = details.booking().getOwner();
        owner.updateSesWsConfiguration("A123456789", "ws-user", "encrypted");
        GeneratedCommunication communication = new GeneratedCommunication(details.booking(), 1, OffsetDateTime.now(), "<xml/>");
        communication.registerSesSubmission(OffsetDateTime.now(), "lote-1", 0, "ok");
        Mockito.when(currentAppUserService.requireCurrentUserEntity()).thenReturn(owner);
        Mockito.when(currentAppUserService.requireCurrentUserId()).thenReturn(7L);
        Mockito.when(generatedCommunicationRepository.findByIdAndBookingIdAndBookingOwnerId(9L, 1L, 7L))
            .thenReturn(Optional.of(communication));
        Mockito.when(sesCommunicationGateway.queryLoteStatus(owner, "lote-1"))
            .thenReturn(new SesLoteStatusResult(10199, "Error consultando lote", "lote-1", null, null, null, null, null, null, "<soap>error</soap>"));

        SesLoteStatusResult result = travelerPartService.refreshSesSubmissionStatus(1L, 9L);

        assertEquals(10199, result.responseCode());
        assertEquals(CommunicationDispatchStatus.SES_RESPONSE_NEEDS_REVIEW, communication.getDispatchStatus());
        assertEquals("Error consultando lote", communication.getSesResponseDescription());
        assertEquals("<soap>error</soap>", communication.getSesStatusRawResponse());
    }

    @Test
    void storesRawStatusResponseWhenLoteParserFails() {
        BookingDetails details = sampleDetails(true);
        AppUser owner = details.booking().getOwner();
        owner.updateSesWsConfiguration("A123456789", "ws-user", "encrypted");
        GeneratedCommunication communication = new GeneratedCommunication(details.booking(), 1, OffsetDateTime.now(), "<xml/>");
        communication.registerSesSubmission(OffsetDateTime.now(), "lote-1", 0, "ok");
        Mockito.when(currentAppUserService.requireCurrentUserEntity()).thenReturn(owner);
        Mockito.when(currentAppUserService.requireCurrentUserId()).thenReturn(7L);
        Mockito.when(generatedCommunicationRepository.findByIdAndBookingIdAndBookingOwnerId(9L, 1L, 7L))
            .thenReturn(Optional.of(communication));
        Mockito.when(sesCommunicationGateway.queryLoteStatus(owner, "lote-1"))
            .thenThrow(new SesCommunicationException("No he podido interpretar la respuesta de consulta de lote de SES.", null, "<soap>estado-raro</soap>"));

        SesLoteStatusResult result = travelerPartService.refreshSesSubmissionStatus(1L, 9L);

        assertEquals(-1, result.responseCode());
        assertEquals(CommunicationDispatchStatus.SES_RESPONSE_NEEDS_REVIEW, communication.getDispatchStatus());
        assertEquals("<soap>estado-raro</soap>", communication.getSesStatusRawResponse());
    }

    @Test
    void cancelsSesSubmissionByLote() {
        BookingDetails details = sampleDetails(true);
        AppUser owner = details.booking().getOwner();
        owner.updateSesWsConfiguration("A123456789", "ws-user", "encrypted");
        GeneratedCommunication communication = new GeneratedCommunication(details.booking(), 1, OffsetDateTime.now(), "<xml/>");
        communication.registerSesSubmission(OffsetDateTime.now(), "lote-1", 0, "ok");
        Mockito.when(currentAppUserService.requireCurrentUserEntity()).thenReturn(owner);
        Mockito.when(currentAppUserService.requireCurrentUserId()).thenReturn(7L);
        Mockito.when(generatedCommunicationRepository.findByIdAndBookingIdAndBookingOwnerId(9L, 1L, 7L))
            .thenReturn(Optional.of(communication));
        Mockito.when(sesCommunicationGateway.cancelLote(owner, "lote-1"))
            .thenReturn(new SesSubmissionResult(0, "ok", null));

        SesSubmissionResult result = travelerPartService.cancelSesSubmission(1L, 9L);

        assertEquals(0, result.responseCode());
        assertEquals(CommunicationDispatchStatus.SES_CANCELLED, communication.getDispatchStatus());
    }

    @Test
    void storesRawCancellationResponseWhenCancellationIsRejected() {
        BookingDetails details = sampleDetails(true);
        AppUser owner = details.booking().getOwner();
        owner.updateSesWsConfiguration("A123456789", "ws-user", "encrypted");
        GeneratedCommunication communication = new GeneratedCommunication(details.booking(), 1, OffsetDateTime.now(), "<xml/>");
        communication.registerSesSubmission(OffsetDateTime.now(), "lote-1", 0, "ok");
        Mockito.when(currentAppUserService.requireCurrentUserEntity()).thenReturn(owner);
        Mockito.when(currentAppUserService.requireCurrentUserId()).thenReturn(7L);
        Mockito.when(generatedCommunicationRepository.findByIdAndBookingIdAndBookingOwnerId(9L, 1L, 7L))
            .thenReturn(Optional.of(communication));
        Mockito.when(sesCommunicationGateway.cancelLote(owner, "lote-1"))
            .thenReturn(new SesSubmissionResult(12, "Lote no anulable", null, "<soap>no-anulado</soap>"));

        SesSubmissionResult result = travelerPartService.cancelSesSubmission(1L, 9L);

        assertEquals(12, result.responseCode());
        assertEquals(CommunicationDispatchStatus.SUBMITTED_TO_SES, communication.getDispatchStatus());
        assertEquals("<soap>no-anulado</soap>", communication.getSesCancellationRawResponse());
    }

    private BookingDetails sampleDetails(boolean ready) {
        AppUser owner = new AppUser("owner", "hash", "Owner", AppUserRole.OWNER, true, OffsetDateTime.now(), OffsetDateTime.now());
        org.springframework.test.util.ReflectionTestUtils.setField(owner, "id", 7L);
        Accommodation accommodation = new Accommodation(owner, "Casa Olivo", "H123456789", "VT-123");
        Booking booking = new Booking(
            owner,
            accommodation,
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
        return new BookingDetails(
            booking,
            List.<Guest>of(),
            0,
            2,
            false,
            ready,
            false,
            Optional.empty(),
            0,
            List.of(),
            Optional.empty(),
            ready ? BookingOperationalStatus.READY_FOR_XML : BookingOperationalStatus.INCOMPLETE,
            0,
            0,
            !ready,
            !ready,
            false,
            false,
            ready ? null : "La estancia no esta lista.",
            ready ? null : "La estancia no esta lista.",
            ready ? List.of() : List.of("La estancia no esta lista.")
        );
    }
}
