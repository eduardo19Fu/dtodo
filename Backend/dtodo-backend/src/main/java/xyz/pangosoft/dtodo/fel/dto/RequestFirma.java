package xyz.pangosoft.dtodo.fel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Cuerpo JSON del servicio de firma de INFILE
 * ({@code POST /sign_solicitud_firmas/firma_xml}).
 */
public class RequestFirma {

    @JsonProperty("llave")
    private String llave;

    /** XML del DTE codificado en Base64. */
    @JsonProperty("archivo")
    private String archivo;

    @JsonProperty("codigo")
    private String codigo;

    @JsonProperty("alias")
    private String alias;

    /** "S" para anulaciones ({@code GTAnulacionDocumento}), "N" en caso contrario. */
    @JsonProperty("es_anulacion")
    private String es_anulacion;

    public String getLlave() {
        return llave;
    }

    public void setLlave(String llave) {
        this.llave = llave;
    }

    public String getArchivo() {
        return archivo;
    }

    public void setArchivo(String archivo) {
        this.archivo = archivo;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getEs_anulacion() {
        return es_anulacion;
    }

    public void setEs_anulacion(String es_anulacion) {
        this.es_anulacion = es_anulacion;
    }
}
