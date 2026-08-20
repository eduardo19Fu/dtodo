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
}
