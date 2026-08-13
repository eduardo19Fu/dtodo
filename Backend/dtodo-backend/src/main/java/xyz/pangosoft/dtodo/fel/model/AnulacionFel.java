package xyz.pangosoft.dtodo.fel.model;

/**
 * Solicitud de anulación de un DTE ya certificado ({@code dte:GTAnulacionDocumento}).
 * Reemplazo de {@code com.fel.validaciones.documento.AnulacionFel}.
 */
public class AnulacionFel {

    private String FechaHoraAnulacion;
    private String NITEmisor;
    private String FechaEmisionDocumentoAnular;
    private String IDReceptor;
    private String NumeroDocumentoAAnular;
    private String MotivoAnulacion;

    public String getFechaHoraAnulacion() {
        return FechaHoraAnulacion;
    }

    public void setFechaHoraAnulacion(String fechaHoraAnulacion) {
        this.FechaHoraAnulacion = fechaHoraAnulacion;
    }

    public String getNITEmisor() {
        return NITEmisor;
    }

    public void setNITEmisor(String nitEmisor) {
        this.NITEmisor = nitEmisor;
    }

    public String getFechaEmisionDocumentoAnular() {
        return FechaEmisionDocumentoAnular;
    }

    public void setFechaEmisionDocumentoAnular(String fechaEmisionDocumentoAnular) {
        this.FechaEmisionDocumentoAnular = fechaEmisionDocumentoAnular;
    }

    public String getIDReceptor() {
        return IDReceptor;
    }

    public void setIDReceptor(String idReceptor) {
        this.IDReceptor = idReceptor;
    }

    public String getNumeroDocumentoAAnular() {
        return NumeroDocumentoAAnular;
    }

    public void setNumeroDocumentoAAnular(String numeroDocumentoAAnular) {
        this.NumeroDocumentoAAnular = numeroDocumentoAAnular;
    }

    public String getMotivoAnulacion() {
        return MotivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.MotivoAnulacion = motivoAnulacion;
    }
}
