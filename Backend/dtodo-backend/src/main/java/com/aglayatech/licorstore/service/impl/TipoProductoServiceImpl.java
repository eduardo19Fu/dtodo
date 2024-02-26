package com.aglayatech.licorstore.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import com.aglayatech.licorstore.model.TipoProducto;
import com.aglayatech.licorstore.repository.ITipoProductoRepository;
import com.aglayatech.licorstore.service.ITipoProductoService;

@Service
public class TipoProductoServiceImpl implements ITipoProductoService {

	@Autowired
	private ITipoProductoRepository repoTipo;

	@Override
	public List<TipoProducto> findAll() {
		return repoTipo.findAll(Sort.by(Direction.ASC, "tipoProducto"));
	}

	@Override
	public Page<TipoProducto> findAll(Pageable pageable) {
		return repoTipo.findAll(pageable);
	}

	@Override
	public List<TipoProducto> findByTipo(String tipo) {
		return repoTipo.findByTipoProducto(tipo);
	}

	@Override
	public TipoProducto findById(Integer id) {
		return repoTipo.findById(id).orElse(null);
	}

	@Override
	public TipoProducto save(TipoProducto tipo) {
		return repoTipo.save(tipo);
	}

	@Override
	public void delete(Integer idtipo) {
		repoTipo.deleteById(idtipo);
	}

}
