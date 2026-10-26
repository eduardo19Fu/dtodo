package xyz.pangosoft.dtodo.repository;

import java.util.List;
import java.util.Optional;

import xyz.pangosoft.dtodo.dto.ClienteDto;
import xyz.pangosoft.dtodo.dto.ReporteSelectorDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import xyz.pangosoft.dtodo.model.Cliente;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IClienteRepository extends JpaRepository<Cliente, Integer>, JpaSpecificationExecutor<Cliente> {

	@Query("select new xyz.pangosoft.dtodo.dto.ReporteSelectorDto(" +
			"c.idCliente, c.nombre, concat(concat('NIT ', coalesce(c.nit, 'sin registrar')), " +
			"concat(' · ', coalesce(c.telefono, '')))) from Cliente c order by c.nombre")
	List<ReporteSelectorDto> findOpcionesReporte();
	
	// Busqueda de cliente por nombre
	// Consulta = 'Select * from Cliente where nombre = /*parametro dado*/
	List<Cliente> findByNombre(String nombre);
	
	// Búsqueda de cliente por nit
	// Consulta = 'Select * from Cliente where nit = /*parametro dado*/
	Optional<Cliente> findByNit(String nit);

	@Query(value = "Select get_cant_clientes()", nativeQuery = true)
	Integer getCantClientes();

	@Query("select new xyz.pangosoft.dtodo.dto.ClienteDto(" +
			"c.idCliente, c.nombre, c.nit, c.direccion, c.fechaRegistro, c.telefono, c.codigoCliente, c.nombreNegocio, " +
			"c.tipoCliente, c.categoriaDescuento, r.idRuta, r.nombre, c.contacto, c.telefonoContacto, " +
			"c.referenciaUbicacion, c.limiteCredito, c.diasCredito) " +
			"from Cliente c left join c.ruta r")
	List<ClienteDto> consultarClientesDto();

	@Query(value = "SELECT new xyz.pangosoft.dtodo.dto.ClienteDto(" +
			"c.idCliente, c.nombre, c.nit, c.direccion, c.fechaRegistro, c.telefono, c.codigoCliente, c.nombreNegocio, " +
			"c.tipoCliente, c.categoriaDescuento, r.idRuta, r.nombre, c.contacto, c.telefonoContacto, " +
			"c.referenciaUbicacion, c.limiteCredito, c.diasCredito) " +
			"FROM Cliente c LEFT JOIN c.ruta r WHERE (:filtro = '' " +
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

	Optional<Cliente> findByCodigoCliente(String codigoCliente);

	List<Cliente> findByRuta_IdRutaOrderByNombre(Integer idRuta);

	long countByRuta_IdRuta(Integer idRuta);
}
