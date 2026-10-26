package xyz.pangosoft.dtodo.service;

import java.util.List;

import xyz.pangosoft.dtodo.dto.ClienteDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.enums.TipoClienteEnum;

public interface IClienteService {
	
	public List<Cliente> findAll();

	public List<ClienteDto> findAllDto();

	Page<ClienteDto> findListado(String filtro, Pageable pageable);

	/** Listado con filtros opcionales por tipo de cliente y por ruta (cualquiera puede ser {@code null}). */
	Page<ClienteDto> findListado(String filtro, TipoClienteEnum tipoCliente, Integer idRuta, Pageable pageable);
	
	public Page<Cliente> findAll(Pageable pageable);
	
	public Cliente findById(Integer idcliente);
	
	public List<Cliente> findByName(String nombre);
	
	public Cliente findByNit(String nit);

	/** Busca por el código con el que el negocio identifica al cliente (por defecto su teléfono). */
	public Cliente findByCodigo(String codigoCliente);

	public Integer totalClientes();
	
	public Cliente save(Cliente cliente);
	
	public void delete(Cliente cliente);
	
}
