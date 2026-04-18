package es.checkpol.infrastructure.ses;

import es.checkpol.service.SesCredentialCipher;
import es.checkpol.service.SesLoteStatusResult;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SoapSesCommunicationGatewayTest {

    @Test
    void parsesConsultaLoteResponseWithSesLocalDateTime() throws Exception {
        SoapSesCommunicationGateway gateway = new SoapSesCommunicationGateway(
            HttpClient.newHttpClient(),
            new SesCredentialCipher("test-secret"),
            "https://example.test",
            "checkpol"
        );

        SesLoteStatusResult result = gateway.parseConsultaLoteResponse("""
            <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
              <SOAP-ENV:Body>
                <ns3:consultaLoteResponse xmlns:ns3="http://www.soap.servicios.hospedajes.mir.es/comunicacion" xmlns="">
                  <respuesta>
                    <codigo>0</codigo>
                    <descripcion>Ok</descripcion>
                  </respuesta>
                  <resultado>
                    <lote>523a323d-3a7b-11f1-a800-0050569580a9</lote>
                    <fechaProcesamiento>2026-04-17 18:37:17</fechaProcesamiento>
                    <codigoEstado>1</codigoEstado>
                    <descEstado>Lote tramitado sin errores</descEstado>
                    <resultadoComunicaciones>
                      <resultadoComunicacion>
                        <codigoComunicacion>b26926f4-3a7b-11f1-a800-0050569580a9</codigoComunicacion>
                      </resultadoComunicacion>
                    </resultadoComunicaciones>
                  </resultado>
                </ns3:consultaLoteResponse>
              </SOAP-ENV:Body>
            </SOAP-ENV:Envelope>
            """);

        assertEquals(0, result.responseCode());
        assertEquals("Ok", result.responseDescription());
        assertEquals(1, result.processingStateCode());
        assertEquals("Lote tramitado sin errores", result.processingStateDescription());
        assertEquals("b26926f4-3a7b-11f1-a800-0050569580a9", result.communicationCode());
        assertNotNull(result.processedAt());
    }

    @Test
    void parsesSuccessfulAnulacionLoteResponseWithoutDescription() throws Exception {
        SoapSesCommunicationGateway gateway = new SoapSesCommunicationGateway(
            HttpClient.newHttpClient(),
            new SesCredentialCipher("test-secret"),
            "https://example.test",
            "checkpol"
        );

        var result = gateway.parseAnulacionLoteResponse("""
            <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
              <SOAP-ENV:Body>
                <ns3:anulacionLoteResponse xmlns:ns3="http://www.soap.servicios.hospedajes.mir.es/comunicacion" xmlns="">
                  <codigo>0</codigo>
                </ns3:anulacionLoteResponse>
              </SOAP-ENV:Body>
            </SOAP-ENV:Envelope>
            """);

        assertEquals(0, result.responseCode());
        assertEquals("Anulación aceptada por SES.", result.responseDescription());
    }
}
