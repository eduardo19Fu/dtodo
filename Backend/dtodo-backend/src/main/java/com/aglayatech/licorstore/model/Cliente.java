package com.aglayatech.licorstore.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

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

	@PrePersist
	public void configFechaRegistro() {
		fechaRegistro = LocalDateTime.now();
		setNombre(nombre.toUpperCase());
		setNit(nit.toUpperCase());
		setDireccion(direccion.toUpperCase());
	}

}
