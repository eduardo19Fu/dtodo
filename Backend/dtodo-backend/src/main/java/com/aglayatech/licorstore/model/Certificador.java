package com.aglayatech.licorstore.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "certificadores")
public class Certificador implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCertificador;
    private String aliasWs;
    private String llaveWs;
    private String tokenSigner;

    private String prefijo;

    private String correoCopia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado")
    @JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"})
    private Estado estado;

    public Integer getIdCertificador() {
        return idCertificador;
    }

    public void setIdCertificador(Integer idCertificador) {
        this.idCertificador = idCertificador;
    }

    public String getAliasWs() {
        return aliasWs;
    }

    public void setAliasWs(String aliasWs) {
        this.aliasWs = aliasWs;
    }

    public String getLlaveWs() {
        return llaveWs;
    }

    public void setLlaveWs(String llaveWs) {
        this.llaveWs = llaveWs;
    }

    public String getTokenSigner() {
        return tokenSigner;
    }

    public void setTokenSigner(String tokenSigner) {
        this.tokenSigner = tokenSigner;
    }

    public String getPrefijo() {
        return prefijo;
    }

    public void setPrefijo(String prefijo) {
        this.prefijo = prefijo;
    }

    public String getCorreoCopia() {
        return correoCopia;
    }

    public void setCorreoCopia(String correoCopia) {
        this.correoCopia = correoCopia;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    private static final long serialVersionUID = 1L;
}
