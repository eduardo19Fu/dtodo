package xyz.pangosoft.dtodo.service;

import xyz.pangosoft.dtodo.dto.EntregaSolicitudDto;
import xyz.pangosoft.dtodo.dto.PedidoDetalleDto;

/**
 * Entrega de un pedido mayorista al cliente, y su descuento de inventario. La entrega descuenta de la sucursal que
 * elige quien la registra solo lo que todavía no estaba descontado por este pedido; si ya se había descontado a mano,
 * se marca y no descuenta. Las reglas responden 422 (401 y 403 los reserva el interceptor del frontend).
 */
public interface IEntregaPedidoService {

    /**
     * Registra una entrega. El pedido debe estar {@code EN_RUTA} o con entrega parcial, con el descuento autorizado.
     * Se entrega como máximo lo pendiente de cada línea. Si la sucursal no tiene existencias de lo que hay que
     * descontar no se registra nada. El pedido queda {@code ENTREGADA} o {@code ENTREGADA_PARCIAL}.
     */
    PedidoDetalleDto registrar(Long idProforma, EntregaSolicitudDto solicitud);

    /**
     * Anula una entrega y reingresa al inventario lo que descontó. Solo si el pedido no tiene una factura vigente;
     * el motivo es obligatorio. El pedido vuelve a {@code EN_RUTA}, o queda parcial si tiene otras entregas.
     */
    PedidoDetalleDto anular(Long idProforma, Long idEntrega, String motivo);

    /** Da por entregado un pedido con entrega parcial aunque falte parte: lo pendiente ya no se llevará. */
    PedidoDetalleDto cerrarConLoEntregado(Long idProforma, String motivo);
}
