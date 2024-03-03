package com.aglayatech.licorstore.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "notas_credito")
public class NotaCredito implements Serializable {

    private static final long serialVersionUID = 3881631841718824337L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idNotaCredito;

    private BigDecimal abono;
    private BigDecimal total;
    private BigDecimal restante;

    private LocalDateTime fechaCreacion;
    private LocalDate fechaPagoLimite;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "id_nota_credito")
    @JsonIgnoreProperties({ "notaCredito", "hibernateLazyInitializer", "handler" })
    private List<NotaCreditoDetalle> items;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado")
    @JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"})
    private Estado estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente")
    @JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"})
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    @JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler", "password"})
    private Usuario usuario;
}
