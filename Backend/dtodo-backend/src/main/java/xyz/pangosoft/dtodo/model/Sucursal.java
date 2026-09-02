package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@Table(name = "sucursales")
public class Sucursal implements Serializable {

	private static final long serialVersionUID = -8154611931032725581L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer idSucursal;

	@NotEmpty(message = "El nombre de la sucursal no puede estar vacío.")
	private String nombre;

	@NotEmpty(message = "La dirección de la sucursal no puede estar vacía.")
	private String direccion;

	private String telefono;
	private String encargado;

	@NotNull(message = "El código de establecimiento SAT no puede estar vacío.")
	private Integer codigoEstablecimientoSat;

	private boolean esPrincipal;
	private LocalDateTime fechaRegistro;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_estado")
	@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
	private Estado estado;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario")
	@JsonIgnoreProperties({ "password", "roles", "hibernateLazyInitializer", "handler" })
	private Usuario usuario;

	@PrePersist
	public void configFechaRegistro() {
		this.fechaRegistro = LocalDateTime.now();
	}

}
