package xyz.pangosoft.dtodo.fel.client;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import xyz.pangosoft.dtodo.fel.config.FelInfileProperties;
import xyz.pangosoft.dtodo.fel.dto.RequestFirma;
import xyz.pangosoft.dtodo.fel.dto.RespuestaFirma;

/**
 * Cliente del servicio de firma electrónica de INFILE.
 *
 * <p>Reemplazo de {@code com.fel.firma.emisor.FirmaEmisor}: la firma
 * criptográfica del XML la realiza INFILE en su servidor — este sistema nunca
 * maneja llaves privadas localmente. Los fallos de red o de parseo no lanzan
 * excepción: devuelven {@code resultado=false} con la descripción del problema,
 * igual que el conector original.</p>
 */
@Component
@Slf4j
public class FelSignerClient {

    private static final String MARCA_ANULACION = "GTAnulacionDocumento";

    private final RestTemplate felRestTemplate;
    private final ObjectMapper objectMapper;
    private final FelInfileProperties properties;

    public FelSignerClient(@Qualifier("felRestTemplate") RestTemplate felRestTemplate,
                           ObjectMapper objectMapper,
                           FelInfileProperties properties) {
        this.felRestTemplate = felRestTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * Envía el XML del DTE al servicio de firma de INFILE.
     *
     * @param xml   XML del DTE (sin firmar)
     * @param alias alias del emisor ante INFILE ({@code certificador.prefijo})
     * @param llave token del servicio de firma ({@code certificador.tokenSigner})
     * @return respuesta con el XML firmado en Base64 ({@code archivo}) si {@code resultado=true}
     */
    public RespuestaFirma firmar(String xml, String alias, String llave) {
        RequestFirma request = new RequestFirma();
        request.setAlias(alias);
        request.setLlave(llave);
        request.setCodigo("");
        request.setEs_anulacion(xml.contains(MARCA_ANULACION) ? "S" : "N");
        request.setArchivo(Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<String> response = felRestTemplate.postForEntity(
                    properties.getFirmaUrl(), new HttpEntity<>(request, headers), String.class);
            log.info("Servicio de firma INFILE respondió HTTP {}", response.getStatusCode().value());
            return parsearRespuesta(response.getBody(), response.getStatusCode().value());
        } catch (RestClientException e) {
            log.error("No se pudo invocar el servicio de firma de INFILE: {}", e.getMessage(), e);
            return respuestaFallida("Error ajeno a INFILE al invocar el servicio de firma: " + e.getMessage());
        }
    }

    private RespuestaFirma parsearRespuesta(String body, int codigoHttp) {
        if (body == null || body.isEmpty()) {
            return respuestaFallida("El servicio de firma de INFILE devolvió una respuesta vacía. HTTP: " + codigoHttp);
        }
        try {
            JsonNode json = objectMapper.readTree(body);
            if (json.get("resultado") == null) {
                RespuestaFirma respuesta = respuestaFallida(
                        "Respuesta inesperada del servicio de firma de INFILE. HTTP: " + codigoHttp);
                respuesta.setJson_respuesta(body);
                return respuesta;
            }
            RespuestaFirma respuesta = objectMapper.treeToValue(json, RespuestaFirma.class);
            respuesta.setJson_respuesta(body);
            return respuesta;
        } catch (Exception e) {
            log.error("No se pudo interpretar la respuesta del servicio de firma: {}", e.getMessage(), e);
            RespuestaFirma respuesta = respuestaFallida(
                    "Error al interpretar la respuesta del servicio de firma: " + e.getMessage());
            respuesta.setJson_respuesta(body);
            return respuesta;
        }
    }

    private RespuestaFirma respuestaFallida(String descripcion) {
        RespuestaFirma respuesta = new RespuestaFirma();
        respuesta.setResultado(false);
        respuesta.setDescripcion(descripcion);
        return respuesta;
    }
}
