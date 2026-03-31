package es.checkpol.service;

import es.checkpol.infrastructure.xml.TravelerPartXmlGenerator;
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

    public TravelerPartService(
        BookingService bookingService,
        TravelerPartXmlGenerator xmlGenerator,
        GeneratedCommunicationRepository generatedCommunicationRepository,
        CurrentAppUserService currentAppUserService
    ) {
        this.bookingService = bookingService;
        this.xmlGenerator = xmlGenerator;
        this.generatedCommunicationRepository = generatedCommunicationRepository;
        this.currentAppUserService = currentAppUserService;
    }

    @Transactional
    public String generateXml(Long bookingId) {
        BookingDetails details = bookingService.getDetails(bookingId);
        if (!details.readyForTravelerPart()) {
            throw new IllegalStateException(details.blockingMessage() == null
                ? "La estancia no esta lista para generar el archivo SES."
                : details.blockingMessage());
        }
        String xml = xmlGenerator.generate(details);
        int nextVersion = generatedCommunicationRepository.findFirstByBookingIdAndBookingOwnerIdOrderByVersionDesc(bookingId, currentAppUserService.requireCurrentUserId())
            .map(communication -> communication.getVersion() + 1)
            .orElse(1);
        generatedCommunicationRepository.save(new GeneratedCommunication(
            details.booking(),
            nextVersion,
            OffsetDateTime.now(),
            xml
        ));
        return xml;
    }

    @Transactional
    public String getGeneratedXml(Long bookingId, Long communicationId) {
        GeneratedCommunication communication = generatedCommunicationRepository.findByIdAndBookingIdAndBookingOwnerId(communicationId, bookingId, currentAppUserService.requireCurrentUserId())
            .orElseThrow(() -> new IllegalArgumentException("La comunicacion solicitada no existe para esta estancia."))
            ;
        communication.registerDownload(OffsetDateTime.now());
        return communication.getXmlContent();
    }
}
