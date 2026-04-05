package es.checkpol.infrastructure.ses;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;

import javax.net.ssl.SSLContext;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SesWsSslContextFactoryTest {

    private final SesWsSslContextFactory factory = new SesWsSslContextFactory();

    @Test
    void buildsSslContextFromPkcs12Truststore() throws Exception {
        Path tempFile = Files.createTempFile("checkpol-ses-truststore", ".p12");
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, "changeit".toCharArray());
        try (OutputStream outputStream = Files.newOutputStream(tempFile)) {
            keyStore.store(outputStream, "changeit".toCharArray());
        }

        SSLContext sslContext = factory.build(new FileSystemResource(tempFile), "changeit", "PKCS12");

        assertNotNull(sslContext);
    }

    @Test
    void rejectsMissingTruststorePassword() {
        assertThrows(IllegalArgumentException.class, () ->
            factory.build(new FileSystemResource("/tmp/no-existe.p12"), "", "PKCS12")
        );
    }
}
