package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.math.BigDecimal;

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
import xyz.pangosoft.dtodo.model.enums.DestinoDevolucionEnum;

/** Lo devuelto de una línea del pedido: unidades, destino del producto y valor. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = { "devolucion" })
@Entity
@Table(name = "devoluciones_cliente_detalle")
public class DevolucionClienteDetalle implements Serializable {

    private static final long serialVersionUID = -4130871142998561207L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDevolucionDetalle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_devolucion")
    private DevolucionCliente devolucion;

    /** La línea de la proforma ({@code proformas_detalle}). */
    private Long idDetalle;

    private Integer cantidad;

    @Enumerated(EnumType.STRING)
    private DestinoDevolucionEnum destino;

    /** Unidades devueltas x importe de la línea con descuento / unidades de la línea. */
    private BigDecimal valor;
}
