package es.checkpol.config;

import es.checkpol.infrastructure.ses.SesWsSslContextFactory;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SesWsHttpClientConfigTest {

    @Test
    void createsDefaultHttpClientWhenTruststoreIsNotConfigured() {
        SesWsHttpClientConfig config = new SesWsHttpClientConfig(new DefaultResourceLoader(), new SesWsSslContextFactory());

        HttpClient httpClient = config.sesHttpClient("", "", "PKCS12");

        assertNotNull(httpClient);
    }

    @Test
    void failsWhenTruststorePathDoesNotExist() {
        SesWsHttpClientConfig config = new SesWsHttpClientConfig(new DefaultResourceLoader(), new SesWsSslContextFactory());

        assertThrows(IllegalStateException.class, () ->
            config.sesHttpClient("file:/tmp/checkpol-ses-no-existe.p12", "changeit", "PKCS12")
        );
    }
}
