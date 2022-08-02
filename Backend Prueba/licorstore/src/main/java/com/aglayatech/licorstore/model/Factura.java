package com.aglayatech.licorstore.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

@Entity
@Table(name = "facturas")
public class Factura implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idFactura;
	private Long noFactura;
	private String serie;
	private Double total;
	private Double iva;
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
	private TipoFactura tipoFactura;

	public Factura() {
		itemsFactura = new ArrayList<>();
	}

	@PrePersist
	public void initFecha() {
		this.fecha = new Date();
	}

	public Long getIdFactura() {
		return idFactura;
	}

	public void setIdFactura(Long idFactura) {
		this.idFactura = idFactura;
	}

	public Long getNoFactura() {
		return noFactura;
	}

	public void setNoFactura(Long noFactura) {
		this.noFactura = noFactura;
	}

	public String getSerie() {
		return serie;
	}

	public void setSerie(String serie) {
		this.serie = serie;
	}

	public Double getTotal() {
		return total;
	}

	public void setTotal(Double total) {
		this.total = total;
	}

	public Double getIva() {
		return iva;
	}

	public void setIva(Double iva) {
		this.iva = iva;
	}

	public String getCorrelativoSat() {
		return correlativoSat;
	}

	public void setCorrelativoSat(String correlativoSat) {
		this.correlativoSat = correlativoSat;
	}

	public String getCertificacionSat() {
		return certificacionSat;
	}

	public void setCertificacionSat(String certificacionSat) {
		this.certificacionSat = certificacionSat;
	}

	public String getSerieSat() {
		return serieSat;
	}

	public void setSerieSat(String serieSat) {
		this.serieSat = serieSat;
	}

	public String getMensajeSat() {
		return mensajeSat;
	}

	public void setMensajeSat(String mensajeSat) {
		this.mensajeSat = mensajeSat;
	}

	public String getFechaCertificacionSat() {
		return fechaCertificacionSat;
	}

	public void setFechaCertificacionSat(String fechaCertificacionSat) {
		this.fechaCertificacionSat = fechaCertificacionSat;
	}

	public Date getFecha() {
		return fecha;
	}

	public void setFecha(Date fecha) {
		this.fecha = fecha;
	}

	public Estado getEstado() {
		return estado;
	}

	public void setEstado(Estado estado) {
		this.estado = estado;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public Cliente getCliente() {
		return cliente;
	}

	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}

	public List<DetalleFactura> getItemsFactura() {
		return itemsFactura;
	}

	public void setItemsFactura(List<DetalleFactura> itemsFactura) {
		this.itemsFactura = itemsFactura;
	}

	public TipoFactura getTipoFactura() {
		return tipoFactura;
	}

	public void setTipoFactura(TipoFactura tipoFactura) {
		this.tipoFactura = tipoFactura;
	}

	public Double calcularTotal() {
		Double total = 0.00;

		for (DetalleFactura item : itemsFactura) {
			total += item.getSubTotal();
		}

		return total;
	}

	public Double calcularIva(){
		float valorIva = (float) 0.12;
		return this.total * valorIva;
	}

	public Double calcularDescuento(Double nprecio){

		BigDecimal bd = new BigDecimal(nprecio); // Creamos una variable BigDecimal para almacenar el precioVenta.
		bd = bd.setScale(2, RoundingMode.HALF_UP); // Decidimos el formato de redondeo y la cantidad de decimales que deseamos.

		String precio = String.format("%.2f",bd.doubleValue()); // Devolvemos el valor resultante como un String.
		String[] partes = precio.split(Pattern.quote(".")); // Separa el valor del precio en componentes usando el . como separador
		String entero = partes[0]; // Variable que almacena el valor entero.
		String decimal = partes[1]; // Variable que almacena el valor decimal.
		int valor1 = Integer.parseInt(String.valueOf(decimal.charAt(0))); // guarda el primer digito despues del punto decimal.
		int valor2 = Integer.parseInt(String.valueOf(decimal.charAt(1))); // guarda el segundo digito despues del punto decimal.
		int valor3 = Integer.parseInt(entero); // guarda la parte entera del valor ingresado antes del punto decimal.
		String nvalor = "";

		if(valor2 >= 1 && valor2 <= 4){ // Si el valor del segundo dígito del decimal se encuentra entre 1 y 4
			valor2 = 5; // Le damos el valor de 5 al segundo decimal cuando la desicion devuelve verdadero
			nvalor = String.valueOf(valor1) + String.valueOf(valor2); // Concatena el primer y segundo decimal
			precio = entero + "." + nvalor; // Concatena el valor entero
		}else if(valor2 >= 6 && valor2 <= 9){
			valor2 = 0;
			if(valor1 == 9){
				valor1 = 0;
				valor3 += 1;
			}else{
				valor1 += 1;
			}
			nvalor = String.valueOf(valor1) + String.valueOf(valor2);
			precio = String.valueOf(valor3) + "." + nvalor;
		}
		return Double.parseDouble(precio);
	}

	public Double redondearPrecio(double precio){
		BigDecimal bd = new BigDecimal(precio);
		bd = bd.setScale(2,RoundingMode.HALF_UP);

		return bd.doubleValue();
	}

	@Override
	public String toString() {
		return "Factura{" +
				"idFactura=" + idFactura +
				", noFactura=" + noFactura +
				", serie='" + serie + '\'' +
				", total=" + total +
				", fecha=" + fecha +
				", estado=" + estado +
				", usuario=" + usuario +
				", cliente=" + cliente +
				", itemsFactura=" + itemsFactura +
				", tipoFactura=" + tipoFactura +
				'}';
	}

	private static final long serialVersionUID = 1L;

}
