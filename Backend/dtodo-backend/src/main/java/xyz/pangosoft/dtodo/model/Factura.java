package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "facturas")
public class Factura implements Serializable {

	private static final long serialVersionUID = 2591750760215871716L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idFactura;
	private Long noFactura;
	private String serie;
	private BigDecimal total;
	private BigDecimal iva;
	private String correlativoSat;
	private String certificacionSat;
	private String serieSat;
	private String mensajeSat;
	private String fechaCertificacionSat;

	@Temporal(TemporalType.TIMESTAMP)
	private Date fecha;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_estado")
	@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
	private Estado estado;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario")
	@JsonIgnoreProperties({ "password", "roles", "hibernateLazyInitializer", "handler" })
	private Usuario usuario;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_cliente")
	@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
	private Cliente cliente;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "id_factura")
	@JsonIgnoreProperties({ "factura", "hibernateLazyInitializer", "handler" })
	private List<DetalleFactura> itemsFactura;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_tipo_factura")
	@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
	private TipoFactura tipoFactura; // TIPO FACTURA DEBE MOVERSE A ENUMERATION

	public Factura() {
		itemsFactura = new ArrayList<>();
	}

	@PrePersist
	public void initFecha() {
//		this.fecha = LocalDateTime.now();
		this.fecha = new Date();
	}

	public Double redondearPrecio(double precio){
		BigDecimal bd = new BigDecimal(precio);
		bd = bd.setScale(2,RoundingMode.HALF_UP);

		return bd.doubleValue();
	}

}
