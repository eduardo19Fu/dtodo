package xyz.pangosoft.dtodo.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.pangosoft.dtodo.model.MarcaProducto;
import xyz.pangosoft.dtodo.dto.MarcaProductoDto;

public interface IMarcaProductoRepository extends JpaRepository<MarcaProducto, Integer> {
	
	// Consulta que permite buscar marca por nombre
	@Query("Select m from MarcaProducto m where m.marca = :param1")
	List<MarcaProducto> findByMarca(@Param("param1") String marca);

	@Query(value = "SELECT new xyz.pangosoft.dtodo.dto.MarcaProductoDto(" +
			"m.idMarcaProducto, m.marca, m.fechaRegistro, u.usuario) " +
			"FROM MarcaProducto m LEFT JOIN m.usuario u " +
			"WHERE (:filtro = '' OR LOWER(m.marca) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(COALESCE(u.usuario, '')) LIKE LOWER(CONCAT('%', :filtro, '%')))",
			countQuery = "SELECT COUNT(m) FROM MarcaProducto m LEFT JOIN m.usuario u " +
					"WHERE (:filtro = '' OR LOWER(m.marca) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(COALESCE(u.usuario, '')) LIKE LOWER(CONCAT('%', :filtro, '%')))")
	Page<MarcaProductoDto> findListado(@Param("filtro") String filtro, Pageable pageable);

}
