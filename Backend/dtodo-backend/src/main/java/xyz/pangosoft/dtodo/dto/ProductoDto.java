package xyz.pangosoft.dtodo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

public interface ProductoDto {
    Integer getIdProducto();
    String getCodProducto();
    String getNombre();
    BigDecimal getPrecioCompra();
    BigDecimal getPrecioVenta();
    float getPorcentajeGanancia();
    String getDescripcion();
    Date getFechaVencimiento();
    Date getFechaIngreso();
    Date getFechaRegistro();
    int getStock();
    String getMarcaProducto();
    String getTipoProducto();
    String getEstado();
}
