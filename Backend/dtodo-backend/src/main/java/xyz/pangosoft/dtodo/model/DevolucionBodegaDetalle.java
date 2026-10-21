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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import xyz.pangosoft.dtodo.util.SoloAdministradorSerializer;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = "producto")
@Entity
@Table(name = "devoluciones_bodega_detalle")
public class DevolucionBodegaDetalle implements Serializable {

	private static final long serialVersionUID = 5530981462207316842L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idDetalle;

	private Integer cantidad;

	/** Costo del producto al momento de la devolución; el servidor lo asigna, no se toma del cliente. */
	@JsonSerialize(using = SoloAdministradorSerializer.class)
	private BigDecimal precioUnitario;
	@JsonSerialize(using = SoloAdministradorSerializer.class)
	private BigDecimal subTotal;

	/** Existencia que tenía la sucursal justo antes de descontar esta línea. */
	private Integer existenciaSucursal;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_producto")
	@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
	private Producto producto;

}
