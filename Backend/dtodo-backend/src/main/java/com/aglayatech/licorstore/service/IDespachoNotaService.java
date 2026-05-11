package com.aglayatech.licorstore.service;

import com.aglayatech.licorstore.dto.DespachoNotaDto;
import com.aglayatech.licorstore.model.DespachoNota;

import java.util.List;

public interface IDespachoNotaService {

    List<DespachoNotaDto> findByNotaCredito(Long idNotaCredito);

    List<DespachoNotaDto> findByEvento(String idEvento);

    List<DespachoNotaDto> registrarDespacho(Long idNotaCredito, List<DespachoNota> despachos);

    int totalDespachado(Long idNotaCredito, Integer idProducto);

    byte[] generateComprobanteDespacho(String idEvento);
}
