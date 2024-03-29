package com.aglayatech.licorstore.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.exolab.castor.types.DateTime;

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

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "proformas")
public class Proforma implements Serializable {

    private static final long serialVersionUID = -7336700857512359689L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idProforma;
    private String noProforma;
    private BigDecimal total;
//    private LocalDateTime fechaEmision;

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

    public Proforma() {
        itemsProforma = new ArrayList<>();
    }

    @PrePersist
    public void prepersist(){
        //this.fechaEmision = LocalDateTime.now();
        this.fechaEmision = new Date();
    }
}
