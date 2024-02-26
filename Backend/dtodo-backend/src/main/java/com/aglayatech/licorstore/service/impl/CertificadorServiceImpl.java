package com.aglayatech.licorstore.service.impl;

import com.aglayatech.licorstore.model.Certificador;
import com.aglayatech.licorstore.repository.ICertificadorRepository;
import com.aglayatech.licorstore.service.ICertificadorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CertificadorServiceImpl implements ICertificadorService {

    @Autowired
    private ICertificadorRepository certificadorRepository;

    @Override
    public Certificador getCertificador(Integer idcertificador) {
        return this.certificadorRepository.findById(idcertificador).orElse(null);
    }
}
