package xyz.pangosoft.dtodo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.Valid;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum;
import xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@Builder(toBuilder = true)
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
    @JsonIgnoreProperties({"password", "roles", "sucursal", "hibernateLazyInitializer", "handler" })
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Estado estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Cliente cliente;

    /** MINORISTA (la de siempre) o MAYORISTA; es independiente del tipo del cliente. */
    @Enumerated(EnumType.STRING)
    private TipoProformaEnum tipoProforma;

    /** Ruta del cliente al crear la proforma, copiada para que los reportes históricos no cambien. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ruta")
    @JsonIgnoreProperties({ "vendedor", "sucursal", "estado", "hibernateLazyInitializer", "handler" })
    private Ruta ruta;

    /** Observación general, por ejemplo cómo va empacado el pedido; solo se imprime en la copia de la tienda. */
    private String observaciones;

    /** Vigencia de la proforma mayorista: 15 días desde la emisión. */
    private LocalDate fechaVencimiento;

    /** Fecha de entrega prometida, opcional. */
    private LocalDate fechaEntregaEstimada;

    /**
     * {@code null} si no necesita autorización. {@code PENDIENTE} (una línea sobre el tope de descuento) o
     * {@code RECHAZADA} impiden facturarla. Lo calcula el servidor; lo que traiga la petición se ignora.
     */
    @Enumerated(EnumType.STRING)
    private EstadoAutorizacionEnum estadoAutorizacion;

    /**
     * Etapa del pedido; {@code null} en una proforma minorista. La mayorista queda {@code CONFIRMADA} al crearse y
     * avanza con bodega. Lo fija el servidor; lo que traiga la petición se ignora.
     */
    @Enumerated(EnumType.STRING)
    private EstadoPedidoEnum estadoPedido;

    /** Un administrador autorizó facturarla aunque ya venció. */
    private boolean vencimientoAutorizado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sucursal")
    @JsonIgnoreProperties({ "usuario", "hibernateLazyInitializer", "handler" })
    private Sucursal sucursal;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "id_proforma")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    @Valid
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
