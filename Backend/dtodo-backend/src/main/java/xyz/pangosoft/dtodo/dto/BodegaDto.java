package xyz.pangosoft.dtodo.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Fila del listado de bodegas, con el resumen de su inventario. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BodegaDto {
	private Integer idBodega;
	private String nombre;
	private String ubicacion;
	private String descripcion;
	private String encargado;
	private String telefono;
	private LocalDateTime fechaRegistro;
	private String estado;
	private Integer idSucursal;
	private String sucursal;
	private String usuario;
	private Long totalProductos;
	private Long totalUnidades;
}
