package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import xyz.pangosoft.dtodo.model.enums.TipoMovimientoBodegaEnum;

/** Registro inmutable de cada cambio de existencias en una bodega. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = { "bodega", "producto", "usuario" })
@Entity
@Table(name = "movimientos_bodega")
public class MovimientoBodega implements Serializable {

	private static final long serialVersionUID = 8260571183962273844L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idMovimiento;

	private LocalDateTime fechaMovimiento;

	@Enumerated(EnumType.STRING)
	private TipoMovimientoBodegaEnum tipoMovimiento;

	private Integer cantidad;
	private Integer stockInicial;
	private Integer stockFinal;
	private String motivo;

	@Column(name = "tipo_documento_origen", length = 30)
	private String tipoDocumentoOrigen;

	@Column(name = "id_documento_origen")
	private Long idDocumentoOrigen;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_bodega")
	@JsonIgnoreProperties({ "sucursal", "usuario", "hibernateLazyInitializer", "handler" })
	private Bodega bodega;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_producto")
	@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
	private Producto producto;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario")
	@JsonIgnoreProperties({ "password", "roles", "sucursal", "hibernateLazyInitializer", "handler" })
	private Usuario usuario;

	@PrePersist
	public void configFecha() {
		this.fechaMovimiento = LocalDateTime.now();
	}

}
