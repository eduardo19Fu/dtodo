package xyz.pangosoft.dtodo.fel.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Detalle de un error devuelto por el certificador INFILE.
 * Reemplazo de {@code com.fel.validaciones.documento.DescripcionErrores}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DescripcionErrorFel {

    @JsonProperty("resultado")
    private String resultado;

    @JsonProperty("fuente")
    private String fuente;

    @JsonProperty("categoria")
    private String categoria;

    @JsonProperty("numeral")
    private String numeral;

    @JsonProperty("validacion")
    private String validacion;

    @JsonProperty("mensaje_error")
    private String mensaje_error;

    public String getMensaje_error() {
        return mensaje_error;
    }

    public void setMensaje_error(String mensaje_error) {
        this.mensaje_error = mensaje_error;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getFuente() {
        return fuente;
    }

    public void setFuente(String fuente) {
        this.fuente = fuente;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getNumeral() {
        return numeral;
    }

    public void setNumeral(String numeral) {
        this.numeral = numeral;
    }

    public String getValidacion() {
        return validacion;
    }

    public void setValidacion(String validacion) {
        this.validacion = validacion;
    }
}
