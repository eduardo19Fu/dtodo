package xyz.pangosoft.dtodo.fel.dto;

import java.util.List;

/**
 * Resultado de la generación/validación del XML de un DTE, previo al envío a INFILE.
 * Reemplazo de {@code com.fel.validaciones.documento.Respuesta}.
 */
public class RespuestaXml {

    private boolean resultado;
    private String descripcion;
    private int cantidad_errores;
    private List<String> errores;
    private String xml;

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public boolean getResultado() {
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

    public int getCantidad_errores() {
        return cantidad_errores;
    }

    public void setCantidad_errores(int cantidad_errores) {
        this.cantidad_errores = cantidad_errores;
    }

    public List<String> getErrores() {
        return errores;
    }

    public void setErrores(List<String> errores) {
        this.errores = errores;
    }
}
