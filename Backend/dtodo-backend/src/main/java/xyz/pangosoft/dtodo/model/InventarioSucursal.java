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
@Table(name = "inventario_sucursal")
public class InventarioSucursal implements Serializable {

	private static final long serialVersionUID = 4517558968823920262L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idInventarioSucursal;

	private int stock;
	private Integer stockMinimo;
	private LocalDateTime fechaActualizacion;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_sucursal")
	@JsonIgnoreProperties({ "usuario", "hibernateLazyInitializer", "handler" })
	private Sucursal sucursal;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_producto")
	@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
	private Producto producto;

	@PrePersist
	public void configFechaActualizacion() {
		this.fechaActualizacion = LocalDateTime.now();
	}

}
