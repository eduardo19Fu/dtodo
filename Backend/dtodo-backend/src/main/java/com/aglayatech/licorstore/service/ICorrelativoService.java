package com.aglayatech.licorstore.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.aglayatech.licorstore.model.Correlativo;
import com.aglayatech.licorstore.model.Estado;
import com.aglayatech.licorstore.model.Usuario;

public interface ICorrelativoService {
	
	public List<Correlativo> findAll();
	
	public Page<Correlativo> findAll(Pageable pageable);
	
	public Correlativo findById(Long idcorrelativo);
	
	public Correlativo findByUsuario(Usuario usuario, Estado estado);
	
	public Correlativo save(Correlativo correlativo);
	
	public void delete(Long id);

}
