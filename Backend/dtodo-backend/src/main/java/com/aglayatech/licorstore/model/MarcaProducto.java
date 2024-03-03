package com.aglayatech.licorstore.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.validation.constraints.NotEmpty;

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
@Table(name = "marcas_producto")
public class MarcaProducto implements Serializable {

	private static final long serialVersionUID = 6163462751993275676L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer idMarcaProducto;

	@NotEmpty
	private String marca;
	private LocalDateTime fechaRegistro;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario")
	@JsonIgnoreProperties({ "password", "roles", "fecha_registro","hibernateLazyInitializer", "handler" })
	private Usuario usuario;

	@PrePersist
	public void configFechaRegistro() {
		this.fechaRegistro = LocalDateTime.now();
	}

}
