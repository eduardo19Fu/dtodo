package xyz.pangosoft.dtodo.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.pangosoft.dtodo.dto.DevolucionBodegaDto;
import xyz.pangosoft.dtodo.model.DevolucionBodega;
import xyz.pangosoft.dtodo.model.enums.EstadoDespachoBodegaEnum;

public interface IDevolucionBodegaRepository extends JpaRepository<DevolucionBodega, Long> {

	/** Bloquea la devolución mientras se aprueba o rechaza, para que dos usuarios no la resuelvan a la vez. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT d FROM DevolucionBodega d WHERE d.idDevolucion = :idDevolucion")
	Optional<DevolucionBodega> findParaResolver(@Param("idDevolucion") Long idDevolucion);

	@Query(value = "SELECT new xyz.pangosoft.dtodo.dto.DevolucionBodegaDto(" +
			"d.idDevolucion, d.fechaRegistro, d.fechaResolucion, d.estado, d.total, d.entregadoPor, d.motivo, " +
			"s.idSucursal, s.nombre, b.idBodega, b.nombre, " +
			"concat(coalesce(ur.primerNombre, ''), ' ', coalesce(ur.apellido, '')), " +
			"concat(coalesce(uv.primerNombre, ''), ' ', coalesce(uv.apellido, '')), " +
			"size(d.items)) " +
			"FROM DevolucionBodega d JOIN d.sucursalOrigen s JOIN d.bodega b " +
			"JOIN d.usuarioRegistra ur " +
			"LEFT JOIN d.usuarioResuelve uv " +
			"WHERE (:idBodega IS NULL OR b.idBodega = :idBodega) " +
			"AND (:estado IS NULL OR d.estado = :estado) " +
			"AND (:filtro = '' OR CAST(d.idDevolucion AS string) LIKE CONCAT('%', :filtro, '%') " +
			"OR LOWER(b.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(s.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(d.entregadoPor) LIKE LOWER(CONCAT('%', :filtro, '%')))",
			countQuery = "SELECT COUNT(d) FROM DevolucionBodega d JOIN d.sucursalOrigen s JOIN d.bodega b " +
					"WHERE (:idBodega IS NULL OR b.idBodega = :idBodega) " +
					"AND (:estado IS NULL OR d.estado = :estado) " +
					"AND (:filtro = '' OR CAST(d.idDevolucion AS string) LIKE CONCAT('%', :filtro, '%') " +
					"OR LOWER(b.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(s.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(d.entregadoPor) LIKE LOWER(CONCAT('%', :filtro, '%')))")
	Page<DevolucionBodegaDto> findListado(
			@Param("filtro") String filtro,
			@Param("idBodega") Integer idBodega,
			@Param("estado") EstadoDespachoBodegaEnum estado,
			Pageable pageable);

}
