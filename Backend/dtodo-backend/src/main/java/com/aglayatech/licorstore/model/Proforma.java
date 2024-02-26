package com.aglayatech.licorstore.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "proformas")
public class Proforma implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idProforma;
    private String noProforma;
    private BigDecimal total;

    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaEmision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    @JsonIgnoreProperties({"password", "roles", "hibernateLazyInitializer", "handler" })
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Estado estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Cliente cliente;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "id_proforma")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private List<DetalleProforma> itemsProforma;

    @PrePersist
    public void prepersist(){
        this.fechaEmision = new Date();
    }

    public Long getIdProforma() {
        return idProforma;
    }

    public void setIdProforma(Long idProforma) {
        this.idProforma = idProforma;
    }

    public String getNoProforma() {
        return noProforma;
    }

    public void setNoProforma(String noProforma) {
        this.noProforma = noProforma;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public Date getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(Date fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public List<DetalleProforma> getItemsProforma() {
        return itemsProforma;
    }

    public void setItemsProforma(List<DetalleProforma> itemsProforma) {
        this.itemsProforma = itemsProforma;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Proforma{");
        sb.append("idProforma=").append(idProforma);
        sb.append(", noProforma='").append(noProforma).append('\'');
        sb.append(", total=").append(total);
        sb.append(", fechaEmision=").append(fechaEmision);
        sb.append(", usuario=").append(usuario);
        sb.append(", estado=").append(estado);
        sb.append(", cliente=").append(cliente);
        sb.append(", itemsProforma=").append(itemsProforma);
        sb.append('}');
        return sb.toString();
    }

    private static final long serialVersionUID = 1L;
}
