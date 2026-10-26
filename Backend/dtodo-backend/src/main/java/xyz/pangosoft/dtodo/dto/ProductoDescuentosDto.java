package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Precio mayorista y descuentos especiales de un producto, para la pantalla de descuentos. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductoDescuentosDto {

    private Integer idProducto;
    private String codProducto;
    private String nombre;
    private BigDecimal precioVenta;
    private BigDecimal precioMayorista;
    private List<DescuentoProductoDto> descuentos;
}
