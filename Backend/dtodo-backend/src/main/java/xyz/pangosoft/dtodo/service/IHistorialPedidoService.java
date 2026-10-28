package xyz.pangosoft.dtodo.service;

import java.util.List;

import xyz.pangosoft.dtodo.dto.HistorialEventoDto;

/**
 * Línea de tiempo de un pedido mayorista: quién lo creó, editó, autorizó, preparó, empacó, cargó, entregó, devolvió y facturó,
 * y cuándo, de lo más antiguo a lo más reciente. Un vendedor solo consulta los pedidos suyos (404 si es de otro).
 */
public interface IHistorialPedidoService {

    List<HistorialEventoDto> historial(Long idProforma);
}
