package com.aglayatech.licorstore.service;

import com.aglayatech.licorstore.dto.NotaCreditoListDto;
import com.aglayatech.licorstore.model.NotaCredito;
import com.aglayatech.licorstore.model.enums.EstadoNotaCreditoEnum;

import java.util.List;

public interface INotaCreditoService {

    public List<NotaCreditoListDto> findNotas();

    public List<NotaCreditoListDto> findNotasActivas(EstadoNotaCreditoEnum estado);

    public NotaCredito findNota(Long idNota);

    public NotaCredito save(NotaCredito notaCredito, EstadoNotaCreditoEnum estado);

    public void delete(Long idNota);

    public byte[] generateReport(Long idNota);
}
