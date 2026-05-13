package xyz.pangosoft.dtodo.service;

import xyz.pangosoft.dtodo.dto.DespachoNotaDto;
import xyz.pangosoft.dtodo.model.DespachoNota;

import java.util.List;

public interface IDespachoNotaService {

    List<DespachoNotaDto> findByNotaCredito(Long idNotaCredito);

    List<DespachoNotaDto> findByEvento(String idEvento);

    List<DespachoNotaDto> registrarDespacho(Long idNotaCredito, List<DespachoNota> despachos);

    int totalDespachado(Long idNotaCredito, Integer idProducto);

    byte[] generateComprobanteDespacho(String idEvento);
}
