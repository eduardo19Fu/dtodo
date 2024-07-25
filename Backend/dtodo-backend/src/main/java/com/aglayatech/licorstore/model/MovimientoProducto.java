package com.aglayatech.licorstore.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import com.aglayatech.licorstore.model.enums.TipoMovimientoEnum;
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
@Table(name = "movimientos_producto")
public class MovimientoProducto implements Serializable {

	private static final long serialVersionUID = 776971246070242035L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idMovimiento;
	private Integer cantidad;
	private Integer stockInicial;
	private LocalDateTime fechaMovimiento;

	@Enumerated(EnumType.STRING)
	private TipoMovimientoEnum tipoMovimiento;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_producto")
	@JsonIgnoreProperties({ "movimientos", "hibernateLazyInitializer", "handler" })
	private Producto producto;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario")
	@JsonIgnoreProperties({"password", "roles", "hibernateLazyInitializer", "handler" })
	private Usuario usuario;
	
	@PrePersist
	public void configFecha() {
		this.fechaMovimiento = LocalDateTime.now();
	}

}
