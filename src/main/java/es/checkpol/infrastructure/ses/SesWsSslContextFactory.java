package es.checkpol.infrastructure.ses;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;

@Component
public class SesWsSslContextFactory {

    public SSLContext build(Resource truststoreResource, String truststorePassword, String truststoreType) {
        if (truststoreResource == null) {
            throw new IllegalArgumentException("No he recibido el recurso del truststore de SES.");
        }
        if (truststorePassword == null || truststorePassword.isBlank()) {
            throw new IllegalArgumentException("Falta la contraseña del truststore de SES.");
        }
        if (truststoreType == null || truststoreType.isBlank()) {
            throw new IllegalArgumentException("Falta el tipo de truststore de SES.");
        }

        try {
            KeyStore keyStore = KeyStore.getInstance(truststoreType);
            try (InputStream inputStream = truststoreResource.getInputStream()) {
                keyStore.load(inputStream, truststorePassword.toCharArray());
            }

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            return sslContext;
        } catch (Exception exception) {
            throw new IllegalStateException("No he podido cargar el truststore de SES.", exception);
        }
    }
}
