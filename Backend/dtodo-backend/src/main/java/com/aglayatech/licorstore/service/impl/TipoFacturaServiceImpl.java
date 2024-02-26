package com.aglayatech.licorstore.service.impl;

import com.aglayatech.licorstore.model.TipoFactura;
import com.aglayatech.licorstore.repository.ITipoFacturaRepository;
import com.aglayatech.licorstore.service.ITipoFacturaService;
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
