package xyz.pangosoft.dtodo.fel.model;

/**
 * Datos generales del DTE (nodo {@code dte:DatosGenerales}).
 * Reemplazo de {@code com.fel.validaciones.documento.DatosGenerales}.
 */
public class DatosGenerales {

    private String FechaHoraEmision;
    private Integer NumeroAcceso;
    private String CodigoMoneda;
    private String Tipo;
    private String Exportacion;
    private String TipoPersoneria;

    public String getTipoPersoneria() {
        return TipoPersoneria;
    }

    public void setTipoPersoneria(String tipoPersoneria) {
        this.TipoPersoneria = tipoPersoneria;
    }

    public String getFechaHoraEmision() {
        return FechaHoraEmision;
    }

    public void setFechaHoraEmision(String fechaHoraEmision) {
        this.FechaHoraEmision = fechaHoraEmision;
    }

    public Integer getNumeroAcceso() {
        return NumeroAcceso;
    }

    public void setNumeroAcceso(Integer numeroAcceso) {
        this.NumeroAcceso = numeroAcceso;
    }

    public String getCodigoMoneda() {
        return CodigoMoneda;
    }

    public void setCodigoMoneda(String codigoMoneda) {
        this.CodigoMoneda = codigoMoneda;
    }

    public String getTipo() {
        return Tipo;
    }

    public void setTipo(String tipo) {
        this.Tipo = tipo;
    }

    public String getExportacion() {
        return Exportacion;
    }

    public void setExportacion(String exportacion) {
        this.Exportacion = exportacion;
    }
}
