package xyz.pangosoft.dtodo.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.dto.CrearDevolucionClienteDto;
import xyz.pangosoft.dtodo.dto.DevolucionClienteDto;
import xyz.pangosoft.dtodo.dto.PedidoDevolubleDto;

/**
 * Devoluciones de clientes sobre lo ya entregado de pedidos mayoristas. Solo el administrador las registra, y quedan aprobadas
 * al registrarlas. El valor devuelto baja lo que debe el cliente; cada línea reingresa al inventario de una sucursal o se da de
 * baja. Si el pedido tiene una factura vigente, la devolución se liga a una nota de crédito. Las reglas responden 422 (401 y
 * 403 los reserva el interceptor del frontend).
 */
public interface IDevolucionClienteService {

    /** Registra la devolución: valida las cantidades, mueve el inventario de lo que reingresa y deja constancia en el historial. */
    DevolucionClienteDto registrar(CrearDevolucionClienteDto solicitud);

    /**
     * Anula una devolución con motivo obligatorio: vuelve a subir lo que debe el cliente y lo devolvible de cada línea, y lo que
     * había reingresado al inventario sale de nuevo (si ya no hay existencias, se rechaza). No se anula si está ligada a una nota
     * de crédito que sigue vigente, ni si el año de la devolución ya tiene las comisiones liquidadas.
     */
    DevolucionClienteDto anular(Long idDevolucion, String motivo);

    DevolucionClienteDto buscar(Long idDevolucion);

    /** Lista por fecha (sin fechas, los últimos 90 días), de la más reciente a la más antigua. */
    Page<DevolucionClienteDto> listar(Integer idCliente, LocalDate desde, LocalDate hasta, Pageable pageable);

    /** Los pedidos de un cliente con algo entregado, con lo que se puede devolver de cada línea. */
    List<PedidoDevolubleDto> pedidosDevolubles(Integer idCliente);
}
