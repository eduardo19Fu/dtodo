package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "clientes")
public class Cliente implements Serializable {

	private static final long serialVersionUID = 195366071153481819L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer idCliente;
	private String nombre;
	private String nit;
	private String direccion;
	private String telefono;
	private LocalDateTime fechaRegistro;

	@PrePersist
	public void configFechaRegistro() {
		if (fechaRegistro == null) {
			fechaRegistro = LocalDateTime.now();
		}

		setNombre(nombre.toUpperCase());
		setNit(nit.toUpperCase());
		setDireccion(direccion.toUpperCase());
	}

}
