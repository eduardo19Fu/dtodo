package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Una devolución de lo entregado al cliente de un pedido mayorista. La registra el administrador y queda aprobada al
 * registrarla. {@code idProforma} no es una relación a propósito, igual que en las entregas: el historial de una proforma
 * anulada (borrada) se conserva.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = { "cliente", "sucursal", "usuario", "usuarioAnulacion", "detalles" })
@Entity
@Table(name = "devoluciones_cliente")
public class DevolucionCliente implements Serializable {

    private static final long serialVersionUID = 7012284451093386112L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDevolucion;

    private Long idProforma;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    /** Sucursal que recibe el producto que reingresa; nula si todo se da de baja. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sucursal")
    private Sucursal sucursal;

    /** La nota de crédito a la que se liga cuando el pedido tiene una factura vigente. */
    private Long idNotaCredito;

    private LocalDate fecha;
    private String motivo;

    /** Valor de lo devuelto, a precio de línea con descuento. Baja lo que debe el cliente. */
    private BigDecimal total;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    private LocalDateTime fechaRegistro;

    /** Una devolución anulada se conserva y deja de contar para el saldo, lo devolvible y las comisiones. */
    private boolean anulada;

    private LocalDateTime fechaAnulacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_anulacion")
    private Usuario usuarioAnulacion;

    private String motivoAnulacion;

    @Builder.Default
    @OneToMany(mappedBy = "devolucion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DevolucionClienteDetalle> detalles = new ArrayList<>();
}
