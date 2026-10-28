package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.pangosoft.dtodo.model.enums.CondicionPagoEnum;

/** Cómo va el cobro de un pedido mayorista. */
@Data
@NoArgsConstructor
public class PedidoSaldoDto {
    private Long idProforma;
    private String noProforma;
    private CondicionPagoEnum condicionPago;
    private Integer diasCredito;
    /** Total del pedido. */
    private BigDecimal total = BigDecimal.ZERO;
    /** Valor de lo entregado hasta hoy (entregas vigentes, a precio de línea con descuento). */
    private BigDecimal entregado = BigDecimal.ZERO;
    /** Lo cobrado con recibos vigentes ligados al pedido (directamente o por una factura suya). */
    private BigDecimal cobrado = BigDecimal.ZERO;
    /** Valor de lo que el cliente devolvió de este pedido; baja lo que debe. */
    private BigDecimal devuelto = BigDecimal.ZERO;
    /** Lo que se aplicó de los abonos sin pedido del cliente, del más antiguo al más reciente. */
    private BigDecimal aplicadoDeAbonos = BigDecimal.ZERO;
    /** Lo entregado que todavía no se paga. */
    private BigDecimal pendiente = BigDecimal.ZERO;
    /** Fecha de la primera entrega vigente; desde ahí corren los días de crédito. */
    private LocalDate primeraEntrega;
    /** Vence cuando se cumplen los días de crédito desde la primera entrega; solo en pedidos a crédito entregados. */
    private LocalDate vencimiento;
}
