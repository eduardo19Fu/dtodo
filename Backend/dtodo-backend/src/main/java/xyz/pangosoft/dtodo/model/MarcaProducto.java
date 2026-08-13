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
@Table(name = "marcas_producto")
public class MarcaProducto implements Serializable {

	private static final long serialVersionUID = 6163462751993275676L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer idMarcaProducto;

	@NotEmpty
	private String marca;
	private LocalDateTime fechaRegistro;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario")
	@JsonIgnoreProperties({ "password", "roles", "fecha_registro","hibernateLazyInitializer", "handler" })
	private Usuario usuario;

	@PrePersist
	public void configFechaRegistro() {
		this.fechaRegistro = LocalDateTime.now();
	}

}
