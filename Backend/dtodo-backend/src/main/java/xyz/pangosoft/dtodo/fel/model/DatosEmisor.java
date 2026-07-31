package xyz.pangosoft.dtodo.fel.model;

/**
 * Datos del emisor del DTE (nodo {@code dte:Emisor}).
 * Reemplazo de {@code com.fel.validaciones.documento.DatosEmisor}.
 */
public class DatosEmisor {

    private String CorreoEmisor;
    private Integer CodigoEstablecimiento;
    private String NITEmisor;
    private String NombreComercial;
    private String AfiliacionIVA;
    private String NombreEmisor;
    private String Direccion;
    private String CodigoPostal;
    private String Municipio;
    private String Departamento;
    private String Pais;

    public String getCorreoEmisor() {
        return CorreoEmisor;
    }

    public void setCorreoEmisor(String correoEmisor) {
        this.CorreoEmisor = correoEmisor;
    }

    public Integer getCodigoEstablecimiento() {
        return CodigoEstablecimiento;
    }

    public void setCodigoEstablecimiento(Integer codigoEstablecimiento) {
        this.CodigoEstablecimiento = codigoEstablecimiento;
    }

    public String getNITEmisor() {
        return NITEmisor;
    }

    public void setNITEmisor(String nitEmisor) {
        this.NITEmisor = nitEmisor;
    }

    public String getNombreComercial() {
        return NombreComercial;
    }

    public void setNombreComercial(String nombreComercial) {
        this.NombreComercial = nombreComercial;
    }

    public String getAfiliacionIVA() {
        return AfiliacionIVA;
    }

    public void setAfiliacionIVA(String afiliacionIVA) {
        this.AfiliacionIVA = afiliacionIVA;
    }

    public String getNombreEmisor() {
        return NombreEmisor;
    }

    public void setNombreEmisor(String nombreEmisor) {
        this.NombreEmisor = nombreEmisor;
    }

    public String getDireccion() {
        return Direccion;
    }

    public void setDireccion(String direccion) {
        this.Direccion = direccion;
    }

    public String getCodigoPostal() {
        return CodigoPostal;
    }

    public void setCodigoPostal(String codigoPostal) {
        this.CodigoPostal = codigoPostal;
    }

    public String getMunicipio() {
        return Municipio;
    }

    public void setMunicipio(String municipio) {
        this.Municipio = municipio;
    }

    public String getDepartamento() {
        return Departamento;
    }

    public void setDepartamento(String departamento) {
        this.Departamento = departamento;
    }

    public String getPais() {
        return Pais;
    }

    public void setPais(String pais) {
        this.Pais = pais;
    }
}
