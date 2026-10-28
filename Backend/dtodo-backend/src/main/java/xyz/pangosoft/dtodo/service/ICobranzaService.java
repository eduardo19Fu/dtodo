package xyz.pangosoft.dtodo.service;

import java.util.List;

import xyz.pangosoft.dtodo.dto.CobranzaDto;
import xyz.pangosoft.dtodo.dto.MovimientoEstadoCuentaDto;

/**
 * Cobranza de los clientes mayoristas: cuánto debe cada uno y desde cuándo, y su estado de cuenta. Solo el administrador y el
 * cobrador. Las reglas responden 422 (401 y 403 los reserva el interceptor del frontend).
 */
public interface ICobranzaService {

    /**
     * Las cuentas por cobrar de hoy: los clientes con deuda, de mayor a menor vencido, con su deuda por antigüedad.
     *
     * @param idRuta        opcional: solo los clientes de esa ruta
     * @param soloVencidos  solo los clientes con algo vencido
     */
    CobranzaDto cuentasPorCobrar(Integer idRuta, boolean soloVencidos);

    /** Lo mismo con una fecha de corte distinta de hoy (para pruebas y reportes). */
    CobranzaDto cuentasPorCobrar(Integer idRuta, boolean soloVencidos, java.time.LocalDate hoy);

    /** Las entregas y los recibos vigentes de un cliente por fecha, con el saldo corrido. */
    List<MovimientoEstadoCuentaDto> estadoDeCuenta(Integer idCliente);
}
