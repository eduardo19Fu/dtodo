package xyz.pangosoft.dtodo.model;

import xyz.pangosoft.dtodo.model.enums.EstadoNotaCreditoEnum;
import xyz.pangosoft.dtodo.model.enums.TipoDocumentoOrigenEnum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

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

    private String correlativoFacturaSat;
    private String serieFacturaSat;

    @Enumerated(EnumType.STRING)
    private TipoDocumentoOrigenEnum tipoDocumentoOrigen;

    private String noProforma;

    private BigDecimal total;
    private String observaciones;

    private LocalDateTime fechaCreacion;
    private LocalDate fechaEntregaEstimada;

    @Enumerated(EnumType.STRING)
    private EstadoNotaCreditoEnum estado;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "id_nota_credito")
    @JsonIgnoreProperties({ "notaCredito", "hibernateLazyInitializer", "handler" })
    private List<NotaCreditoDetalle> items;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente")
    @JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"})
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    @JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler", "password"})
    private Usuario usuario;
}
