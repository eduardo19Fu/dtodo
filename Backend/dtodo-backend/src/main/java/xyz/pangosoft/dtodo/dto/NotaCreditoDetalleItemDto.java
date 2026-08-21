package xyz.pangosoft.dtodo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Línea ligera utilizada exclusivamente en la visualización de una nota de crédito.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotaCreditoDetalleItemDto {
    private Integer idNotaDetalle;
    private Integer idProducto;
    private String codProducto;
    private String producto;
    private BigDecimal subTotal;
    private int cantidad;
    private double descuento;
    private BigDecimal subTotalDescuento;
}
