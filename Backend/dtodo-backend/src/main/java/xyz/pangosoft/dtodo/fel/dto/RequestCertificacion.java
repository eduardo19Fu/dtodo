package xyz.pangosoft.dtodo.fel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Cuerpo JSON del servicio de certificación/anulación de INFILE
 * ({@code POST /fel/certificacion/v2/dte/} y {@code POST /fel/anulacion/v2/dte/}).
 *
 * <p>Nota: igual que el conector original, {@code nit_emisor} y {@code correo_copia}
 * se envían siempre vacíos — el NIT viaja dentro del XML firmado. Comportamiento
 * replicado deliberadamente del jar en producción.</p>
 */
public class RequestCertificacion {

    @JsonProperty("nit_emisor")
    private String nit_emisor = "";

    @JsonProperty("correo_copia")
    private String correo_copia = "";

    /** XML firmado (Base64) devuelto por el servicio de firma. */
    @JsonProperty("xml_dte")
    private String xml_dte;

    public String getNit_emisor() {
        return nit_emisor;
    }

    public void setNit_emisor(String nit_emisor) {
        this.nit_emisor = nit_emisor;
    }

    public String getCorreo_copia() {
        return correo_copia;
    }

    public void setCorreo_copia(String correo_copia) {
        this.correo_copia = correo_copia;
    }

    public String getXml_dte() {
        return xml_dte;
    }

    public void setXml_dte(String xml_dte) {
        this.xml_dte = xml_dte;
    }
}
