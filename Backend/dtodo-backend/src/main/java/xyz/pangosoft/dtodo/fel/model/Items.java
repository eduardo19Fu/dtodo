package xyz.pangosoft.dtodo.fel.model;

import java.util.ArrayList;

/**
 * Línea de detalle del DTE (nodo {@code dte:Item}).
 * Reemplazo de {@code com.fel.validaciones.documento.Items}. El setter
 * {@link #setImpuestos_detalle} agrega a la lista, igual que en el conector original.
 */
public class Items {

    private Integer NumeroLinea;
    private String BienOServicio;
    private Double Cantidad;
    private String UnidadMedida;
    private String Descripcion;
    private Double PrecioUnitario;
    private Double Precio;
    private Double Descuento;
    private Double Total;
    private final ArrayList<ImpuestosDetalle> impuestos_detalle = new ArrayList<>();

    public Double getTotal() {
        return Total;
    }

    public void setTotal(Double total) {
        this.Total = total;
    }

    public Integer getNumeroLinea() {
        return NumeroLinea;
    }

    public void setNumeroLinea(Integer numeroLinea) {
        this.NumeroLinea = numeroLinea;
    }

    public String getBienOServicio() {
        return BienOServicio;
    }

    public void setBienOServicio(String bienOServicio) {
        this.BienOServicio = bienOServicio;
    }

    public Double getCantidad() {
        return Cantidad;
    }

    public void setCantidad(Double cantidad) {
        this.Cantidad = cantidad;
    }

    public String getUnidadMedida() {
        return UnidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.UnidadMedida = unidadMedida;
    }

    public String getDescripcion() {
        return Descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.Descripcion = descripcion;
    }

    public Double getPrecioUnitario() {
        return PrecioUnitario;
    }

    public void setPrecioUnitario(Double precioUnitario) {
        this.PrecioUnitario = precioUnitario;
    }

    public Double getPrecio() {
        return Precio;
    }

    public void setPrecio(Double precio) {
        this.Precio = precio;
    }

    public Double getDescuento() {
        return Descuento;
    }

    public void setDescuento(Double descuento) {
        this.Descuento = descuento;
    }

    public ArrayList<ImpuestosDetalle> getImpuestos_detalle() {
        return impuestos_detalle;
    }

    public void setImpuestos_detalle(ImpuestosDetalle impuestoDetalle) {
        this.impuestos_detalle.add(impuestoDetalle);
    }
}
