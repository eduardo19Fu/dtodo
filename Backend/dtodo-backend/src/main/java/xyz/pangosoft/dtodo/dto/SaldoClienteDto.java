package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * El estado de cuenta de un cliente mayorista: lo que se le ha entregado, lo que ha pagado con recibos vigentes y la
 * diferencia. Un saldo negativo es dinero a favor del cliente (por ejemplo, un pago adelantado de un pedido de contado).
 */
@Data
@NoArgsConstructor
public class SaldoClienteDto {
    private Integer idCliente;
    private String cliente;
    private BigDecimal limiteCredito;
    private Integer diasCredito;
    /** Valor de todo lo entregado en pedidos vigentes. */
    private BigDecimal entregado = BigDecimal.ZERO;
    /** Valor de todo lo que el cliente devolvió de sus pedidos. */
    private BigDecimal devuelto = BigDecimal.ZERO;
    /** Suma de los recibos vigentes (ligados o no a un pedido). */
    private BigDecimal abonado = BigDecimal.ZERO;
    /** Entregado menos devuelto menos abonado. */
    private BigDecimal saldo = BigDecimal.ZERO;
    /** Lo que queda del límite de crédito (límite menos el saldo pendiente); nulo si el cliente no tiene límite. */
    private BigDecimal disponible;
    /** Pedidos vigentes que aún no se entregan por completo o que tienen algo pendiente de pago. */
    private List<PedidoSaldoDto> pedidos = new ArrayList<>();
}
