package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@ToString(exclude = "producto")
@Entity
@Table(name = "despachos_bodega_detalle")
public class DespachoBodegaDetalle implements Serializable {

	private static final long serialVersionUID = -6402157390318772041L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idDetalle;

	private Integer cantidad;

	/** Costo del producto al momento del despacho; el servidor lo asigna, no se toma del cliente. */
	private BigDecimal precioUnitario;
	private BigDecimal subTotal;

	/** Existencia que tenía la bodega justo antes de descontar esta línea. */
	private Integer existenciaBodega;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_producto")
	@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
	private Producto producto;

}
