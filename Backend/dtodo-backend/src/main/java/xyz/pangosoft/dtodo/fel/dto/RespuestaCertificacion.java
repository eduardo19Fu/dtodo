package xyz.pangosoft.dtodo.fel.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Respuesta del servicio de certificación/anulación de INFILE.
 * Reemplazo de {@code com.fel.validaciones.documento.RespuestaServicioFel},
 * conservando los mismos nombres de accessors (incluido {@code getResultado()}
 * para el boolean, como en el original).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RespuestaCertificacion {

    @JsonProperty("resultado")
    private boolean resultado;

    @JsonProperty("fecha")
    private String fecha;

    @JsonProperty("origen")
    private String origen;

    @JsonProperty("descripcion")
    private String descripcion;

    @JsonProperty("descripcion_errores")
    private List<DescripcionErrorFel> descripcion_errores;

    @JsonProperty("alertas_infile")
    private boolean alertas_infile;

    @JsonProperty("descripcion_alertas_infile")
    private List<String> descripcion_alertas_infile;

    @JsonProperty("alertas_sat")
    private boolean alertas_sat;

    @JsonProperty("descripcion_alertas_sat")
    private List<String> descripcion_alertas_sat;

    @JsonProperty("cantidad_errores")
    private int cantidad_errores;

    @JsonProperty("control_emision")
    private ControlEmisionFel control_emision;

    @JsonProperty("informacion_adicional")
    private String informacion_adicional;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("serie")
    private String serie;

    @JsonProperty("numero")
    private String numero;

    @JsonProperty("xml_certificado")
    private String xml_certificado;

    @JsonProperty("info")
    private String info;

    /** Cuerpo JSON crudo devuelto por INFILE, para trazabilidad. */
    @JsonIgnore
    private String json_respuesta;

    public boolean getResultado() {
        return resultado;
    }

    public void setResultado(boolean resultado) {
        this.resultado = resultado;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public List<DescripcionErrorFel> getDescripcion_errores() {
        return descripcion_errores;
    }

    public void setDescripcion_errores(List<DescripcionErrorFel> descripcion_errores) {
        this.descripcion_errores = descripcion_errores;
    }

    public boolean isAlertas_infile() {
        return alertas_infile;
    }

    public void setAlertas_infile(boolean alertas_infile) {
        this.alertas_infile = alertas_infile;
    }

    public List<String> getDescripcion_alertas_infile() {
        return descripcion_alertas_infile;
    }

    public void setDescripcion_alertas_infile(List<String> descripcion_alertas_infile) {
        this.descripcion_alertas_infile = descripcion_alertas_infile;
    }

    public boolean isAlertas_sat() {
        return alertas_sat;
    }

    public void setAlertas_sat(boolean alertas_sat) {
        this.alertas_sat = alertas_sat;
    }

    public List<String> getDescripcion_alertas_sat() {
        return descripcion_alertas_sat;
    }

    public void setDescripcion_alertas_sat(List<String> descripcion_alertas_sat) {
        this.descripcion_alertas_sat = descripcion_alertas_sat;
    }

    public int getCantidad_errores() {
        return cantidad_errores;
    }

    public void setCantidad_errores(int cantidad_errores) {
        this.cantidad_errores = cantidad_errores;
    }

    public ControlEmisionFel getControl_emision() {
        return control_emision;
    }

    public void setControl_emision(ControlEmisionFel control_emision) {
        this.control_emision = control_emision;
    }

    public String getInformacion_adicional() {
        return informacion_adicional;
    }

    public void setInformacion_adicional(String informacion_adicional) {
        this.informacion_adicional = informacion_adicional;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getXml_certificado() {
        return xml_certificado;
    }

    public void setXml_certificado(String xml_certificado) {
        this.xml_certificado = xml_certificado;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getJson_respuesta() {
        return json_respuesta;
    }

    public void setJson_respuesta(String json_respuesta) {
        this.json_respuesta = json_respuesta;
    }
}
