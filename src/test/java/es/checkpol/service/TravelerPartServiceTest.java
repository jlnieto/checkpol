package es.checkpol.service;

import es.checkpol.domain.Accommodation;
import es.checkpol.domain.Booking;
import es.checkpol.domain.BookingChannel;
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
    private final TravelerPartService travelerPartService = new TravelerPartService(
        bookingService,
        xmlGenerator,
        generatedCommunicationRepository
    );

    @Test
    void savesGeneratedCommunication() {
        BookingDetails details = sampleDetails(true);
        Mockito.when(bookingService.getDetails(1L)).thenReturn(details);
        Mockito.when(xmlGenerator.generate(details)).thenReturn("<xml/>");
        Mockito.when(generatedCommunicationRepository.findFirstByBookingIdOrderByVersionDesc(1L)).thenReturn(Optional.empty());

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
        Mockito.when(generatedCommunicationRepository.findByIdAndBookingId(9L, 1L))
            .thenReturn(Optional.of(communication));

        String xml = travelerPartService.getGeneratedXml(1L, 9L);

        assertTrue(xml.contains("<xml/>"));
        assertTrue(communication.getDownloadCount() == 1);
    }

    private BookingDetails sampleDetails(boolean ready) {
        Accommodation accommodation = new Accommodation("Casa Olivo", "H123456789", "VT-123");
        Booking booking = new Booking(
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
