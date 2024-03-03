package com.aglayatech.licorstore.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "emisores")
public class Emisor implements Serializable {

    private static final long serialVersionUID = -2672814903805575844L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idEmisor;
    private String codigoPostal;
    private String correoEmisor;
    private String departamento;
    private String municipio;
    private String direccion;
    private String nit;
    private String nombreComercial;
    private String nombreEmisor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado")
    @JsonIgnoreProperties(value = {"hibernateLazyIntializer", "handler"})
    private Estado estado;
}
