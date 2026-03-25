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

    public TravelerPartService(
        BookingService bookingService,
        TravelerPartXmlGenerator xmlGenerator,
        GeneratedCommunicationRepository generatedCommunicationRepository
    ) {
        this.bookingService = bookingService;
        this.xmlGenerator = xmlGenerator;
        this.generatedCommunicationRepository = generatedCommunicationRepository;
    }

    @Transactional
    public String generateXml(Long bookingId) {
        BookingDetails details = bookingService.getDetails(bookingId);
        if (!details.readyForTravelerPart()) {
            throw new IllegalStateException("La estancia no tiene todavia los datos minimos para generar el parte de viajeros.");
        }
        String xml = xmlGenerator.generate(details);
        int nextVersion = generatedCommunicationRepository.findFirstByBookingIdOrderByVersionDesc(bookingId)
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
        GeneratedCommunication communication = generatedCommunicationRepository.findByIdAndBookingId(communicationId, bookingId)
            .orElseThrow(() -> new IllegalArgumentException("La comunicacion solicitada no existe para esta estancia."))
            ;
        communication.registerDownload(OffsetDateTime.now());
        return communication.getXmlContent();
    }
}
