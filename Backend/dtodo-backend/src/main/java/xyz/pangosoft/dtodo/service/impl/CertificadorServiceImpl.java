package xyz.pangosoft.dtodo.service.impl;

import xyz.pangosoft.dtodo.model.Certificador;
import xyz.pangosoft.dtodo.repository.ICertificadorRepository;
import xyz.pangosoft.dtodo.service.ICertificadorService;
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
