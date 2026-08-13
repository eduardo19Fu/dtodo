package xyz.pangosoft.dtodo.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import xyz.pangosoft.dtodo.model.Certificador;
import xyz.pangosoft.dtodo.repository.ICertificadorRepository;
import xyz.pangosoft.dtodo.service.ICertificadorService;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificadorServiceImpl implements ICertificadorService {

    private final ICertificadorRepository certificadorRepository;

    @Override
    @Transactional(readOnly = true)
    public Certificador getCertificador(Integer idcertificador) {
        log.info("Buscando certificador con ID: {}", idcertificador);
        return certificadorRepository.findById(idcertificador).orElse(null);
    }
}
