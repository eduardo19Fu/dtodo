package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;

public interface DetalleDocumentoDto {
    Long getIdDetalle();
    String getProducto();
    Integer getCantidad();
    BigDecimal getSubTotal();
    Number getDescuento();
    BigDecimal getSubTotalDescuento();
}
