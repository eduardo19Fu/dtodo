package xyz.pangosoft.dtodo.fel.client;

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
import xyz.pangosoft.dtodo.fel.dto.RequestCertificacion;
import xyz.pangosoft.dtodo.fel.dto.RespuestaCertificacion;

/**
 * Cliente del servicio de certificación/anulación de DTE de INFILE.
 *
 * <p>Reemplazo de {@code com.fel.validaciones.documento.ServicioFel}. Las
 * credenciales viajan en headers ({@code usuario}/{@code llave}) y el XML
 * firmado en el body JSON. Los fallos de red o de parseo devuelven
 * {@code resultado=false} en lugar de lanzar excepción, igual que el conector
 * original — el código HTTP no determina el éxito, solo el campo
 * {@code resultado} del JSON.</p>
 */
@Component
@Slf4j
public class FelCertificadorClient {

    public static final String TRANSACCION_CERTIFICACION = "CERTIFICACION";
    public static final String TRANSACCION_ANULACION = "ANULACION";

    /** User-Agent fijo que enviaba ConectorJava.jar; se conserva por compatibilidad. */
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11";

    private final RestTemplate felRestTemplate;
    private final ObjectMapper objectMapper;
    private final FelInfileProperties properties;

    public FelCertificadorClient(@Qualifier("felRestTemplate") RestTemplate felRestTemplate,
                                 ObjectMapper objectMapper,
                                 FelInfileProperties properties) {
        this.felRestTemplate = felRestTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * Envía el DTE firmado al certificador INFILE.
     *
     * @param xmlDte           XML firmado tal como lo devolvió el servicio de firma (Base64)
     * @param usuario          usuario ante INFILE ({@code certificador.prefijo})
     * @param llave            llave del web service ({@code certificador.llaveWs})
     * @param identificador    identificador único de la transacción
     * @param tipoTransaccion  {@link #TRANSACCION_CERTIFICACION} o {@link #TRANSACCION_ANULACION}
     */
    public RespuestaCertificacion certificar(String xmlDte, String usuario, String llave,
                                             String identificador, String tipoTransaccion) {
        String url = resolverUrl(tipoTransaccion);

        // nit_emisor y correo_copia viajan vacíos a propósito: ver RequestCertificacion.
        RequestCertificacion request = new RequestCertificacion();
        request.setXml_dte(xmlDte);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("usuario", usuario);
        headers.set("llave", llave);
        headers.set("identificador", identificador);
        headers.set(HttpHeaders.USER_AGENT, USER_AGENT);

        try {
            ResponseEntity<String> response = felRestTemplate.postForEntity(
                    url, new HttpEntity<>(request, headers), String.class);
            log.info("Certificador INFILE ({}) respondió HTTP {}", tipoTransaccion, response.getStatusCodeValue());
            return parsearRespuesta(response.getBody(), response.getStatusCodeValue());
        } catch (RestClientException e) {
            log.error("No se pudo invocar el servicio de {} de INFILE: {}", tipoTransaccion, e.getMessage(), e);
            return respuestaFallida("Error ajeno a INFILE al invocar el servicio. "
                    + "Este equipo tiene inconvenientes de comunicacion: " + e.getMessage());
        }
    }

    private String resolverUrl(String tipoTransaccion) {
        if (TRANSACCION_ANULACION.equals(tipoTransaccion)) {
            return properties.getAnulacionUrl();
        }
        if (TRANSACCION_CERTIFICACION.equals(tipoTransaccion)) {
            return properties.getCertificacionUrl();
        }
        throw new IllegalArgumentException("Tipo de transacción FEL desconocido: " + tipoTransaccion);
    }

    private RespuestaCertificacion parsearRespuesta(String body, int codigoHttp) {
        if (body == null || body.isEmpty()) {
            return respuestaFallida("El certificador INFILE devolvió una respuesta vacía. HTTP: " + codigoHttp);
        }
        try {
            JsonNode json = objectMapper.readTree(body);
            if (json.get("resultado") == null) {
                RespuestaCertificacion respuesta = respuestaFallida(
                        "Error ajeno a INFILE, al intentar invocar el servicio. HTTP: " + codigoHttp);
                respuesta.setJson_respuesta(body);
                return respuesta;
            }
            RespuestaCertificacion respuesta = objectMapper.treeToValue(json, RespuestaCertificacion.class);
            respuesta.setJson_respuesta(body);
            return respuesta;
        } catch (Exception e) {
            log.error("No se pudo interpretar la respuesta del certificador: {}", e.getMessage(), e);
            RespuestaCertificacion respuesta = respuestaFallida(
                    "Error al interpretar la respuesta del certificador: " + e.getMessage());
            respuesta.setJson_respuesta(body);
            return respuesta;
        }
    }

    private RespuestaCertificacion respuestaFallida(String info) {
        RespuestaCertificacion respuesta = new RespuestaCertificacion();
        respuesta.setResultado(false);
        respuesta.setInfo(info);
        return respuesta;
    }
}
