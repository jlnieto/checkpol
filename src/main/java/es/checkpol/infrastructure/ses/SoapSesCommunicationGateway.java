package es.checkpol.infrastructure.ses;

import es.checkpol.domain.AppUser;
import es.checkpol.domain.SesConnectionTestStatus;
import es.checkpol.service.SesCommunicationException;
import es.checkpol.service.SesCommunicationGateway;
import es.checkpol.service.SesConnectionTestResult;
import es.checkpol.service.SesCredentialCipher;
import es.checkpol.service.SesLoteStatusResult;
import es.checkpol.service.SesSubmissionResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.xpath.XPathFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

@Component
public class SoapSesCommunicationGateway implements SesCommunicationGateway {

    private static final String COMMUNICATION_NAMESPACE = "http://www.soap.servicios.hospedajes.mir.es/comunicacion";
    private static final String SOAP_ENVELOPE_NAMESPACE = "http://schemas.xmlsoap.org/soap/envelope/";

    private final HttpClient httpClient;
    private final SesCredentialCipher sesCredentialCipher;
    private final String endpointUrl;
    private final String applicationName;

    public SoapSesCommunicationGateway(
        @Qualifier("sesHttpClient") HttpClient httpClient,
        SesCredentialCipher sesCredentialCipher,
        @Value("${checkpol.ses.ws.url}") String endpointUrl,
        @Value("${checkpol.ses.ws.application-name}") String applicationName
    ) {
        this.httpClient = httpClient;
        this.sesCredentialCipher = sesCredentialCipher;
        this.endpointUrl = endpointUrl;
        this.applicationName = applicationName;
    }

    @Override
    public SesConnectionTestResult testConnection(AppUser owner) {
        OffsetDateTime testedAt = OffsetDateTime.now();
        if (!owner.hasSesWebServiceConfiguration()) {
            return new SesConnectionTestResult(
                SesConnectionTestStatus.CONFIGURATION_INCOMPLETE,
                "Completa el código de entidad, el usuario del servicio web y la clave para poder probar la conexión.",
                "No se puede probar la conexión porque faltan datos WS: código de entidad/arrendador, usuario o clave.",
                testedAt,
                endpointUrl,
                null,
                "CONFIGURATION_INCOMPLETE",
                null
            );
        }

        try {
            HttpResponse<String> response = sendSoapRequest(owner, buildCatalogoSoapEnvelope("SEXO"));
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                return accessError(
                    testedAt,
                    "SES no acepta estos datos. Revisa el código de entidad, el usuario del servicio web y la clave.",
                    "SES ha devuelto HTTP " + response.statusCode() + " al probar la conexión. Revisa usuario WS y clave. " + summarizeSoapFault(response.body()),
                    response.statusCode(),
                    normalizeRawDetail(response.body())
                );
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String adminDetail = describeTestHttpError(response.statusCode());
                String soapFault = summarizeSoapFault(response.body());
                if (hasText(soapFault)) {
                    adminDetail = adminDetail + " " + soapFault;
                }
                if (looksLikeAccessProblem(soapFault)) {
                    return accessError(
                        testedAt,
                        "SES no acepta estos datos. Revisa el código de entidad, el usuario del servicio web y la clave.",
                        adminDetail,
                        response.statusCode(),
                        hasText(soapFault) ? soapFault : normalizeRawDetail(response.body())
                    );
                }
                return technicalError(
                    testedAt,
                    adminDetail,
                    response.statusCode(),
                    hasText(soapFault) ? soapFault : normalizeRawDetail(response.body()),
                    "HTTP_" + response.statusCode()
                );
            }
            return parseCatalogConnectionResponse(response.body(), testedAt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return technicalError(testedAt, "La prueba de conexión con SES se ha interrumpido.", null, exception.getMessage(), "InterruptedException");
        } catch (HttpTimeoutException exception) {
            return technicalError(testedAt, "La prueba de conexión con SES ha excedido el tiempo de espera.", null, exception.getMessage(), "HttpTimeoutException");
        } catch (SSLHandshakeException exception) {
            return technicalError(testedAt, "Error TLS/SSL al conectar con SES: " + exception.getMessage(), null, exception.toString(), "SSLHandshakeException");
        } catch (SSLException exception) {
            return technicalError(testedAt, "Error TLS/SSL al conectar con SES: " + exception.getMessage(), null, exception.toString(), "SSLException");
        } catch (ConnectException exception) {
            return technicalError(testedAt, "No he podido abrir conexión con SES: " + exception.getMessage(), null, exception.toString(), "ConnectException");
        } catch (Exception exception) {
            return technicalError(testedAt, "No he podido comprobar la conexión con SES: " + exception.getMessage(), null, exception.toString(), exception.getClass().getSimpleName());
        }
    }

