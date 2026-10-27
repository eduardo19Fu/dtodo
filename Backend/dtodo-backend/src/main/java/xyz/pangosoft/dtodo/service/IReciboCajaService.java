package xyz.pangosoft.dtodo.service;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.dto.CrearReciboCajaDto;
import xyz.pangosoft.dtodo.dto.ReciboCajaDto;

/**
 * Recibos de caja: abonos y depósitos de los clientes. Cada sucursal numera los suyos. Los emiten el administrador y el
 * cobrador, siempre desde su sucursal (el administrador puede elegir otra); solo el administrador anula, con motivo. Las
 * reglas de negocio responden 422 (401 y 403 los reserva el interceptor del frontend).
 */
public interface IReciboCajaService {

    /**
     * Emite un recibo. Pide cliente, monto mayor que cero y forma de pago; el depósito y la transferencia piden referencia.
     * El pedido o la factura, si se indican, deben ser del cliente y no estar anulados.
     */
    ReciboCajaDto emitir(CrearReciboCajaDto solicitud);

    /** Anula un recibo con motivo obligatorio. Deja de contar para el saldo del cliente. */
    ReciboCajaDto anular(Long idRecibo, String motivo);

    /** Un recibo; el cobrador solo ve los de su sucursal (404 si es de otra). */
    ReciboCajaDto buscar(Long idRecibo);

    /**
     * Lista de recibos por fecha de pago (sin fechas, los de los últimos 30 días). El cobrador solo ve su sucursal; el
     * administrador, todas o la que indique.
     */
    Page<ReciboCajaDto> listar(Integer idSucursal, Integer idCliente, LocalDate desde, LocalDate hasta, boolean incluirAnulados,
            Pageable pageable);
}
