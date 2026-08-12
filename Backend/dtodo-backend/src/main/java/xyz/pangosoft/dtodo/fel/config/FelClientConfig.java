package xyz.pangosoft.dtodo.fel.config;

import java.io.IOException;
import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * Configuración del cliente HTTP hacia los servicios de INFILE.
 *
 * <p>Los timeouts replican los del conector original (connect 280s / read 310s:
 * el certificador de INFILE puede tardar). El {@link ResponseErrorHandler} nunca
 * lanza excepción por código HTTP: INFILE devuelve el detalle del error en el
 * body JSON incluso con HTTP &gt;= 400, y es el campo {@code resultado} del JSON
 * el que determina el éxito — mismo comportamiento del jar.</p>
 */
@Configuration
@EnableConfigurationProperties(FelInfileProperties.class)
public class FelClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofMillis(280_000);
    private static final Duration READ_TIMEOUT = Duration.ofMillis(310_000);

    @Bean
    public RestTemplate felRestTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(CONNECT_TIMEOUT)
                .readTimeout(READ_TIMEOUT)
                .errorHandler(new ResponseErrorHandler() {
                    @Override
                    public boolean hasError(ClientHttpResponse response) throws IOException {
                        return false;
                    }

                })
                .build();
    }
}
