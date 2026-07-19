package xyz.pangosoft.dtodo.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import xyz.pangosoft.dtodo.model.TipoFactura;
import xyz.pangosoft.dtodo.repository.ITipoFacturaRepository;
import xyz.pangosoft.dtodo.service.ITipoFacturaService;

@Service
@RequiredArgsConstructor
@Slf4j
public class TipoFacturaServiceImpl implements ITipoFacturaService {

    private final ITipoFacturaRepository tipoFacturaRepository;

    @Override
    @Transactional(readOnly = true)
    public TipoFactura getTipoFactura(Integer id) {
        log.info("Buscando tipo de factura con ID: {}", id);
        return tipoFacturaRepository.findById(id).orElse(null);
    }
}
