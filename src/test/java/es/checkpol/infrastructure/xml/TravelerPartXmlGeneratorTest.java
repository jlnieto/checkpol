package es.checkpol.infrastructure.xml;

import es.checkpol.domain.Accommodation;
import es.checkpol.domain.Address;
import es.checkpol.domain.Booking;
import es.checkpol.domain.BookingChannel;
import es.checkpol.domain.DocumentType;
import es.checkpol.domain.Guest;
import es.checkpol.domain.GuestReviewStatus;
import es.checkpol.domain.GuestSex;
import es.checkpol.domain.GuestSubmissionSource;
import es.checkpol.domain.PaymentType;
import es.checkpol.service.BookingDetails;
import es.checkpol.service.BookingOperationalStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelerPartXmlGeneratorTest {

    private final TravelerPartXmlGenerator generator = new TravelerPartXmlGenerator();

    @Test
    void generatesTravelerPartXml() {
        Accommodation accommodation = new Accommodation("Casa Olivo", "H123456789", "VT-123");
        Booking booking = new Booking(
            accommodation,
            "ABC123",
            2,
            LocalDate.of(2026, 3, 20),
            BookingChannel.DIRECT,
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 4, 4),
            PaymentType.EFECT,
            LocalDate.of(2026, 3, 20),
            "Caja",
            "Ana Lopez",
            null
        );
        Address address = new Address(
            booking,
            "Calle Mayor 1",
            null,
            "28079",
            null,
            "28001",
            "ESP"
        );
        Guest guest = new Guest(
            booking,
            "Ana",
            "Lopez",
            "Martin",
            DocumentType.NIF,
            "00000000T",
            "SUP123",
            LocalDate.of(1990, 5, 1),
            "ESP",
            GuestSex.M,
            address,
            "600000000",
            null,
            "ana@example.com",
            null,
            GuestSubmissionSource.MANUAL,
            GuestReviewStatus.REVIEWED,
            OffsetDateTime.now()
        );

        String xml = generator.generate(new BookingDetails(
            booking,
            List.of(guest),
            1,
            1,
            false,
            true,
            Optional.empty(),
            0,
            List.of(),
            Optional.empty(),
            BookingOperationalStatus.READY_FOR_XML,
            0,
            0,
            false,
            false,
            false,
            false,
            null,
            null,
            List.of()
        ));

        assertTrue(xml.contains("<codigoEstablecimiento>H123456789</codigoEstablecimiento>"));
        assertTrue(xml.contains("<tipoPago>EFECT</tipoPago>"));
        assertTrue(xml.contains("<rol>VI</rol>"));
        assertTrue(xml.contains("<codigoMunicipio>28079</codigoMunicipio>"));
    }

    @Test
    void keepsNonSpanishMunicipalityAsNameInXml() {
        Accommodation accommodation = new Accommodation("Casa Olivo", "H123456789", "VT-123");
        Booking booking = new Booking(
            accommodation,
            "XYZ789",
            1,
            LocalDate.of(2026, 3, 20),
            BookingChannel.DIRECT,
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 4, 4),
            PaymentType.EFECT,
            LocalDate.of(2026, 3, 20),
            "Caja",
            "Ana Lopez",
            null
        );
        Address address = new Address(
            booking,
            "Rue de Rivoli 10",
            null,
            null,
            "Paris",
            "75001",
            "FRA"
        );
        Guest guest = new Guest(
            booking,
            "Claire",
            "Martin",
            null,
            DocumentType.PAS,
            "AB123456",
            null,
            LocalDate.of(1990, 5, 1),
            "FRA",
            GuestSex.M,
            address,
            "600000000",
            null,
            "claire@example.com",
            null,
            GuestSubmissionSource.MANUAL,
            GuestReviewStatus.REVIEWED,
            OffsetDateTime.now()
        );

        String xml = generator.generate(new BookingDetails(
            booking,
            List.of(guest),
            1,
            1,
            false,
            true,
            Optional.empty(),
            0,
            List.of(),
            Optional.empty(),
            BookingOperationalStatus.READY_FOR_XML,
            0,
            0,
            false,
            false,
            false,
            false,
            null,
            null,
            List.of()
        ));

        assertTrue(xml.contains("<nombreMunicipio>Paris</nombreMunicipio>"));
        assertTrue(!xml.contains("<codigoMunicipio>"));
    }
}
