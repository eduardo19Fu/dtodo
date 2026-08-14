package xyz.pangosoft.dtodo.service;

import java.util.List;

import xyz.pangosoft.dtodo.dto.ClienteDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.model.Cliente;

public interface IClienteService {
	
	public List<Cliente> findAll();

	public List<ClienteDto> findAllDto();

	Page<ClienteDto> findListado(String filtro, Pageable pageable);
	
	public Page<Cliente> findAll(Pageable pageable);
	
	public Cliente findById(Integer idcliente);
	
	public List<Cliente> findByName(String nombre);
	
	public Cliente findByNit(String nit);

	public Integer totalClientes();
	
	public Cliente save(Cliente cliente);
	
	public void delete(Cliente cliente);
	
}
