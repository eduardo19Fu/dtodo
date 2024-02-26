package com.aglayatech.licorstore.model;

import java.io.Serializable;
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

@Entity
@Table(name = "movimientos_producto")
public class MovimientoProducto implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idMovimiento;
	private String tipoMovimiento;
	private Integer cantidad;
	private Integer stockInicial;

	@Temporal(TemporalType.TIMESTAMP)
	private Date fechaMovimiento;

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
		this.fechaMovimiento = new Date();
	}

	public Long getIdMovimiento() {
		return idMovimiento;
	}

	public void setIdMovimiento(Long idMovimiento) {
		this.idMovimiento = idMovimiento;
	}

	public String getTipoMovimiento() {
		return tipoMovimiento;
	}

	public void setTipoMovimiento(String tipoMovimiento) {
		this.tipoMovimiento = tipoMovimiento;
	}

	public Integer getCantidad() {
		return cantidad;
	}

	public void setCantidad(Integer cantidad) {
		this.cantidad = cantidad;
	}

	public Date getFechaMovimiento() {
		return fechaMovimiento;
	}

	public void setFechaMovimiento(Date fechaMovimiento) {
		this.fechaMovimiento = fechaMovimiento;
	}

	public Producto getProducto() {
		return producto;
	}

	public void setProducto(Producto producto) {
		this.producto = producto;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public Integer getStockInicial() {
		return stockInicial;
	}

	public void setStockInicial(Integer stockInicial) {
		this.stockInicial = stockInicial;
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

	private static final long serialVersionUID = 1L;

}
