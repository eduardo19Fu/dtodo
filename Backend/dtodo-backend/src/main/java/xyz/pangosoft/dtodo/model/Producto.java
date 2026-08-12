package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;

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
@Table(name = "productos")
public class Producto implements Serializable {

	private static final long serialVersionUID = -5903517381510772590L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer idProducto;

	private String codProducto;
	private String nombre;
	private BigDecimal precioCompra;
	private BigDecimal precioVenta;
	private float porcentajeGanancia;
	private String imagen;
	private String descripcion;
	private String link;

	@Temporal(TemporalType.DATE)
	private Date fechaVencimiento;

	@Temporal(TemporalType.DATE)
	private Date fechaIngreso;

	@Temporal(TemporalType.TIMESTAMP)
	private Date fechaRegistro;

	private int stock;

	@NotNull(message = "Marca no puede estar vacío.")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_marca_producto")
	@JsonIgnoreProperties({ "usuario", "hibernateLazyInitializer", "handler" })
	private MarcaProducto marcaProducto;

	@NotNull(message = "Tipo no puede estar vacío.")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_tipo_producto")
	@JsonIgnoreProperties({ "usuario", "hibernateLazyInitializer", "handler" })
	private TipoProducto tipoProducto;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_estado")
	@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
	private Estado estado;

	@PrePersist
	public void configFechaRegistro() {
		this.fechaRegistro = new Date();
	}
}