    private SesConnectionTestResult parseCatalogConnectionResponse(String responseBody, OffsetDateTime testedAt) throws Exception {
        try {
            Document document = parseDocument(responseBody);
            int code = parseRequiredXPathInt(document, "string((//*[local-name()='catalogoResponse']/*[local-name()='resultado']/*[local-name()='codigo'])[1])");
            String description = parseOptionalXPath(document, "string((//*[local-name()='catalogoResponse']/*[local-name()='resultado']/*[local-name()='descripcion'])[1])");
            Integer tupleCount = parseOptionalXPathInt(document, "string(count(//*[local-name()='catalogoResponse']//*[local-name()='tupla']))");

            if (code == 0) {
                return new SesConnectionTestResult(
                    SesConnectionTestStatus.OK,
                    "Conexión correcta con SES. Ya puedes usar la presentación automática desde Checkpol.",
                    "Conexión WS correcta. La operación catalogo(SEXO) ha respondido con código 0"
                        + (tupleCount == null ? "." : " y " + tupleCount + " filas.")
                        + " Esta prueba valida conexión, autenticación y TLS; no confirma un envío real.",
                    testedAt,
                    endpointUrl,
                    200,
                    null,
                    hasText(description)
                        ? "catalogo(SEXO) código 0: " + description
                        : (tupleCount == null ? "catalogo(SEXO) código 0" : "catalogo(SEXO) código 0 con " + tupleCount + " filas")
                );
            }

            return accessError(
                testedAt,
                "SES ha rechazado estos datos. Revisa el código de entidad, el usuario del servicio web y la clave.",
                hasText(description)
                    ? "SES ha rechazado la prueba de conexión con código " + code + ": " + description
                    : "SES ha rechazado la prueba de conexión con código " + code + ".",
                200,
                normalizeRawDetail(responseBody)
            );
        } catch (Exception exception) {
            String faultString = summarizeSoapFault(responseBody);
            String adminDetail = "SES ha respondido HTTP 200, pero el cuerpo no coincide con el formato esperado para catalogoResponse.";
            if (hasText(faultString)) {
                adminDetail = adminDetail + " Fault: " + faultString;
            } else {
                adminDetail = adminDetail + " Revisa el detalle bruto guardado.";
            }
            return technicalError(
                testedAt,
                adminDetail,
                200,
                normalizeRawDetail(responseBody),
                "UNEXPECTED_SOAP_RESPONSE"
            );
        }
    }

