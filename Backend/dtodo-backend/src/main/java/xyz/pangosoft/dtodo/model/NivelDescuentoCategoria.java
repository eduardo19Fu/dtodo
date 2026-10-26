package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.pangosoft.dtodo.model.enums.CategoriaClienteEnum;

/**
 * Un porcentaje de descuento que puede recibir un cliente de una categoría. A y B tienen uno; Volumen varios,
 * y el vendedor elige cuál aplicar en cada línea.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "categorias_descuento_niveles")
public class NivelDescuentoCategoria implements Serializable {

	private static final long serialVersionUID = 6641382908112270431L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer idNivel;

	@Enumerated(EnumType.STRING)
	private CategoriaClienteEnum categoria;

	private BigDecimal porcentaje;

	@Builder.Default
	private boolean activo = true;
}
