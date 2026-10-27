package xyz.pangosoft.dtodo.service;

import java.math.BigDecimal;

import xyz.pangosoft.dtodo.dto.PedidoSaldoDto;
import xyz.pangosoft.dtodo.dto.SaldoClienteDto;

/**
 * Saldo de los clientes mayoristas. La deuda nace al entregar: el saldo es el valor de lo entregado menos los recibos de caja
 * vigentes. Un recibo ligado a un pedido (o a una factura suya) paga ese pedido; los abonos sin pedido se aplican a lo más
 * antiguo que quede pendiente. Las reglas responden 422 (401 y 403 los reserva el interceptor del frontend).
 */
public interface ISaldoClienteService {

    /** El estado de cuenta de un cliente; un vendedor solo consulta a los de sus rutas (404 si es de otra). */
    SaldoClienteDto saldo(Integer idCliente);

    /** Cómo va el cobro de un pedido, sin repartir los abonos sin pedido. */
    PedidoSaldoDto saldoDelPedido(Long idProforma);

    /** Valor de lo entregado en un pedido con las entregas vigentes. */
    BigDecimal valorEntregado(Long idProforma);

    /** Lo cobrado, con recibos vigentes, directamente a un pedido (ligados a él o a una factura suya). */
    BigDecimal cobrado(Long idProforma);
}
