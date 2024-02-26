package com.aglayatech.licorstore.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import com.aglayatech.licorstore.model.Correlativo;
import com.aglayatech.licorstore.model.Estado;
import com.aglayatech.licorstore.model.Usuario;
import com.aglayatech.licorstore.repository.ICorrelativoRepository;
import com.aglayatech.licorstore.service.ICorrelativoService;

@Service
public class CorrelativoServiceImpl implements ICorrelativoService {

	@Autowired
	private ICorrelativoRepository repoCorrelativo;
	
	@Override
	public List<Correlativo> findAll() {
		return repoCorrelativo.findAll(Sort.by(Direction.ASC, "idCorrelativo"));
	}

	@Override
	public Page<Correlativo> findAll(Pageable pageable) {
		return repoCorrelativo.findAll(pageable);
	}

	@Override
	public Correlativo findById(Long idcorrelativo) {
		return repoCorrelativo.findById(idcorrelativo).orElse(null);
	}

	@Override
	public Correlativo save(Correlativo correlativo) {
		return repoCorrelativo.save(correlativo);
	}

	@Override
	public void delete(Long id) {
		repoCorrelativo.deleteById(id);
	}

	@Override
	public Correlativo findByUsuario(Usuario usuario, Estado estado) {
		return repoCorrelativo.findByUsuarioAndEstado(usuario, estado).orElse(null);
	}

}
