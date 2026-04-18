package es.checkpol.service;

import es.checkpol.infrastructure.xml.TravelerPartXmlGenerator;
import es.checkpol.domain.AppUser;
import es.checkpol.domain.CommunicationDispatchMode;
import es.checkpol.domain.CommunicationDispatchStatus;
import es.checkpol.domain.GeneratedCommunication;
import es.checkpol.repository.GeneratedCommunicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class TravelerPartService {

    private final BookingService bookingService;
    private final TravelerPartXmlGenerator xmlGenerator;
    private final GeneratedCommunicationRepository generatedCommunicationRepository;
    private final CurrentAppUserService currentAppUserService;
    private final SesCommunicationGateway sesCommunicationGateway;

    public TravelerPartService(
        BookingService bookingService,
        TravelerPartXmlGenerator xmlGenerator,
        GeneratedCommunicationRepository generatedCommunicationRepository,
        CurrentAppUserService currentAppUserService,
        SesCommunicationGateway sesCommunicationGateway
    ) {
        this.bookingService = bookingService;
        this.xmlGenerator = xmlGenerator;
        this.generatedCommunicationRepository = generatedCommunicationRepository;
        this.currentAppUserService = currentAppUserService;
        this.sesCommunicationGateway = sesCommunicationGateway;
    }

    @Transactional
    public String generateXml(Long bookingId) {
        BookingDetails details = getReadyDetails(bookingId);
        String xml = xmlGenerator.generate(details);
        return createGeneratedCommunication(
            bookingId,
            currentAppUserService.requireCurrentUserId(),
            details,
            xml,
            CommunicationDispatchMode.MANUAL_DOWNLOAD
        ).getXmlContent();
    }

    @Transactional
    public SesSubmissionResult submitTravelerPart(Long bookingId) {
        AppUser owner = currentAppUserService.requireCurrentUserEntity();
        if (!owner.hasSesWebServiceConfiguration()) {
            throw new IllegalStateException("Falta la configuración del servicio web de SES para este owner.");
        }

        Long ownerId = currentAppUserService.requireCurrentUserId();
        BookingDetails details = getReadyDetails(bookingId);
        String xml = xmlGenerator.generate(details);
        preventIdenticalSesResubmission(bookingId, ownerId, xml);

        GeneratedCommunication communication = createGeneratedCommunication(
            bookingId,
            ownerId,
            details,
            xml,
            CommunicationDispatchMode.SES_WEB_SERVICE
        );
        try {
            SesSubmissionResult result = sesCommunicationGateway.submitTravelerPart(owner, communication.getXmlContent());
            communication.registerSesSubmission(OffsetDateTime.now(), result.loteCode(), result.responseCode(), result.responseDescription(), result.rawResponse());
            return result;
        } catch (RuntimeException exception) {
            SesSubmissionResult result = failedSubmissionResult(exception);
            communication.registerFailedSesSubmission(OffsetDateTime.now(), result.responseCode(), result.responseDescription(), result.rawResponse());
            return result;
        }
    }

    @Transactional
    public SesLoteStatusResult refreshSesSubmissionStatus(Long bookingId, Long communicationId) {
        AppUser owner = currentAppUserService.requireCurrentUserEntity();
        GeneratedCommunication communication = generatedCommunicationRepository.findByIdAndBookingIdAndBookingOwnerId(
                communicationId,
                bookingId,
                currentAppUserService.requireCurrentUserId()
            )
            .orElseThrow(() -> new IllegalArgumentException("La comunicación solicitada no existe para esta estancia."));

        if (communication.getSesLoteCode() == null || communication.getSesLoteCode().isBlank()) {
            throw new IllegalStateException("Esta comunicación todavía no tiene lote de SES para consultar.");
        }
        if (!owner.hasSesWebServiceConfiguration()) {
            throw new IllegalStateException("Falta la configuración del servicio web de SES para este owner.");
        }

        try {
            SesLoteStatusResult result = sesCommunicationGateway.queryLoteStatus(owner, communication.getSesLoteCode());
            communication.registerSesProcessingResult(
                OffsetDateTime.now(),
                result.processingStateCode(),
                result.processingStateDescription(),
                result.communicationCode(),
                result.processingErrorType(),
                result.processingErrorDescription(),
                result.responseCode(),
                result.responseDescription(),
                result.rawResponse()
            );
            return result;
        } catch (RuntimeException exception) {
            SesLoteStatusResult result = failedLoteStatusResult(communication.getSesLoteCode(), exception);
            communication.registerSesResponseNeedsReview(OffsetDateTime.now(), result.responseCode(), result.responseDescription(), result.rawResponse());
            return result;
        }
    }

    @Transactional
    public SesSubmissionResult cancelSesSubmission(Long bookingId, Long communicationId) {
        AppUser owner = currentAppUserService.requireCurrentUserEntity();
        GeneratedCommunication communication = generatedCommunicationRepository.findByIdAndBookingIdAndBookingOwnerId(
                communicationId,
                bookingId,
                currentAppUserService.requireCurrentUserId()
            )
            .orElseThrow(() -> new IllegalArgumentException("La comunicación solicitada no existe para esta estancia."));

        if (communication.getSesLoteCode() == null || communication.getSesLoteCode().isBlank()) {
            throw new IllegalStateException("Esta comunicación todavía no tiene lote de SES para anular.");
        }
        if (!owner.hasSesWebServiceConfiguration()) {
            throw new IllegalStateException("Falta la configuración del servicio web de SES para este owner.");
        }
        if (communication.getDispatchStatus() == CommunicationDispatchStatus.SES_CANCELLED) {
            throw new IllegalStateException("Esta comunicación ya está anulada en SES.");
        }

        SesSubmissionResult result;
        try {
            result = sesCommunicationGateway.cancelLote(owner, communication.getSesLoteCode());
        } catch (RuntimeException exception) {
            result = failedSubmissionResult(exception);
        }
        communication.registerSesCancellation(OffsetDateTime.now(), result.responseCode(), result.responseDescription(), result.rawResponse());
        return result;
    }

    @Transactional
    public String getGeneratedXml(Long bookingId, Long communicationId) {
        GeneratedCommunication communication = generatedCommunicationRepository.findByIdAndBookingIdAndBookingOwnerId(communicationId, bookingId, currentAppUserService.requireCurrentUserId())
            .orElseThrow(() -> new IllegalArgumentException("La comunicación solicitada no existe para esta estancia."))
            ;
        communication.registerDownload(OffsetDateTime.now());
        return communication.getXmlContent();
    }

    private BookingDetails getReadyDetails(Long bookingId) {
        BookingDetails details = bookingService.getDetails(bookingId);
        if (!details.readyForTravelerPart()) {
            throw new IllegalStateException(details.blockingMessage() == null
                ? "La estancia no está lista para generar el archivo SES."
                : details.blockingMessage());
        }
        return details;
    }

    private void preventIdenticalSesResubmission(Long bookingId, Long ownerId, String xml) {
        generatedCommunicationRepository.findFirstByBookingIdAndBookingOwnerIdOrderByGeneratedAtDesc(bookingId, ownerId)
            .filter(communication -> xml.equals(communication.getXmlContent()))
            .filter(this::blocksIdenticalSesResubmission)
            .ifPresent(communication -> {
                throw new IllegalStateException("SES no acepta reenviar exactamente el mismo archivo. Cambia la estancia o algún huésped antes de presentarlo de nuevo.");
            });
    }

    private boolean blocksIdenticalSesResubmission(GeneratedCommunication communication) {
        return communication.getDispatchStatus() == CommunicationDispatchStatus.SES_CANCELLED
            || (communication.getDispatchStatus() == CommunicationDispatchStatus.SUBMISSION_FAILED
                && Integer.valueOf(10121).equals(communication.getSesResponseCode()));
    }

    private GeneratedCommunication createGeneratedCommunication(
        Long bookingId,
        Long ownerId,
        BookingDetails details,
        String xml,
        CommunicationDispatchMode dispatchMode
    ) {
        int nextVersion = generatedCommunicationRepository.findFirstByBookingIdAndBookingOwnerIdOrderByVersionDesc(bookingId, ownerId)
            .map(communication -> communication.getVersion() + 1)
            .orElse(1);
        GeneratedCommunication communication = new GeneratedCommunication(
            details.booking(),
            nextVersion,
            OffsetDateTime.now(),
            xml,
            dispatchMode,
            CommunicationDispatchStatus.XML_READY
        );
        generatedCommunicationRepository.save(communication);
        return communication;
    }

    private SesSubmissionResult failedSubmissionResult(RuntimeException exception) {
        if (exception instanceof SesCommunicationException sesException) {
            return new SesSubmissionResult(
                sesException.getResponseCode() == null ? -1 : sesException.getResponseCode(),
                sesException.getMessage(),
                null,
                sesException.getRawResponse()
            );
        }
        return new SesSubmissionResult(-1, exception.getMessage(), null, null);
    }

    private SesLoteStatusResult failedLoteStatusResult(String loteCode, RuntimeException exception) {
        if (exception instanceof SesCommunicationException sesException) {
            return new SesLoteStatusResult(
                sesException.getResponseCode() == null ? -1 : sesException.getResponseCode(),
                sesException.getMessage(),
                loteCode,
                null,
                null,
                null,
                null,
                sesException.getMessage(),
                null,
                sesException.getRawResponse()
            );
        }
        return new SesLoteStatusResult(-1, exception.getMessage(), loteCode, null, null, null, null, exception.getMessage(), null, null);
    }
}
