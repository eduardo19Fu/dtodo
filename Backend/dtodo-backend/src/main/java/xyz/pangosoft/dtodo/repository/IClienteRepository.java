package xyz.pangosoft.dtodo.repository;

import java.util.List;
import java.util.Optional;

import xyz.pangosoft.dtodo.dto.ClienteDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import xyz.pangosoft.dtodo.model.Cliente;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IClienteRepository extends JpaRepository<Cliente, Integer>, JpaSpecificationExecutor<Cliente> {
	
	// Busqueda de cliente por nombre
	// Consulta = 'Select * from Cliente where nombre = /*parametro dado*/
	List<Cliente> findByNombre(String nombre);
	
	// Búsqueda de cliente por nit
	// Consulta = 'Select * from Cliente where nit = /*parametro dado*/
	Optional<Cliente> findByNit(String nit);

	@Query(value = "Select get_cant_clientes()", nativeQuery = true)
	Integer getCantClientes();

	@Query("select new xyz.pangosoft.dtodo.dto.ClienteDto(" +
			"c.idCliente, c.nombre, c.nit, c.direccion, c.fechaRegistro, c.telefono) " +
			"from Cliente c")
	List<ClienteDto> consultarClientesDto();

	@Query(value = "SELECT new xyz.pangosoft.dtodo.dto.ClienteDto(" +
			"c.idCliente, c.nombre, c.nit, c.direccion, c.fechaRegistro, c.telefono) " +
			"FROM Cliente c WHERE (:filtro = '' " +
			"OR LOWER(COALESCE(c.nombre, '')) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(COALESCE(c.nit, '')) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(COALESCE(c.telefono, '')) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(COALESCE(c.direccion, '')) LIKE LOWER(CONCAT('%', :filtro, '%')))",
			countQuery = "SELECT COUNT(c) FROM Cliente c WHERE (:filtro = '' " +
					"OR LOWER(COALESCE(c.nombre, '')) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(COALESCE(c.nit, '')) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(COALESCE(c.telefono, '')) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(COALESCE(c.direccion, '')) LIKE LOWER(CONCAT('%', :filtro, '%')))")
	Page<ClienteDto> findListado(@Param("filtro") String filtro, Pageable pageable);
}
