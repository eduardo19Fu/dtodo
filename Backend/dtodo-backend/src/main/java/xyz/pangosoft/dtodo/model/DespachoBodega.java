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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import xyz.pangosoft.dtodo.model.enums.EstadoDespachoBodegaEnum;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = { "bodega", "sucursalDestino", "bodegaDestino", "usuarioDespacha", "usuarioResuelve", "items" })
@Entity
@Table(name = "despachos_bodega")
public class DespachoBodega implements Serializable {

	private static final long serialVersionUID = 4977120483371605263L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idDespacho;

	private LocalDateTime fechaRegistro;

	/** Fecha en que el despacho fue aprobado o cancelado; nula mientras está pendiente. */
	private LocalDateTime fechaResolucion;

	@Enumerated(EnumType.STRING)
	private EstadoDespachoBodegaEnum estado;

	private BigDecimal total;

	/** Nombre de quien recibe la mercadería; lo captura el usuario que despacha. */
	private String recibidoPor;

	private String observaciones;
	private String motivoCancelacion;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_bodega")
	@JsonIgnoreProperties({ "usuario", "hibernateLazyInitializer", "handler" })
	private Bodega bodega;

	/** Sucursal que recibe el despacho; nula cuando el destino es otra bodega. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_sucursal_destino")
	@JsonIgnoreProperties({ "usuario", "hibernateLazyInitializer", "handler" })
	private Sucursal sucursalDestino;

	/** Bodega que recibe el despacho (traslado entre bodegas); nula cuando el destino es una sucursal. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_bodega_destino")
	@JsonIgnoreProperties({ "usuario", "hibernateLazyInitializer", "handler" })
	private Bodega bodegaDestino;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario_despacha")
	@JsonIgnoreProperties({ "password", "roles", "sucursal", "hibernateLazyInitializer", "handler" })
	private Usuario usuarioDespacha;

	/** Usuario que aprobó o canceló el despacho. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario_resuelve")
	@JsonIgnoreProperties({ "password", "roles", "sucursal", "hibernateLazyInitializer", "handler" })
	private Usuario usuarioResuelve;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "id_despacho")
	@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
	@Builder.Default
	private List<DespachoBodegaDetalle> items = new ArrayList<>();

	@PrePersist
	public void configFechaRegistro() {
		this.fechaRegistro = LocalDateTime.now();
	}

}
