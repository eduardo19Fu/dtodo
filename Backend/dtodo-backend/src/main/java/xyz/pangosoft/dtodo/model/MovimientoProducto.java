package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import xyz.pangosoft.dtodo.model.enums.TipoMovimientoEnum;
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
@Table(name = "movimientos_producto")
public class MovimientoProducto implements Serializable {

	private static final long serialVersionUID = 776971246070242035L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idMovimiento;
	private Integer cantidad;
	private Integer stockInicial;
	private LocalDateTime fechaMovimiento;

	@Enumerated(EnumType.STRING)
	private TipoMovimientoEnum tipoMovimiento;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_producto")
	@JsonIgnoreProperties({ "movimientos", "hibernateLazyInitializer", "handler" })
	private Producto producto;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario")
	@JsonIgnoreProperties({"password", "roles", "hibernateLazyInitializer", "handler" })
	private Usuario usuario;
	
	@PrePersist
	public void configFecha() {
		this.fechaMovimiento = LocalDateTime.now();
	}

}
