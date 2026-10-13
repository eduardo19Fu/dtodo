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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
@ToString(exclude = { "sucursal", "estado", "usuario" })
@Entity
@Table(name = "bodegas")
public class Bodega implements Serializable {

	private static final long serialVersionUID = 3114729585123348307L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer idBodega;

	@NotBlank(message = "El nombre de la bodega no puede estar vacío.")
	@Size(max = 100, message = "El nombre de la bodega no puede superar los 100 caracteres.")
	private String nombre;

	@NotBlank(message = "La ubicación de la bodega no puede estar vacía.")
	@Size(max = 200, message = "La ubicación no puede superar los 200 caracteres.")
	private String ubicacion;

	@Size(max = 300, message = "La descripción no puede superar los 300 caracteres.")
	private String descripcion;

	@Size(max = 150, message = "El encargado no puede superar los 150 caracteres.")
	private String encargado;

	@Size(max = 15, message = "El teléfono no puede superar los 15 caracteres.")
	private String telefono;

	private LocalDateTime fechaRegistro;

	/** Sucursal a la que pertenece la bodega y destino por defecto de sus despachos (opcional). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_sucursal")
	@JsonIgnoreProperties({ "usuario", "hibernateLazyInitializer", "handler" })
	private Sucursal sucursal;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_estado")
	@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
	private Estado estado;

	/** Usuario que registró la bodega. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario")
	@JsonIgnoreProperties({ "password", "roles", "sucursal", "hibernateLazyInitializer", "handler" })
	private Usuario usuario;

	@PrePersist
	public void configFechaRegistro() {
		this.fechaRegistro = LocalDateTime.now();
	}

}
