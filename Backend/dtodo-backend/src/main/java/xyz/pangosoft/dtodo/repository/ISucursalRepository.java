package xyz.pangosoft.dtodo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.pangosoft.dtodo.dto.SucursalDto;
import xyz.pangosoft.dtodo.model.Sucursal;

public interface ISucursalRepository extends JpaRepository<Sucursal, Integer> {

	@Query(value = "SELECT new xyz.pangosoft.dtodo.dto.SucursalDto(" +
			"s.idSucursal, s.nombre, s.direccion, s.telefono, s.encargado, " +
			"s.codigoEstablecimientoSat, s.esPrincipal, s.fechaRegistro, e.estado, u.usuario) " +
			"FROM Sucursal s LEFT JOIN s.estado e LEFT JOIN s.usuario u " +
			"WHERE (:filtro = '' OR LOWER(s.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(s.direccion) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(COALESCE(s.encargado, '')) LIKE LOWER(CONCAT('%', :filtro, '%')))",
			countQuery = "SELECT COUNT(s) FROM Sucursal s " +
					"WHERE (:filtro = '' OR LOWER(s.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(s.direccion) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(COALESCE(s.encargado, '')) LIKE LOWER(CONCAT('%', :filtro, '%')))")
	Page<SucursalDto> findListado(@Param("filtro") String filtro, Pageable pageable);

	Sucursal findByEsPrincipalTrue();

}
