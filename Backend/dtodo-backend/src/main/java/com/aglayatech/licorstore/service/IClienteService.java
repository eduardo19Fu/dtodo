package com.aglayatech.licorstore.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.aglayatech.licorstore.model.Cliente;

public interface IClienteService {
	
	public List<Cliente> findAll();
	
	public Page<Cliente> findAll(Pageable pageable);
	
	public Cliente findById(Integer idcliente);
	
	public List<Cliente> findByName(String nombre);
	
	public Cliente findByNit(String nit);

	public Integer totalClientes();
	
	public Cliente save(Cliente cliente);
	
	public void delete(Cliente cliente);
	
}
