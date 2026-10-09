package xyz.pangosoft.dtodo.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos que el cliente envía para registrar un despacho de bodega.
 *
 * <p>Solo se aceptan identificadores y cantidades: el precio, la existencia, el estado y los usuarios los
 * determina el servidor.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DespachoBodegaRequest {

	private Integer idBodega;

	/** Sucursal destino; si se omite se usa la sucursal asignada a la bodega. */
	private Integer idSucursalDestino;

	/** Bodega destino (traslado entre bodegas). No se combina con la sucursal destino. */
	private Integer idBodegaDestino;

	private String recibidoPor;
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
