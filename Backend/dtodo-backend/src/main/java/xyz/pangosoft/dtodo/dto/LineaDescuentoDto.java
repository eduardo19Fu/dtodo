package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;

import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.pangosoft.dtodo.model.enums.TipoDescuentoEnum;

/** Una línea de proforma cuyo descuento pasa del tope, para que quien autoriza vea a qué producto se le aplica. */
@Data
@NoArgsConstructor
public class LineaDescuentoDto {
    private String codigo;
    private String producto;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private Float descuento;
    private TipoDescuentoEnum tipoDescuento;
    /** Lo que se deja de cobrar por esa línea: precio de lista por cantidad menos el subtotal con descuento. */
    private BigDecimal importeDescuento;
}
