package xyz.pangosoft.dtodo.repository;

import java.util.List;
import java.util.Optional;

import xyz.pangosoft.dtodo.dto.ClienteDto;
import org.springframework.data.jpa.repository.JpaRepository;

import xyz.pangosoft.dtodo.model.Cliente;
import org.springframework.data.jpa.repository.Query;

public interface IClienteRepository extends JpaRepository<Cliente, Integer> {
	
	// Busqueda de cliente por nombre
	// Consulta = 'Select * from Cliente where nombre = /*parametro dado*/
	List<Cliente> findByNombre(String nombre);
	
	// Búsqueda de cliente por nit
	// Consulta = 'Select * from Cliente where nit = /*parametro dado*/
	Optional<Cliente> findByNit(String nit);

	@Query(value = "Select get_cant_clientes()", nativeQuery = true)
	Integer getCantClientes();

	@Query(value = "{call sp_consultar_clientes()}", nativeQuery = true)
	List<ClienteDto> consultarClientesDto();
}
