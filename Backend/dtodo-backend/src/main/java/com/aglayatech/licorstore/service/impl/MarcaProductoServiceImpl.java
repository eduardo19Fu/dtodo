package com.aglayatech.licorstore.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import com.aglayatech.licorstore.model.MarcaProducto;
import com.aglayatech.licorstore.repository.IMarcaProductoRepository;
import com.aglayatech.licorstore.service.IMarcaProductoService;

@Service
public class MarcaProductoServiceImpl implements IMarcaProductoService {
	
	@Autowired
	private IMarcaProductoRepository marcaProductoRepo;

	@Override
	public List<MarcaProducto> findAll() {
		return marcaProductoRepo.findAll(Sort.by(Direction.ASC, "marca"));
	}
	
	@Override
	public Page<MarcaProducto> findAll(Pageable pageable) {
		return marcaProductoRepo.findAll(pageable);
	}

	@Override
	public MarcaProducto findById(Integer idMarca) {
		return marcaProductoRepo.findById(idMarca).orElse(null);
	}

	@Override
	public List<MarcaProducto> findByMarca(String marca) {
		return marcaProductoRepo.findByMarca(marca);
	}

	@Override
	public MarcaProducto save(MarcaProducto marca) {
		return marcaProductoRepo.save(marca);
	}

	@Override
	public void deleteMarca(Integer idMarca) {
		marcaProductoRepo.deleteById(idMarca);
	}

}
