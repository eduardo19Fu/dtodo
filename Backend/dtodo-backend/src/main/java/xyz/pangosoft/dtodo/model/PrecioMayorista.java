package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Precio mayorista fijo de un producto. Es opcional: solo existe para los productos a los que los usuarios se lo
 * asignan. Vive en su propia tabla para no modificar {@code productos}, cuyas columnas devuelve un procedimiento
 * almacenado que se mapea directo a la entidad {@link Producto}.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "precios_mayorista")
public class PrecioMayorista implements Serializable {

	private static final long serialVersionUID = 2264188309751183017L;

	/** Es el id del producto: cada producto tiene, como máximo, un precio mayorista. */
	@Id
	private Integer idProducto;

	private BigDecimal precio;
}
