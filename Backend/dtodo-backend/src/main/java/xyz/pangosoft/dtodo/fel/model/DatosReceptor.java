package xyz.pangosoft.dtodo.fel.model;

/**
 * Datos del receptor del DTE (nodo {@code dte:Receptor}).
 * Reemplazo de {@code com.fel.validaciones.documento.DatosReceptor}.
 */
public class DatosReceptor {

    private String IDReceptor;
    private String CorreoReceptor;
    private String NombreReceptor;
    private String Direccion;
    private String CodigoPostal;
    private String Municipio;
    private String Departamento;
    private String Pais;
    private String TipoEspecial;

    public String getIDReceptor() {
        return IDReceptor;
    }

    public void setIDReceptor(String idReceptor) {
        this.IDReceptor = idReceptor;
    }

    public String getCorreoReceptor() {
        return CorreoReceptor;
    }

    public void setCorreoReceptor(String correoReceptor) {
        this.CorreoReceptor = correoReceptor;
    }

    public String getNombreReceptor() {
        return NombreReceptor;
    }

    public void setNombreReceptor(String nombreReceptor) {
        this.NombreReceptor = nombreReceptor;
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

    public String getTipoEspecial() {
        return TipoEspecial;
    }

    public void setTipoEspecial(String tipoEspecial) {
        this.TipoEspecial = tipoEspecial;
    }
}
