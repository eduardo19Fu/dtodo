package com.aglayatech.licorstore.service;

import com.aglayatech.licorstore.model.Estado;
import com.aglayatech.licorstore.model.NotaCredito;
import com.aglayatech.licorstore.repository.INotaCreditoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotaCreditoServiceImpl implements INotaCreditoService {

    @Autowired
    private INotaCreditoRepository notaCreditoRepository;

    @Override
    public List<NotaCredito> findNotas() {
        return notaCreditoRepository.findAll();
    }

    @Override
    public List<NotaCredito> findNotasActivas(Estado estado) {
        return notaCreditoRepository.findNotasByEstado(estado);
    }

    @Override
    public NotaCredito findNota(Integer idNota) {
        return notaCreditoRepository.findById(idNota).orElse(null);
    }

    @Override
    public NotaCredito save(NotaCredito notaCredito) {
        return notaCreditoRepository.save(notaCredito);
    }

    @Override
    public void delete(Integer idNota) {
        notaCreditoRepository.deleteById(idNota);
    }
}