    @Override
    public SesSubmissionResult submitTravelerPart(AppUser owner, String xmlContent) {
        if (!owner.hasSesWebServiceConfiguration()) {
            throw new IllegalArgumentException("El owner no tiene configurado el servicio web de SES.");
        }

        try {
            HttpResponse<String> response = sendSoapRequest(owner, buildSoapEnvelope(owner, encodeZipBase64(xmlContent)));
            String responseBody = response.body();
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new SesCommunicationException("SES ha devuelto HTTP " + response.statusCode() + ".", null, normalizeRawDetail(responseBody));
            }
            try {
                return parseSoapResponse(responseBody);
            } catch (Exception exception) {
                throw unexpectedSesResponse("No he podido interpretar la respuesta de envío de SES.", responseBody, exception);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La comunicación con SES se ha interrumpido.", exception);
        } catch (SesCommunicationException exception) {
            throw exception;
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException || exception instanceof IllegalArgumentException) {
                throw (RuntimeException) exception;
            }
            throw new IllegalStateException("No he podido enviar la comunicación al servicio web de SES.", exception);
        }
    }

    @Override
    public SesLoteStatusResult queryLoteStatus(AppUser owner, String loteCode) {
        if (!owner.hasSesWebServiceConfiguration()) {
            throw new IllegalArgumentException("El owner no tiene configurado el servicio web de SES.");
        }
        if (loteCode == null || loteCode.isBlank()) {
            throw new IllegalArgumentException("No he recibido el lote de SES a consultar.");
        }

        try {
            HttpResponse<String> response = sendSoapRequest(owner, buildConsultaLoteSoapEnvelope(loteCode.trim()));
            String responseBody = response.body();
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new SesCommunicationException("SES ha devuelto HTTP " + response.statusCode() + ".", null, normalizeRawDetail(responseBody));
            }
            try {
                return parseConsultaLoteResponse(responseBody);
            } catch (Exception exception) {
                throw unexpectedSesResponse("No he podido interpretar la respuesta de consulta de lote de SES.", responseBody, exception);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La consulta a SES se ha interrumpido.", exception);
        } catch (SesCommunicationException exception) {
            throw exception;
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException || exception instanceof IllegalArgumentException) {
                throw (RuntimeException) exception;
            }
            throw new IllegalStateException("No he podido consultar el lote en SES.", exception);
        }
    }

    @Override
    public SesSubmissionResult cancelLote(AppUser owner, String loteCode) {
        if (!owner.hasSesWebServiceConfiguration()) {
            throw new IllegalArgumentException("El owner no tiene configurado el servicio web de SES.");
        }
        if (loteCode == null || loteCode.isBlank()) {
            throw new IllegalArgumentException("No he recibido el lote de SES a anular.");
        }

        try {
            HttpResponse<String> response = sendSoapRequest(owner, buildAnulacionLoteSoapEnvelope(loteCode.trim()));
            String responseBody = response.body();
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new SesCommunicationException("SES ha devuelto HTTP " + response.statusCode() + ".", null, normalizeRawDetail(responseBody));
            }
            try {
                return parseAnulacionLoteResponse(responseBody);
            } catch (Exception exception) {
                throw unexpectedSesResponse("No he podido interpretar la respuesta de anulación de SES.", responseBody, exception);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La anulación en SES se ha interrumpido.", exception);
        } catch (SesCommunicationException exception) {
            throw exception;
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException || exception instanceof IllegalArgumentException) {
                throw (RuntimeException) exception;
            }
            throw new IllegalStateException("No he podido anular el lote en SES.", exception);
        }
    }

    private String buildSoapEnvelope(AppUser owner, String compressedBase64Xml) {
        StringBuilder xml = new StringBuilder();
        xml.append("<soapenv:Envelope xmlns:soapenv=\"").append(SOAP_ENVELOPE_NAMESPACE).append("\" xmlns:com=\"").append(COMMUNICATION_NAMESPACE).append("\">");
        xml.append("<soapenv:Header/>");
        xml.append("<soapenv:Body>");
        xml.append("<com:comunicacionRequest>");
        xml.append("<peticion>");
        xml.append("<cabecera>");
        xml.append(tag("codigoArrendador", owner.getSesArrendadorCode()));
        xml.append(tag("aplicacion", applicationName));
        xml.append(tag("tipoOperacion", "A"));
        xml.append(tag("tipoComunicacion", "PV"));
        xml.append("</cabecera>");
        xml.append(tag("solicitud", compressedBase64Xml));
        xml.append("</peticion>");
        xml.append("</com:comunicacionRequest>");
        xml.append("</soapenv:Body>");
        xml.append("</soapenv:Envelope>");
        return xml.toString();
    }

    private String buildConsultaLoteSoapEnvelope(String loteCode) {
        StringBuilder xml = new StringBuilder();
        xml.append("<soapenv:Envelope xmlns:soapenv=\"").append(SOAP_ENVELOPE_NAMESPACE).append("\" xmlns:com=\"").append(COMMUNICATION_NAMESPACE).append("\">");
        xml.append("<soapenv:Header/>");
        xml.append("<soapenv:Body>");
        xml.append("<com:consultaLoteRequest>");
        xml.append("<codigosLote>");
        xml.append(tag("lote", loteCode));
        xml.append("</codigosLote>");
        xml.append("</com:consultaLoteRequest>");
        xml.append("</soapenv:Body>");
        xml.append("</soapenv:Envelope>");
        return xml.toString();
    }

    private String buildAnulacionLoteSoapEnvelope(String loteCode) {
        StringBuilder xml = new StringBuilder();
        xml.append("<soapenv:Envelope xmlns:soapenv=\"").append(SOAP_ENVELOPE_NAMESPACE).append("\" xmlns:com=\"").append(COMMUNICATION_NAMESPACE).append("\">");
        xml.append("<soapenv:Header/>");
        xml.append("<soapenv:Body>");
        xml.append("<com:anulacionLoteRequest>");
        xml.append(tag("lote", loteCode));
        xml.append("</com:anulacionLoteRequest>");
        xml.append("</soapenv:Body>");
        xml.append("</soapenv:Envelope>");
        return xml.toString();
    }

    private String buildCatalogoSoapEnvelope(String catalogCode) {
        StringBuilder xml = new StringBuilder();
        xml.append("<soapenv:Envelope xmlns:soapenv=\"").append(SOAP_ENVELOPE_NAMESPACE).append("\" xmlns:com=\"").append(COMMUNICATION_NAMESPACE).append("\">");
        xml.append("<soapenv:Header/>");
        xml.append("<soapenv:Body>");
        xml.append("<com:catalogoRequest>");
        xml.append("<peticion>");
        xml.append(tag("catalogo", catalogCode));
        xml.append("</peticion>");
        xml.append("</com:catalogoRequest>");
        xml.append("</soapenv:Body>");
        xml.append("</soapenv:Envelope>");
        return xml.toString();
    }

    private HttpResponse<String> sendSoapRequest(AppUser owner, String payload) throws Exception {
        String basicToken = Base64.getEncoder().encodeToString(
            (owner.getSesWsUsername() + ":" + sesCredentialCipher.decrypt(owner.getSesWsPasswordEncrypted()))
                .getBytes(StandardCharsets.UTF_8)
        );
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpointUrl))
            .timeout(Duration.ofSeconds(30))
            .header(HttpHeaders.AUTHORIZATION, "Basic " + basicToken)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML_VALUE + "; charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
            .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String encodeZipBase64(String xmlContent) throws Exception {
        java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream, StandardCharsets.UTF_8)) {
            zipOutputStream.putNextEntry(new ZipEntry("parte-viajeros.xml"));
            zipOutputStream.write(xmlContent.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
    }

    private SesSubmissionResult parseSoapResponse(String body) throws Exception {
        Document document = parseDocument(body);

        Integer code = parseRequiredInt(document, "codigo");
        String description = parseRequiredText(document, "descripcion");
        String lote = parseOptionalText(document, "lote");
        return new SesSubmissionResult(code, description, lote, normalizeRawDetail(body));
    }

    SesLoteStatusResult parseConsultaLoteResponse(String body) throws Exception {
        Document document = parseDocument(body);
        int responseCode = parseRequiredXPathInt(document, "string((//*[local-name()='consultaLoteResponse']/*[local-name()='respuesta']/*[local-name()='codigo'])[1])");
        String responseDescription = parseRequiredXPath(document, "string((//*[local-name()='consultaLoteResponse']/*[local-name()='respuesta']/*[local-name()='descripcion'])[1])");
        String loteCode = parseOptionalXPath(document, "string((//*[local-name()='consultaLoteResponse']/*[local-name()='resultado']/*[local-name()='lote'])[1])");
        Integer processingStateCode = parseOptionalXPathInt(document, "string((//*[local-name()='consultaLoteResponse']/*[local-name()='resultado']/*[local-name()='codigoEstado'])[1])");
        String processingStateDescription = parseOptionalXPath(document, "string((//*[local-name()='consultaLoteResponse']/*[local-name()='resultado']/*[local-name()='descEstado'])[1])");
        String communicationCode = parseOptionalXPath(document, "string((//*[local-name()='consultaLoteResponse']/*[local-name()='resultado']//*[local-name()='resultadoComunicacion'][1]/*[local-name()='codigoComunicacion'])[1])");
        String processingErrorType = parseOptionalXPath(document, "string((//*[local-name()='consultaLoteResponse']/*[local-name()='resultado']//*[local-name()='resultadoComunicacion'][1]/*[local-name()='tipoError'])[1])");
        String processingErrorDescription = parseOptionalXPath(document, "string((//*[local-name()='consultaLoteResponse']/*[local-name()='resultado']//*[local-name()='resultadoComunicacion'][1]/*[local-name()='error'])[1])");
        String processedAtValue = parseOptionalXPath(document, "string((//*[local-name()='consultaLoteResponse']/*[local-name()='resultado']/*[local-name()='fechaProcesamiento'])[1])");

        return new SesLoteStatusResult(
            responseCode,
            responseDescription,
            loteCode,
            processingStateCode,
            blankToNull(processingStateDescription),
            blankToNull(communicationCode),
            blankToNull(processingErrorType),
            blankToNull(processingErrorDescription),
            parseSesDateTime(processedAtValue),
            normalizeRawDetail(body)
        );
    }

    private OffsetDateTime parseSesDateTime(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(normalized);
        } catch (DateTimeParseException ignored) {
            // SES returns fechaProcesamiento as "yyyy-MM-dd HH:mm:ss" in production responses.
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                return localDateTime.atZone(ZoneId.of("Europe/Madrid")).toOffsetDateTime();
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    SesSubmissionResult parseAnulacionLoteResponse(String body) throws Exception {
        Document document = parseDocument(body);
        int code = parseRequiredXPathInt(document, "string((//*[local-name()='anulacionLoteResponse']/*[local-name()='codigo'])[1])");
        String description = parseOptionalXPath(document, "string((//*[local-name()='anulacionLoteResponse']/*[local-name()='descripcion'])[1])");
        if (!hasText(description) && code == 0) {
            description = "Anulación aceptada por SES.";
        } else if (!hasText(description)) {
            description = "SES ha respondido a la anulación con código " + code + ".";
        }
        return new SesSubmissionResult(code, description, null, normalizeRawDetail(body));
    }

    private SesCommunicationException unexpectedSesResponse(String message, String body, Exception cause) {
        String faultString = summarizeSoapFault(body);
        String detail = hasText(faultString) ? message + " Fault: " + faultString : message;
        return new SesCommunicationException(detail, null, normalizeRawDetail(body), cause);
    }

    private Document parseDocument(String body) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(body)));
    }

    private Integer parseRequiredInt(Document document, String localName) {
        String value = parseRequiredText(document, localName);
        return parseIntegerValue(value);
    }

    private String parseRequiredText(Document document, String localName) {
        String value = parseOptionalText(document, localName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("La respuesta de SES no incluye " + localName + ".");
        }
        return value.trim();
    }

    private String parseOptionalText(Document document, String localName) {
        org.w3c.dom.NodeList nodeList = document.getElementsByTagNameNS("*", localName);
        if (nodeList.getLength() == 0 || nodeList.item(0) == null) {
            return null;
        }
        return nodeList.item(0).getTextContent();
    }

    private String parseRequiredXPath(Document document, String expression) throws Exception {
        String value = parseOptionalXPath(document, expression);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("La respuesta de SES no incluye el dato esperado.");
        }
        return value.trim();
    }

    private String parseOptionalXPath(Document document, String expression) throws Exception {
        String value = XPathFactory.newInstance().newXPath().evaluate(expression, document);
        return blankToNull(value);
    }

    private Integer parseRequiredXPathInt(Document document, String expression) throws Exception {
        return parseIntegerValue(parseRequiredXPath(document, expression));
    }

    private Integer parseOptionalXPathInt(Document document, String expression) throws Exception {
        String value = parseOptionalXPath(document, expression);
        return value == null ? null : parseIntegerValue(value);
    }

    private Integer parseIntegerValue(String value) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalStateException("No he recibido un número entero de SES.");
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ignored) {
            double numericValue = Double.parseDouble(normalized);
            return (int) Math.round(numericValue);
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String summarizeSoapFault(String body) {
        if (!hasText(body)) {
            return null;
        }
        try {
            Document document = parseDocument(body);
            return parseOptionalXPath(document, "string((//*[local-name()='faultstring'])[1])");
        } catch (Exception exception) {
            return null;
        }
    }

    private String normalizeRawDetail(String body) {
        if (!hasText(body)) {
            return null;
        }
        return body.trim();
    }

    private String describeTestHttpError(int statusCode) {
        if (statusCode == 502) {
            return "SES ha devuelto HTTP 502 al probar la conexión. El gateway del Ministerio no ha podido completar la petición. Suele apuntar a una incidencia temporal del servicio o a que la operación de prueba no está siendo aceptada por su backend, no a un fallo claro de usuario o clave.";
        }
        if (statusCode == 503 || statusCode == 504) {
            return "SES ha devuelto HTTP " + statusCode + " al probar la conexión. El servicio del Ministerio no estaba disponible o no respondió a tiempo.";
        }
        return "SES ha devuelto HTTP " + statusCode + " al probar la conexión.";
    }

    private boolean looksLikeAccessProblem(String detail) {
        if (!hasText(detail)) {
            return false;
        }
        String normalized = detail.toLowerCase();
        return normalized.contains("credencial")
            || normalized.contains("autentic")
            || normalized.contains("usuario")
            || normalized.contains("password")
            || normalized.contains("clave");
    }

    private SesConnectionTestResult accessError(OffsetDateTime testedAt, String ownerMessage, String adminMessage, Integer httpStatus, String rawDetail) {
        return new SesConnectionTestResult(
            SesConnectionTestStatus.ACCESS_ERROR,
            ownerMessage,
            adminMessage,
            testedAt,
            endpointUrl,
            httpStatus,
            "ACCESS_ERROR",
            rawDetail
        );
    }

    private SesConnectionTestResult technicalError(OffsetDateTime testedAt, String adminMessage, Integer httpStatus, String rawDetail, String errorType) {
        return new SesConnectionTestResult(
            SesConnectionTestStatus.TECHNICAL_ERROR,
            "No he podido comprobar la conexión por un problema técnico. Ponte en contacto con el administrador.",
            adminMessage,
            testedAt,
            endpointUrl,
            httpStatus,
            errorType,
            rawDetail
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String tag(String name, String value) {
        return "<" + name + ">" + escapeXml(value) + "</" + name + ">";
    }

    private String escapeXml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
