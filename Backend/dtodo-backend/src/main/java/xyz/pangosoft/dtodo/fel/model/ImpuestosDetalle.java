package xyz.pangosoft.dtodo.fel.model;

/**
 * Impuesto de una línea de detalle (nodo {@code dte:Impuesto}).
 * Reemplazo de {@code com.fel.validaciones.documento.ImpuestosDetalle}.
 */
public class ImpuestosDetalle {

    private String NombreCorto;
    private Integer CodigoUnidadGravable;
    private Double MontoGravable;
    private Double CantidadUnidadesGravables;
    private Double MontoImpuesto;

    public Double getCantidadUnidadesGravables() {
        return CantidadUnidadesGravables;
    }

    public void setCantidadUnidadesGravables(Double cantidadUnidadesGravables) {
        this.CantidadUnidadesGravables = cantidadUnidadesGravables;
    }

    public String getNombreCorto() {
        return NombreCorto;
    }

    public void setNombreCorto(String nombreCorto) {
        this.NombreCorto = nombreCorto;
    }

    public Integer getCodigoUnidadGravable() {
        return CodigoUnidadGravable;
    }

    public void setCodigoUnidadGravable(Integer codigoUnidadGravable) {
        this.CodigoUnidadGravable = codigoUnidadGravable;
    }

    public Double getMontoGravable() {
        return MontoGravable;
    }

    public void setMontoGravable(Double montoGravable) {
        this.MontoGravable = montoGravable;
    }

    public Double getMontoImpuesto() {
        return MontoImpuesto;
    }

    public void setMontoImpuesto(Double montoImpuesto) {
        this.MontoImpuesto = montoImpuesto;
    }
}
