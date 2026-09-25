package xyz.pangosoft.dtodo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.pangosoft.dtodo.dto.ProveedorDto;
import xyz.pangosoft.dtodo.dto.ReporteSelectorDto;
import xyz.pangosoft.dtodo.model.Proveedor;

import java.util.List;

public interface IProveedorRepository extends JpaRepository<Proveedor, Integer> {

	@Query("select new xyz.pangosoft.dtodo.dto.ReporteSelectorDto(" +
			"p.idProveedor, p.nombre, coalesce(p.contacto, p.telefonoEntidad, '')) " +
			"from Proveedor p order by p.nombre")
	List<ReporteSelectorDto> findOpcionesReporte();

	@Query(value = "SELECT new xyz.pangosoft.dtodo.dto.ProveedorDto(" +
			"p.idProveedor, p.nombre, p.contacto, p.telefonoEntidad, p.telefonoContacto, " +
			"p.emailEntidad, p.emailContacto, p.direccion, p.sitioWeb, pa.nombre, e.estado, p.fechaRegistro, u.usuario, " +
			"concat(coalesce(u.primerNombre, ''), ' ', coalesce(u.apellido, ''))) " +
			"FROM Proveedor p LEFT JOIN p.pais pa LEFT JOIN p.estado e LEFT JOIN p.usuario u " +
			"WHERE (:filtro = '' OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(COALESCE(p.contacto, '')) LIKE LOWER(CONCAT('%', :filtro, '%')))",
			countQuery = "SELECT COUNT(p) FROM Proveedor p " +
					"WHERE (:filtro = '' OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(COALESCE(p.contacto, '')) LIKE LOWER(CONCAT('%', :filtro, '%')))")
	Page<ProveedorDto> findListado(@Param("filtro") String filtro, Pageable pageable);

}
