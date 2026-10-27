package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import xyz.pangosoft.dtodo.model.enums.CategoriaClienteEnum;
import xyz.pangosoft.dtodo.model.enums.TipoClienteEnum;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = { "ruta" })
@Entity
@Table(name = "clientes")
public class Cliente implements Serializable {

	private static final long serialVersionUID = 195366071153481819L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer idCliente;
	private String nombre;
	private String nit;
	private String direccion;
	private String telefono;
	private LocalDateTime fechaRegistro;

	/** Código del cliente: un UUID que asigna el servidor al crearlo y que no se edita. Único. */
	private String codigoCliente;
	private String nombreNegocio;

	@Enumerated(EnumType.STRING)
	@Builder.Default
	private TipoClienteEnum tipoCliente = TipoClienteEnum.MINORISTA;

	@Enumerated(EnumType.STRING)
	private CategoriaClienteEnum categoriaDescuento;

	/** Ruta del cliente mayorista; los minoristas no tienen. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_ruta")
	@JsonIgnoreProperties({ "vendedor", "sucursal", "estado", "hibernateLazyInitializer", "handler" })
	private Ruta ruta;

	private String contacto;
	private String telefonoContacto;
	private String referenciaUbicacion;
	private BigDecimal limiteCredito;
	private Integer diasCredito;
}
