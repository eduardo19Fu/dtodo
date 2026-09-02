package xyz.pangosoft.dtodo.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SucursalDto {
	private Integer idSucursal;
	private String nombre;
	private String direccion;
	private String telefono;
	private String encargado;
	private Integer codigoEstablecimientoSat;
	private boolean esPrincipal;
	private LocalDateTime fechaRegistro;
	private String estado;
	private String usuario;
}
