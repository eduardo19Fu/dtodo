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
@Table(name = "roles")
public class Role implements Serializable {

	private static final long serialVersionUID = 5266167066671126406L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer idRole;
	private String role;
}
