package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.math.BigDecimal;
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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import xyz.pangosoft.dtodo.model.enums.EstadoDespachoBodegaEnum;
import xyz.pangosoft.dtodo.util.SoloAdministradorSerializer;

/**
 * Devolución de producto desde una sucursal (tienda) hacia una bodega. Comparte con el despacho de bodega el
 * ciclo de estados: {@code PENDIENTE} hasta que un administrador la aprueba ({@code REALIZADO}) o se rechaza
 * ({@code CANCELADO}).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = { "sucursalOrigen", "bodega", "usuarioRegistra", "usuarioResuelve", "items" })
@Entity
@Table(name = "devoluciones_bodega")
public class DevolucionBodega implements Serializable {

	private static final long serialVersionUID = 2814035771628459027L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idDevolucion;

	private LocalDateTime fechaRegistro;

	/** Fecha en que la devolución fue aprobada o rechazada; nula mientras está pendiente. */
	private LocalDateTime fechaResolucion;

	@Enumerated(EnumType.STRING)
	private EstadoDespachoBodegaEnum estado;

	/** Valor de lo devuelto al costo; solo lo ve el administrador. */
	@JsonSerialize(using = SoloAdministradorSerializer.class)
	private BigDecimal total;

	/** Nombre de quien entrega la mercadería en la tienda. */
	private String entregadoPor;

	/** Razón por la que la tienda devuelve el producto (sobrestock, producto dañado, etc.). */
	private String motivo;

	private String observaciones;

	/** Motivo por el que se rechazó o canceló la devolución. */
	private String motivoCancelacion;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_sucursal_origen")
	@JsonIgnoreProperties({ "usuario", "hibernateLazyInitializer", "handler" })
	private Sucursal sucursalOrigen;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_bodega")
	@JsonIgnoreProperties({ "usuario", "hibernateLazyInitializer", "handler" })
	private Bodega bodega;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario_registra")
	@JsonIgnoreProperties({ "password", "roles", "sucursal", "hibernateLazyInitializer", "handler" })
	private Usuario usuarioRegistra;

	/** Usuario que aprobó o rechazó la devolución. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario_resuelve")
	@JsonIgnoreProperties({ "password", "roles", "sucursal", "hibernateLazyInitializer", "handler" })
	private Usuario usuarioResuelve;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "id_devolucion")
	@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
	@Builder.Default
	private List<DevolucionBodegaDetalle> items = new ArrayList<>();

	@PrePersist
	public void configFechaRegistro() {
		this.fechaRegistro = LocalDateTime.now();
	}

}
