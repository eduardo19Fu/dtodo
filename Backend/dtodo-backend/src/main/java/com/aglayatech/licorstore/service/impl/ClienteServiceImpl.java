package com.aglayatech.licorstore.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import com.aglayatech.licorstore.model.Cliente;
import com.aglayatech.licorstore.repository.IClienteRepository;
import com.aglayatech.licorstore.service.IClienteService;

@Service
public class ClienteServiceImpl implements IClienteService {

	@Autowired
	private IClienteRepository repoCliente;

	@Override
	public List<Cliente> findAll() {
		return repoCliente.findAll(Sort.by(Direction.ASC, "idCliente"));
	}

	@Override
	public Page<Cliente> findAll(Pageable pageable) {
		return repoCliente.findAll(pageable);
	}

	@Override
	public Cliente findById(Integer idcliente) {
		return repoCliente.findById(idcliente).orElse(null);
	}

	@Override
	public List<Cliente> findByName(String nombre) {
		return repoCliente.findByNombre(nombre);
	}

	@Override
	public Cliente findByNit(String nit) {
		return repoCliente.findByNit(nit).orElse(null);
	}

	@Override
	public Integer totalClientes() {
		return this.repoCliente.getCantClientes() == null ? 0 : this.repoCliente.getCantClientes();
	}

	@Override
	public Cliente save(Cliente cliente) {
		return repoCliente.save(cliente);
	}

	@Override
	public void delete(Integer idcliente) {
		repoCliente.deleteById(idcliente);
	}

}
