package xyz.pangosoft.dtodo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.pangosoft.dtodo.dto.CorrelativoDto;
import xyz.pangosoft.dtodo.model.Correlativo;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.Usuario;

public interface ICorrelativoRepository extends JpaRepository<Correlativo, Long> {
	
	// Buscar el correlativo del usuario logueado en el sistema
	public Optional<Correlativo> findByUsuarioAndEstado(Usuario usuario, Estado estado);

	@Query(value = "SELECT new xyz.pangosoft.dtodo.dto.CorrelativoDto(" +
			"c.idCorrelativo, c.correlativoInicial, c.correlativoFinal, c.correlativoActual, " +
			"c.serie, c.fechaCreacion, u.usuario, e.idEstado, e.estado) " +
			"FROM Correlativo c LEFT JOIN c.usuario u LEFT JOIN c.estado e " +
			"WHERE (:filtro = '' " +
			"OR LOWER(COALESCE(c.serie, '')) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(COALESCE(u.usuario, '')) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(COALESCE(e.estado, '')) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR STR(c.idCorrelativo) LIKE CONCAT('%', :filtro, '%') " +
			"OR STR(c.correlativoInicial) LIKE CONCAT('%', :filtro, '%') " +
			"OR STR(c.correlativoFinal) LIKE CONCAT('%', :filtro, '%') " +
			"OR STR(c.correlativoActual) LIKE CONCAT('%', :filtro, '%'))",
			countQuery = "SELECT COUNT(c) FROM Correlativo c " +
					"LEFT JOIN c.usuario u LEFT JOIN c.estado e " +
					"WHERE (:filtro = '' " +
					"OR LOWER(COALESCE(c.serie, '')) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(COALESCE(u.usuario, '')) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(COALESCE(e.estado, '')) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR STR(c.idCorrelativo) LIKE CONCAT('%', :filtro, '%') " +
					"OR STR(c.correlativoInicial) LIKE CONCAT('%', :filtro, '%') " +
					"OR STR(c.correlativoFinal) LIKE CONCAT('%', :filtro, '%') " +
					"OR STR(c.correlativoActual) LIKE CONCAT('%', :filtro, '%'))")
	Page<CorrelativoDto> findListado(@Param("filtro") String filtro, Pageable pageable);

}
