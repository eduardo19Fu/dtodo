package xyz.pangosoft.dtodo.fel.model;

/**
 * Frase de régimen del DTE (nodo {@code dte:Frase}).
 * Reemplazo de {@code com.fel.validaciones.documento.Frases}.
 */
public class Frases {

    private Integer TipoFrase;
    private Integer CodigoEscenario;
    private String NumeroResolucion;
    private String FechaResolucion;

    public Integer getTipoFrase() {
        return TipoFrase;
    }

    public void setTipoFrase(Integer tipoFrase) {
        this.TipoFrase = tipoFrase;
    }

    public Integer getCodigoEscenario() {
        return CodigoEscenario;
    }

    public void setCodigoEscenario(Integer codigoEscenario) {
        this.CodigoEscenario = codigoEscenario;
    }

    public String getNumeroResolucion() {
        return NumeroResolucion;
    }

    public void setNumeroResolucion(String numeroResolucion) {
        this.NumeroResolucion = numeroResolucion;
    }

    public String getFechaResolucion() {
        return FechaResolucion;
    }

    public void setFechaResolucion(String fechaResolucion) {
        this.FechaResolucion = fechaResolucion;
    }
}
