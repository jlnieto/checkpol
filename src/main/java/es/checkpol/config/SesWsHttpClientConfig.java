package es.checkpol.config;

import es.checkpol.infrastructure.ses.SesWsSslContextFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class SesWsHttpClientConfig {

    private final ResourceLoader resourceLoader;
    private final SesWsSslContextFactory sesWsSslContextFactory;

    public SesWsHttpClientConfig(ResourceLoader resourceLoader, SesWsSslContextFactory sesWsSslContextFactory) {
        this.resourceLoader = resourceLoader;
        this.sesWsSslContextFactory = sesWsSslContextFactory;
    }

    @Bean("sesHttpClient")
    public HttpClient sesHttpClient(
        @Value("${checkpol.ses.ws.truststore-path:}") String truststorePath,
        @Value("${checkpol.ses.ws.truststore-password:}") String truststorePassword,
        @Value("${checkpol.ses.ws.truststore-type:PKCS12}") String truststoreType
    ) {
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15));

        if (truststorePath == null || truststorePath.isBlank()) {
            return builder.build();
        }

        Resource truststoreResource = resourceLoader.getResource(truststorePath);
        if (!truststoreResource.exists()) {
            throw new IllegalStateException("No encuentro el truststore de SES en " + truststorePath + ".");
        }

        return builder
            .sslContext(sesWsSslContextFactory.build(truststoreResource, truststorePassword, truststoreType))
            .build();
    }
}
