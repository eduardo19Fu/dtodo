package xyz.pangosoft.dtodo.fel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * URLs de los servicios web de INFILE, configurables por perfil bajo el prefijo
 * {@code fel.infile.*}. Los valores por defecto son los endpoints de producción
 * que ConectorJava.jar traía embebidos.
 */
@ConfigurationProperties(prefix = "fel.infile")
public class FelInfileProperties {

    /** Servicio de firma electrónica del emisor. */
    private String firmaUrl = "https://signer-emisores.feel.com.gt/sign_solicitud_firmas/firma_xml";

    /** Servicio de certificación de DTE. */
    private String certificacionUrl = "https://certificador.feel-rarp.com/fel/certificacion/v2/dte/";

    /** Servicio de anulación de DTE. */
    private String anulacionUrl = "https://certificador.feel-rarp.com/fel/anulacion/v2/dte/";

    public String getFirmaUrl() {
        return firmaUrl;
    }

    public void setFirmaUrl(String firmaUrl) {
        this.firmaUrl = firmaUrl;
    }

    public String getCertificacionUrl() {
        return certificacionUrl;
    }

    public void setCertificacionUrl(String certificacionUrl) {
        this.certificacionUrl = certificacionUrl;
    }

    public String getAnulacionUrl() {
        return anulacionUrl;
    }

    public void setAnulacionUrl(String anulacionUrl) {
        this.anulacionUrl = anulacionUrl;
    }
}
