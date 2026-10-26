package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Ruta de venta. Agrupa a los clientes mayoristas de una zona y tiene, como máximo, un vendedor
 * responsable (que puede atender varias rutas).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = { "vendedor", "sucursal", "estado" })
@Entity
@Table(name = "rutas")
public class Ruta implements Serializable {

	private static final long serialVersionUID = 4021718226139907153L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer idRuta;

	@NotBlank(message = "El nombre de la ruta no puede estar vacío.")
	@Size(max = 100, message = "El nombre de la ruta admite hasta 100 caracteres.")
	private String nombre;

	@Size(max = 255, message = "La zona admite hasta 255 caracteres.")
	private String zona;

	@Size(max = 100, message = "Los días de visita admiten hasta 100 caracteres.")
	private String diasVisita;

	private LocalDateTime fechaRegistro;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_vendedor")
	@JsonIgnoreProperties({ "password", "roles", "sucursal", "hibernateLazyInitializer", "handler" })
	private Usuario vendedor;

	@NotNull(message = "La sucursal de la ruta no puede estar vacía.")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_sucursal")
	@JsonIgnoreProperties({ "usuario", "hibernateLazyInitializer", "handler" })
	private Sucursal sucursal;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_estado")
	@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
	private Estado estado;

	@PrePersist
	public void configFechaRegistro() {
		if (this.fechaRegistro == null) {
			this.fechaRegistro = LocalDateTime.now();
		}
	}
}
