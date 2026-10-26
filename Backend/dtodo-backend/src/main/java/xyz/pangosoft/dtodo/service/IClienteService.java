package xyz.pangosoft.dtodo.service;

import java.util.List;

import xyz.pangosoft.dtodo.dto.ClienteDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.dto.ClienteFiltro;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.enums.TipoClienteEnum;

public interface IClienteService {
	
	public List<Cliente> findAll();

	public List<ClienteDto> findAllDto();

	Page<ClienteDto> findListado(String filtro, Pageable pageable);

	/** Listado con filtros opcionales por tipo de cliente y por ruta (cualquiera puede ser {@code null}). */
	Page<ClienteDto> findListado(String filtro, TipoClienteEnum tipoCliente, Integer idRuta, Pageable pageable);

	/** Listado con todos los criterios disponibles. */
	Page<ClienteDto> findListado(ClienteFiltro filtro, Pageable pageable);

	/** Cantidad de clientes con un NIT escrito que no es válido (por ejemplo, un teléfono). */
	long totalNitInvalidos();
	
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
