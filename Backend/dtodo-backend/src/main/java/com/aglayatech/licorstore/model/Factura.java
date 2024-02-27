package com.aglayatech.licorstore.model;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "facturas")
public class Factura implements Serializable {

	@Serial
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
	private LocalDateTime fecha;

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
		this.fecha = LocalDateTime.now();
	}

	public Double redondearPrecio(double precio){
		BigDecimal bd = new BigDecimal(precio);
		bd = bd.setScale(2,RoundingMode.HALF_UP);

		return bd.doubleValue();
	}

}
