package com.aglayatech.licorstore.repository;

import com.aglayatech.licorstore.model.Estado;
import com.aglayatech.licorstore.model.NotaCredito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface INotaCreditoRepository extends JpaRepository<NotaCredito, Integer> {
    public List<NotaCredito> findNotasByEstado(Estado estado);
}
