package com.aglayatech.licorstore.service;

import com.aglayatech.licorstore.model.Estado;
import com.aglayatech.licorstore.model.NotaCredito;

import java.util.List;

public interface INotaCreditoService {

    public List<NotaCredito> findNotas();

    public List<NotaCredito> findNotasActivas(Estado estado);

    public NotaCredito findNota(Integer idNota);

    public NotaCredito save(NotaCredito notaCredito);

    public void delete(Integer idNota);
}
