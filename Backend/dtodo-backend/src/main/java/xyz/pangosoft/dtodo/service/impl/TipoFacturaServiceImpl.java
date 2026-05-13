package xyz.pangosoft.dtodo.service.impl;

import xyz.pangosoft.dtodo.model.TipoFactura;
import xyz.pangosoft.dtodo.repository.ITipoFacturaRepository;
import xyz.pangosoft.dtodo.service.ITipoFacturaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TipoFacturaServiceImpl implements ITipoFacturaService {

    @Autowired
    private ITipoFacturaRepository tipoFacturaRepository;

    @Override
    public TipoFactura getTipoFactura(Integer id) {
        return this.tipoFacturaRepository.findById(id).orElse(null);
    }
}
