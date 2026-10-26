package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lo que se guarda para un producto: su precio mayorista (nulo para quitarlo) y sus descuentos especiales. La
 * lista reemplaza a la anterior: un descuento que no viene se elimina.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GuardarDescuentosProductoRequest {

    private BigDecimal precioMayorista;
    private List<DescuentoProductoDto> descuentos;
}
