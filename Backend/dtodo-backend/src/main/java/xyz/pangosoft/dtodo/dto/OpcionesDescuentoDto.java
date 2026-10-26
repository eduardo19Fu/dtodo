package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.pangosoft.dtodo.model.enums.CategoriaClienteEnum;

/** Descuentos que puede recibir un producto para un cliente y una cantidad, con la sugerencia del sistema. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OpcionesDescuentoDto {

    private Integer idProducto;
    private BigDecimal precioVenta;

    /** Precio mayorista fijo del producto, si lo tiene. */
    private BigDecimal precioMayorista;

    /** Categoría del cliente mayorista; nula si el cliente no es mayorista o no tiene categoría. */
    private CategoriaClienteEnum categoria;
    private int cantidad;
    private List<OpcionDescuentoDto> opciones;

    /** La opción que el sistema preselecciona; nula si no hay ninguna con porcentaje definido. */
    private OpcionDescuentoDto sugerida;
}
