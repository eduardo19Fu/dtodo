package xyz.pangosoft.dtodo.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.pangosoft.dtodo.model.TipoProducto;
import xyz.pangosoft.dtodo.dto.TipoProductoDto;

public interface ITipoProductoRepository extends JpaRepository<TipoProducto, Integer> {
	
	// Consulta para encontrar un listado de tipos de producto que coincida con el dato ingresado por el usuario
	@Query("Select t from TipoProducto t where t.tipoProducto = :tipo")
	List<TipoProducto> findByTipoProducto(@Param("tipo") String tipo);

	@Query(value = "SELECT new xyz.pangosoft.dtodo.dto.TipoProductoDto(" +
			"t.idTipoProducto, t.tipoProducto, t.fechaRegistro, u.usuario) " +
			"FROM TipoProducto t LEFT JOIN t.usuario u " +
			"WHERE (:filtro = '' OR LOWER(t.tipoProducto) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(COALESCE(u.usuario, '')) LIKE LOWER(CONCAT('%', :filtro, '%')))",
			countQuery = "SELECT COUNT(t) FROM TipoProducto t LEFT JOIN t.usuario u " +
					"WHERE (:filtro = '' OR LOWER(t.tipoProducto) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(COALESCE(u.usuario, '')) LIKE LOWER(CONCAT('%', :filtro, '%')))")
	Page<TipoProductoDto> findListado(@Param("filtro") String filtro, Pageable pageable);

}
