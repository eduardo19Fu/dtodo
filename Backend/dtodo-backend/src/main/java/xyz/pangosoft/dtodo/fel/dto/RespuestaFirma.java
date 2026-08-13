package xyz.pangosoft.dtodo.fel.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Respuesta del servicio de firma de INFILE.
 * Reemplazo de {@code com.fel.firma.emisor.RespuestaServicioFirma}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RespuestaFirma {

    @JsonProperty("resultado")
    private boolean resultado;

    @JsonProperty("descripcion")
    private String descripcion;

    /** XML firmado codificado en Base64. */
    @JsonProperty("archivo")
    private String archivo;

    /** Cuerpo JSON crudo devuelto por INFILE, para trazabilidad. */
    @JsonIgnore
    private String json_respuesta;

    public String getJson_respuesta() {
        return json_respuesta;
    }

    public void setJson_respuesta(String json_respuesta) {
        this.json_respuesta = json_respuesta;
    }

    public boolean isResultado() {
        return resultado;
    }

    public void setResultado(boolean resultado) {
        this.resultado = resultado;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getArchivo() {
        return archivo;
    }

    public void setArchivo(String archivo) {
        this.archivo = archivo;
    }
}
