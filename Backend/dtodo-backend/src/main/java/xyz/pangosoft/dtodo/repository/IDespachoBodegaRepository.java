package xyz.pangosoft.dtodo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

import xyz.pangosoft.dtodo.dto.DespachoBodegaDto;
import xyz.pangosoft.dtodo.model.DespachoBodega;
import xyz.pangosoft.dtodo.model.enums.EstadoDespachoBodegaEnum;

public interface IDespachoBodegaRepository extends JpaRepository<DespachoBodega, Long> {

	/** Bloquea el despacho mientras se aprueba o cancela, para que dos usuarios no lo resuelvan a la vez. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT d FROM DespachoBodega d WHERE d.idDespacho = :idDespacho")
	Optional<DespachoBodega> findParaResolver(@Param("idDespacho") Long idDespacho);

	@Query(value = "SELECT new xyz.pangosoft.dtodo.dto.DespachoBodegaDto(" +
			"d.idDespacho, d.fechaRegistro, d.fechaResolucion, d.estado, d.total, d.recibidoPor, d.observaciones, " +
			"b.idBodega, b.nombre, s.idSucursal, s.nombre, bd.idBodega, bd.nombre, " +
			"concat(coalesce(ud.primerNombre, ''), ' ', coalesce(ud.apellido, '')), " +
			"concat(coalesce(ur.primerNombre, ''), ' ', coalesce(ur.apellido, '')), " +
			"size(d.items)) " +
			"FROM DespachoBodega d JOIN d.bodega b LEFT JOIN d.sucursalDestino s LEFT JOIN d.bodegaDestino bd " +
			"JOIN d.usuarioDespacha ud " +
			"LEFT JOIN d.usuarioResuelve ur " +
			"WHERE (:idBodega IS NULL OR b.idBodega = :idBodega) " +
			"AND (:estado IS NULL OR d.estado = :estado) " +
			"AND (:filtro = '' OR CAST(d.idDespacho AS string) LIKE CONCAT('%', :filtro, '%') " +
			"OR LOWER(b.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(s.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(bd.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(d.recibidoPor) LIKE LOWER(CONCAT('%', :filtro, '%')))",
			countQuery = "SELECT COUNT(d) FROM DespachoBodega d JOIN d.bodega b LEFT JOIN d.sucursalDestino s " +
					"LEFT JOIN d.bodegaDestino bd " +
					"WHERE (:idBodega IS NULL OR b.idBodega = :idBodega) " +
					"AND (:estado IS NULL OR d.estado = :estado) " +
					"AND (:filtro = '' OR CAST(d.idDespacho AS string) LIKE CONCAT('%', :filtro, '%') " +
					"OR LOWER(b.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(s.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(bd.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(d.recibidoPor) LIKE LOWER(CONCAT('%', :filtro, '%')))")
	Page<DespachoBodegaDto> findListado(
			@Param("filtro") String filtro,
			@Param("idBodega") Integer idBodega,
			@Param("estado") EstadoDespachoBodegaEnum estado,
			Pageable pageable);

}
