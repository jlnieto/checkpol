package es.checkpol.infrastructure.xml;

import es.checkpol.domain.Booking;
import es.checkpol.domain.Guest;
import es.checkpol.service.BookingDetails;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class TravelerPartXmlGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'00:00:00");

    public String generate(BookingDetails details) {
        StringBuilder xml = new StringBuilder();
        Booking booking = details.booking();

        xml.append("<ns2:peticion xmlns:ns2=\"http://www.neg.hospedajes.mir.es/altaParteHospedaje\">");
        xml.append("<solicitud>");
        xml.append(tag("codigoEstablecimiento", booking.getAccommodation().getSesEstablishmentCode()));
        xml.append("<comunicacion>");
        xml.append("<contrato>");
        xml.append(tag("referencia", booking.getReferenceCode()));
        xml.append(tag("fechaContrato", formatDate(booking.getContractDate())));
        xml.append(tag("fechaEntrada", formatDateTime(booking.getCheckInDate())));
        xml.append(tag("fechaSalida", formatDateTime(booking.getCheckOutDate())));
        xml.append(tag("numPersonas", String.valueOf(details.guests().size())));
        xml.append("<pago>");
        xml.append(tag("tipoPago", booking.getPaymentType().name()));
        appendOptional(xml, "fechaPago", booking.getPaymentDate() == null ? null : formatDate(booking.getPaymentDate()));
        appendOptional(xml, "medioPago", booking.getPaymentMethod());
        appendOptional(xml, "titular", booking.getPaymentHolder());
        appendOptional(xml, "caducidadTarjeta", booking.getCardExpiry());
        xml.append("</pago>");
        xml.append("</contrato>");

        for (Guest guest : details.guests()) {
            xml.append("<persona>");
            xml.append(tag("rol", "VI"));
            xml.append(tag("nombre", guest.getFirstName()));
            xml.append(tag("apellido1", guest.getLastName1()));
            appendOptional(xml, "apellido2", guest.getLastName2());
            appendOptional(xml, "tipoDocumento", guest.getDocumentType() == null ? null : guest.getDocumentType().name());
            appendOptional(xml, "numeroDocumento", guest.getDocumentNumber());
            appendOptional(xml, "soporteDocumento", guest.getDocumentSupport());
            xml.append(tag("fechaNacimiento", formatDate(guest.getBirthDate())));
            appendOptional(xml, "nacionalidad", guest.getNationality());
            appendOptional(xml, "sexo", guest.getSex() == null ? null : guest.getSex().name());
            xml.append("<direccion>");
            xml.append(tag("direccion", guest.getAddressLine()));
            appendOptional(xml, "direccionComplementaria", guest.getAddressComplement());
            if ("ESP".equalsIgnoreCase(guest.getCountry())) {
                xml.append(tag("codigoMunicipio", guest.getMunicipalityCode()));
            } else {
                appendOptional(xml, "nombreMunicipio", guest.getMunicipalityName());
            }
            xml.append(tag("codigoPostal", guest.getPostalCode()));
            xml.append(tag("pais", guest.getCountry()));
            xml.append("</direccion>");
            appendOptional(xml, "telefono", guest.getPhone());
            appendOptional(xml, "telefono2", guest.getPhone2());
            appendOptional(xml, "correo", guest.getEmail());
            appendOptional(xml, "parentesco", guest.getRelationship());
            xml.append("</persona>");
        }

        xml.append("</comunicacion>");
        xml.append("</solicitud>");
        xml.append("</ns2:peticion>");
        return xml.toString();
    }

    private String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT);
    }

    private String formatDateTime(LocalDate date) {
        return date.format(DATE_TIME_FORMAT);
    }

    private void appendOptional(StringBuilder xml, String tagName, String value) {
        if (value != null && !value.isBlank()) {
            xml.append(tag(tagName, value));
        }
    }

    private String tag(String name, String value) {
        return "<" + name + ">" + escape(value) + "</" + name + ">";
    }

    private String escape(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
