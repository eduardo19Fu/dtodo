package com.aglayatech.licorstore.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "estados")
public class Estado implements Serializable {

	private static final long serialVersionUID = 1896201450693351566L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer idEstado;
	private String estado;

}
