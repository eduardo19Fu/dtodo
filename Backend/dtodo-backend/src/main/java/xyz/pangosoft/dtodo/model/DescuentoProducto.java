package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import xyz.pangosoft.dtodo.model.enums.TipoDescuentoEnum;

/**
 * Descuento especial de un producto al comprar por ciento ({@code CIENTO}) o por fardo cerrado ({@code FARDO}).
 * Para el fardo, {@code cantidadMinima} son las unidades que trae el fardo de ese producto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = { "producto" })
@Entity
@Table(name = "descuentos_producto")
public class DescuentoProducto implements Serializable {

	private static final long serialVersionUID = -3187405163905581492L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer idDescuento;

	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_producto")
	private Producto producto;

	@Enumerated(EnumType.STRING)
	private TipoDescuentoEnum tipo;

	private Integer cantidadMinima;

	private BigDecimal porcentaje;

	@Builder.Default
	private boolean activo = true;
}
