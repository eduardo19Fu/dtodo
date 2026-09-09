package xyz.pangosoft.dtodo.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import xyz.pangosoft.dtodo.model.Producto;

/**
 * Cuerpo de la petición para registrar un producto nuevo indicando en qué sucursales
 * debe quedar disponible desde el inicio (con su stock inicial respectivo).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductoRegistroRequest {
	private Producto producto;
	private List<SucursalStockDto> sucursales;
}
