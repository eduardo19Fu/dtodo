package xyz.pangosoft.dtodo.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos que el cliente envía para registrar una devolución de tienda a bodega.
 *
 * <p>Solo se aceptan identificadores, cantidades y textos: el costo, la existencia, el estado y los usuarios
 * los determina el servidor.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DevolucionBodegaRequest {

	/** Sucursal (tienda) que devuelve el producto. */
	private Integer idSucursalOrigen;

	/** Bodega que recibe el producto devuelto. */
	private Integer idBodega;

	private String entregadoPor;
	private String motivo;
	private String observaciones;
	private List<Linea> items = new ArrayList<>();

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Linea {
		private Integer idProducto;
		private Integer cantidad;
	}
}
