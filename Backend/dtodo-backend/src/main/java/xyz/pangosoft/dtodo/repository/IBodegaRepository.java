package xyz.pangosoft.dtodo.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.pangosoft.dtodo.dto.BodegaDto;
import xyz.pangosoft.dtodo.dto.ReporteSelectorDto;
import xyz.pangosoft.dtodo.model.Bodega;

public interface IBodegaRepository extends JpaRepository<Bodega, Integer> {

	@Query("select new xyz.pangosoft.dtodo.dto.ReporteSelectorDto(" +
			"b.idBodega, b.nombre, b.ubicacion) from Bodega b order by b.nombre")
	List<ReporteSelectorDto> findOpcionesReporte();

	@Query(value = "SELECT new xyz.pangosoft.dtodo.dto.BodegaDto(" +
			"b.idBodega, b.nombre, b.ubicacion, b.descripcion, b.encargado, b.telefono, b.fechaRegistro, " +
			"e.estado, s.idSucursal, s.nombre, u.usuario, " +
			"(SELECT COUNT(i) FROM InventarioBodega i WHERE i.bodega = b), " +
			"(SELECT COALESCE(SUM(i.stock), 0) FROM InventarioBodega i WHERE i.bodega = b)) " +
			"FROM Bodega b LEFT JOIN b.estado e LEFT JOIN b.sucursal s LEFT JOIN b.usuario u " +
			"WHERE (:filtro = '' OR LOWER(b.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(b.ubicacion) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(COALESCE(b.encargado, '')) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(COALESCE(s.nombre, '')) LIKE LOWER(CONCAT('%', :filtro, '%')))",
			countQuery = "SELECT COUNT(b) FROM Bodega b LEFT JOIN b.sucursal s " +
					"WHERE (:filtro = '' OR LOWER(b.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(b.ubicacion) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(COALESCE(b.encargado, '')) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(COALESCE(s.nombre, '')) LIKE LOWER(CONCAT('%', :filtro, '%')))")
	Page<BodegaDto> findListado(@Param("filtro") String filtro, Pageable pageable);

	boolean existsByNombreIgnoreCase(String nombre);

	boolean existsByNombreIgnoreCaseAndIdBodegaNot(String nombre, Integer idBodega);

}
