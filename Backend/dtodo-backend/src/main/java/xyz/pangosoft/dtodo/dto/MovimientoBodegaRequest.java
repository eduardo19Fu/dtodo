package xyz.pangosoft.dtodo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Datos para agregar producto a una bodega o reducir sus existencias. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoBodegaRequest {
	private Integer idProducto;
	private Integer cantidad;

	/** Solo aplica al agregar producto; si es nulo se conserva el stock mínimo actual. */
	private Integer stockMinimo;

	private String motivo;
}
