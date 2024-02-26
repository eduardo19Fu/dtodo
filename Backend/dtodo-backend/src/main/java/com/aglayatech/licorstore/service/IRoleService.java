package com.aglayatech.licorstore.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.aglayatech.licorstore.model.Role;

public interface IRoleService {
	
	public List<Role> findAll();
	
	public Page<Role> findAll(Pageable pageable);
	
	// Metodo que devuelve un listado de roles cuyo nombre sea igual al parametro otorgado
	public Role findByName(String role);
	
	public Role findById(Integer id);
	
	public Role save(Role role);

}
