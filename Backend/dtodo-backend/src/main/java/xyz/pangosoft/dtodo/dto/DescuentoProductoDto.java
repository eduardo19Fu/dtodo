package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.pangosoft.dtodo.model.enums.TipoDescuentoEnum;

/** Descuento especial de un producto: {@code CIENTO} o {@code FARDO}, con su cantidad mínima y porcentaje. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DescuentoProductoDto {

    private TipoDescuentoEnum tipo;
    private Integer cantidadMinima;
    private BigDecimal porcentaje;
    private boolean activo = true;
}
