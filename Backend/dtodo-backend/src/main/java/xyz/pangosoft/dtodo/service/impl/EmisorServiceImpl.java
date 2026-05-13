package xyz.pangosoft.dtodo.service.impl;

import xyz.pangosoft.dtodo.model.Emisor;
import xyz.pangosoft.dtodo.repository.IEmisorRepository;
import xyz.pangosoft.dtodo.service.IEmisorService;
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
