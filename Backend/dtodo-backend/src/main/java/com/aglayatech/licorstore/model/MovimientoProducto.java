package com.aglayatech.licorstore.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
@NoArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "movimientos_producto")
public class MovimientoProducto implements Serializable {

	@Serial
	private static final long serialVersionUID = 776971246070242035L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idMovimiento;
	private String tipoMovimiento;
	private Integer cantidad;
	private Integer stockInicial;
	private LocalDateTime fechaMovimiento;

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

	public void calcularStock() {
		if(this.getTipoMovimiento().equals("ENTRADA") || this.getTipoMovimiento().equals("ANULACION FACTURA")) {
			int tempStock = this.producto.getStock();
			this.setStockInicial(tempStock);
			this.producto.setStock((tempStock + this.getCantidad()));
		} else if(this.getTipoMovimiento().equals("SALIDA") || this.getTipoMovimiento().equals("VENTA")){
			int tempStock = this.producto.getStock();
			this.setStockInicial(tempStock);
			this.producto.setStock((tempStock - this.getCantidad()));
		}
	}

}
