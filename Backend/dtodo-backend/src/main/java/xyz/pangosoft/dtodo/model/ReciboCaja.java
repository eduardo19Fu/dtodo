package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import xyz.pangosoft.dtodo.model.enums.FormaPagoReciboEnum;

/**
 * Un abono o depósito de un cliente. El número es propio de cada sucursal. Un recibo anulado se conserva, con quién, cuándo y
 * por qué, y deja de contar para el saldo del cliente.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = { "sucursal", "cliente", "proforma", "factura", "usuario", "usuarioAnulacion" })
@Entity
@Table(name = "recibos_caja")
public class ReciboCaja implements Serializable {

    private static final long serialVersionUID = 4417209885136630154L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRecibo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sucursal")
    private Sucursal sucursal;

    private Long numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    /** El pedido al que paga, si se indicó. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proforma")
    private Proforma proforma;

    /** La factura a la que paga, si se indicó. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_factura")
    private Factura factura;

    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    private FormaPagoReciboEnum formaPago;

    private String referencia;
    private String observaciones;
    private LocalDate fechaPago;
    private LocalDateTime fechaRegistro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    private boolean anulado;
    private LocalDateTime fechaAnulacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_anulacion")
    private Usuario usuarioAnulacion;

    private String motivoAnulacion;
}
