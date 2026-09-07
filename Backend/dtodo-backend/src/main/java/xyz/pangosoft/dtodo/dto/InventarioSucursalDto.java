package xyz.pangosoft.dtodo.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventarioSucursalDto {
	private Long idInventarioSucursal;
	private Integer idProducto;
	private String codProducto;
	private String nombreProducto;
	private int stock;
	private Integer stockMinimo;
	private LocalDateTime fechaActualizacion;
}
