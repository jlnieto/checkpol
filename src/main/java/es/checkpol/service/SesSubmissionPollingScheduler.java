package es.checkpol.service;

import es.checkpol.domain.CommunicationDispatchStatus;
import es.checkpol.domain.GeneratedCommunication;
import es.checkpol.repository.GeneratedCommunicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SesSubmissionPollingScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SesSubmissionPollingScheduler.class);

    private final GeneratedCommunicationRepository generatedCommunicationRepository;
    private final SesCommunicationGateway sesCommunicationGateway;

    public SesSubmissionPollingScheduler(
        GeneratedCommunicationRepository generatedCommunicationRepository,
        SesCommunicationGateway sesCommunicationGateway
    ) {
        this.generatedCommunicationRepository = generatedCommunicationRepository;
        this.sesCommunicationGateway = sesCommunicationGateway;
    }

    @Scheduled(fixedDelayString = "${checkpol.ses.status-poll.delay-ms:60000}")
    @Transactional
    public void refreshPendingSesSubmissions() {
        for (GeneratedCommunication communication : generatedCommunicationRepository.findTop25ByDispatchStatusOrderBySubmittedAtAsc(CommunicationDispatchStatus.SUBMITTED_TO_SES)) {
            try {
                if (communication.getSesLoteCode() == null || communication.getSesLoteCode().isBlank()) {
                    continue;
                }
                if (communication.getBooking() == null || communication.getBooking().getOwner() == null || !communication.getBooking().getOwner().hasSesWebServiceConfiguration()) {
                    continue;
                }
                SesLoteStatusResult result = sesCommunicationGateway.queryLoteStatus(
                    communication.getBooking().getOwner(),
                    communication.getSesLoteCode()
                );
                communication.registerSesProcessingResult(
                    java.time.OffsetDateTime.now(),
                    result.processingStateCode(),
                    result.processingStateDescription(),
                    result.communicationCode(),
                    result.processingErrorType(),
                    result.processingErrorDescription()
                );
            } catch (RuntimeException exception) {
                LOGGER.warn("No he podido actualizar el estado SES de la comunicación {}.", communication.getId(), exception);
            }
        }
    }
}
