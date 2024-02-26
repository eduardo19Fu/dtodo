package com.aglayatech.licorstore.service.impl;

import com.aglayatech.licorstore.model.Emisor;
import com.aglayatech.licorstore.repository.IEmisorRepository;
import com.aglayatech.licorstore.service.IEmisorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmisorServiceImpl implements IEmisorService {

    @Autowired
    private IEmisorRepository emisorRepository;

    @Override
    public Emisor getEmisor(Integer idemisor) {
        return this.emisorRepository.findById(idemisor).orElse(null);
    }
}
