package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DetalleDocumentoDto {
    private Long idDetalle;
    private Integer idProducto;
    private String codigoProducto;
    private String producto;
    private Integer cantidad;
    private BigDecimal subTotal;
    private Number descuento;
    private BigDecimal subTotalDescuento;

    /** Observación de la línea; solo la tienen las proformas. */
    private String observacion;

    public DetalleDocumentoDto(Long idDetalle, Integer idProducto, String codigoProducto, String producto,
                               Integer cantidad, BigDecimal subTotal, Number descuento, BigDecimal subTotalDescuento) {
        this(idDetalle, idProducto, codigoProducto, producto, cantidad, subTotal, descuento, subTotalDescuento, null);
    }
}
