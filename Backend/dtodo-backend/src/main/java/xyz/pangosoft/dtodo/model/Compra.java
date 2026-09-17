package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import xyz.pangosoft.dtodo.model.enums.EstadoCompraEnum;
import xyz.pangosoft.dtodo.model.enums.TipoComprobanteCompraEnum;

@Data
@AllArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "compras")
public class Compra implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idCompra;

	private LocalDate fechaCompra;
	private LocalDateTime fechaRegistro;
	private String noComprobante;

	@Enumerated(EnumType.STRING)
	private TipoComprobanteCompraEnum tipoComprobante;

	private BigDecimal total;
	private BigDecimal costoEnvio;
	private String observaciones;

	@Enumerated(EnumType.STRING)
	private EstadoCompraEnum estado;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_proveedor")
	@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
	private Proveedor proveedor;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario")
	@JsonIgnoreProperties({ "password", "roles", "sucursal", "hibernateLazyInitializer", "handler" })
	private Usuario usuario;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_sucursal")
	@JsonIgnoreProperties({ "usuario", "hibernateLazyInitializer", "handler" })
	private Sucursal sucursal;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "id_compra")
	@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
	private List<CompraDetalle> items;

	@PrePersist
	public void configFechaRegistro() {
		this.fechaRegistro = LocalDateTime.now();
	}

	public Compra() {
		this.items = new ArrayList<>();
	}

}
