package xyz.pangosoft.dtodo.fel.model;

/**
 * Resumen de impuestos del DTE (nodo {@code dte:TotalImpuesto}).
 * Reemplazo de {@code com.fel.validaciones.documento.TotalImpuestos}.
 */
public class TotalImpuestos {

    private String NombreCorto;
    private Double TotalMontoImpuesto;

    public String getNombreCorto() {
        return NombreCorto;
    }

    public void setNombreCorto(String nombreCorto) {
        this.NombreCorto = nombreCorto;
    }

    public Double getTotalMontoImpuesto() {
        return TotalMontoImpuesto;
    }

    public void setTotalMontoImpuesto(Double totalMontoImpuesto) {
        this.TotalMontoImpuesto = totalMontoImpuesto;
    }
}
