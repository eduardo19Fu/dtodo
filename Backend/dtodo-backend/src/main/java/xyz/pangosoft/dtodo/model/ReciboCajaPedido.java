package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.persistence.Entity;
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

/** Cuánto de un recibo de caja se aplica a un pedido, cuando un solo recibo paga varios pedidos. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = { "recibo", "proforma" })
@Entity
@Table(name = "recibos_caja_pedidos")
public class ReciboCajaPedido implements Serializable {

    private static final long serialVersionUID = 2918400011622905312L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPartida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_recibo")
    private ReciboCaja recibo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proforma")
    private Proforma proforma;

    private BigDecimal monto;
}
