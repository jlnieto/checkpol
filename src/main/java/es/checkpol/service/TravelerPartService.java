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
        return createGeneratedCommunication(bookingId, CommunicationDispatchMode.MANUAL_DOWNLOAD).getXmlContent();
    }

    @Transactional
    public SesSubmissionResult submitTravelerPart(Long bookingId) {
        AppUser owner = currentAppUserService.requireCurrentUserEntity();
        if (!owner.hasSesWebServiceConfiguration()) {
            throw new IllegalStateException("Falta la configuración del servicio web de SES para este owner.");
        }

        GeneratedCommunication communication = createGeneratedCommunication(bookingId, CommunicationDispatchMode.SES_WEB_SERVICE);
        try {
            SesSubmissionResult result = sesCommunicationGateway.submitTravelerPart(owner, communication.getXmlContent());
            communication.registerSesSubmission(OffsetDateTime.now(), result.loteCode(), result.responseCode(), result.responseDescription());
            return result;
        } catch (RuntimeException exception) {
            communication.registerFailedSesSubmission(OffsetDateTime.now(), null, exception.getMessage());
            throw exception;
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

        SesLoteStatusResult result = sesCommunicationGateway.queryLoteStatus(owner, communication.getSesLoteCode());
        communication.registerSesProcessingResult(
            OffsetDateTime.now(),
            result.processingStateCode(),
            result.processingStateDescription(),
            result.communicationCode(),
            result.processingErrorType(),
            result.processingErrorDescription()
        );
        return result;
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

        SesSubmissionResult result = sesCommunicationGateway.cancelLote(owner, communication.getSesLoteCode());
        if (result.responseCode() == 0) {
            communication.registerSesCancellation(OffsetDateTime.now(), result.responseCode(), result.responseDescription());
        }
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

    private GeneratedCommunication createGeneratedCommunication(Long bookingId, CommunicationDispatchMode dispatchMode) {
        BookingDetails details = bookingService.getDetails(bookingId);
        if (!details.readyForTravelerPart()) {
            throw new IllegalStateException(details.blockingMessage() == null
                ? "La estancia no está lista para generar el archivo SES."
                : details.blockingMessage());
        }
        String xml = xmlGenerator.generate(details);
        int nextVersion = generatedCommunicationRepository.findFirstByBookingIdAndBookingOwnerIdOrderByVersionDesc(bookingId, currentAppUserService.requireCurrentUserId())
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
}
