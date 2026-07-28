package xyz.pangosoft.dtodo.service;

import xyz.pangosoft.dtodo.dto.DespachoNotaDto;
import xyz.pangosoft.dtodo.dto.DespachoRequest;

import java.util.List;

public interface IDespachoNotaService {

    List<DespachoNotaDto> findByNotaCredito(Long idNotaCredito);

    List<DespachoNotaDto> findByEvento(String idEvento);

    /**
     * Registra un despacho de productos de una Nota de Crédito previa
     * autorización del usuario que lo ejecuta.
     *
     * @param idNotaCredito nota sobre la que se despacha
     * @param request       líneas a despachar y contraseña de autorización
     * @param username      usuario autenticado, tomado del token (no del payload)
     * @return las líneas registradas en esta operación
     * @throws xyz.pangosoft.dtodo.error.exceptions.InvalidPasswordException si
     *         la contraseña de autorización no coincide
     */
    List<DespachoNotaDto> registrarDespacho(Long idNotaCredito, DespachoRequest request, String username);

    int totalDespachado(Long idNotaCredito, Integer idProducto);

    byte[] generateComprobanteDespacho(String idEvento);
}
