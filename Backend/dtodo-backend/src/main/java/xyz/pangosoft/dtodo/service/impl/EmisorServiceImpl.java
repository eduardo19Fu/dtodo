package xyz.pangosoft.dtodo.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import xyz.pangosoft.dtodo.model.Emisor;
import xyz.pangosoft.dtodo.repository.IEmisorRepository;
import xyz.pangosoft.dtodo.service.IEmisorService;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmisorServiceImpl implements IEmisorService {

    private final IEmisorRepository emisorRepository;

    @Override
    @Transactional(readOnly = true)
    public Emisor getEmisor(Integer idemisor) {
        log.info("Buscando emisor con ID: {}", idemisor);
        return emisorRepository.findById(idemisor).orElse(null);
    }
}
