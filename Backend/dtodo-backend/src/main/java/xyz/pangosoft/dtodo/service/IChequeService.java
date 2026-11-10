package xyz.pangosoft.dtodo.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.dto.ChequeDto;

/**
 * Control de cheques: los que los clientes entregan como pago y su seguimiento (recibido, depositado, cobrado o rechazado).
 * Lo manejan el administrador y el cobrador; este último solo ve los de su sucursal. Las reglas responden 422.
 */
public interface IChequeService {

    /** Los cheques por fecha de pago del recibo (sin fechas, los últimos 30 días); sucursal, cliente y estado son opcionales. */
    Page<ChequeDto> listar(Integer idSucursal, Integer idCliente, String estado, LocalDate desde, LocalDate hasta, Pageable pageable);

    /** Los mismos cheques sin paginar, para los reportes. */
    List<ChequeDto> todos(Integer idSucursal, Integer idCliente, String estado, LocalDate desde, LocalDate hasta);

    /**
     * Cambia el estado: RECIBIDO pasa a DEPOSITADO o RECHAZADO y DEPOSITADO a COBRADO o RECHAZADO. Un postfechado no se
     * deposita antes de su fecha. Rechazar pide motivo y anula el recibo, así el saldo del cliente vuelve a subir.
     */
    ChequeDto cambiarEstado(Long idCheque, String estado, String motivo);
}
