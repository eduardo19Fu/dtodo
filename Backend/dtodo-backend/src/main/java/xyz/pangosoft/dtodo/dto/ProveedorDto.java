package xyz.pangosoft.dtodo.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProveedorDto {
	private Integer idProveedor;
	private String nombre;
	private String contacto;
	private String telefonoEntidad;
	private String telefonoContacto;
	private String emailEntidad;
	private String emailContacto;
	private String direccion;
	private String sitioWeb;
	private String pais;
	private String estado;
	private LocalDateTime fechaRegistro;
	private String usuario;
	private String registradoPor;
}
