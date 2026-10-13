package xyz.pangosoft.dtodo.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.pangosoft.dtodo.dto.MovimientoBodegaDto;
import xyz.pangosoft.dtodo.model.MovimientoBodega;
import xyz.pangosoft.dtodo.model.enums.TipoMovimientoBodegaEnum;

public interface IMovimientoBodegaRepository extends JpaRepository<MovimientoBodega, Long> {

	@Query(value = "SELECT new xyz.pangosoft.dtodo.dto.MovimientoBodegaDto(" +
			"m.idMovimiento, m.fechaMovimiento, m.tipoMovimiento, m.cantidad, m.stockInicial, m.stockFinal, " +
			"m.motivo, m.tipoDocumentoOrigen, m.idDocumentoOrigen, p.codProducto, p.nombre, u.usuario) " +
			"FROM MovimientoBodega m JOIN m.producto p LEFT JOIN m.usuario u " +
			"WHERE m.bodega.idBodega = :idBodega " +
			"AND m.fechaMovimiento >= :desde AND m.fechaMovimiento < :hasta " +
			"AND (:tipo IS NULL OR m.tipoMovimiento = :tipo) " +
			"AND (:filtro = '' OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(p.codProducto) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(COALESCE(m.motivo, '')) LIKE LOWER(CONCAT('%', :filtro, '%')))",
			countQuery = "SELECT COUNT(m) FROM MovimientoBodega m JOIN m.producto p " +
					"WHERE m.bodega.idBodega = :idBodega " +
					"AND m.fechaMovimiento >= :desde AND m.fechaMovimiento < :hasta " +
					"AND (:tipo IS NULL OR m.tipoMovimiento = :tipo) " +
					"AND (:filtro = '' OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(p.codProducto) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(COALESCE(m.motivo, '')) LIKE LOWER(CONCAT('%', :filtro, '%')))")
	Page<MovimientoBodegaDto> findListado(
			@Param("idBodega") Integer idBodega,
			@Param("desde") LocalDateTime desde,
			@Param("hasta") LocalDateTime hasta,
			@Param("tipo") TipoMovimientoBodegaEnum tipo,
			@Param("filtro") String filtro,
			Pageable pageable);

	/**
	 * Registra un movimiento IMPORTACION por cada producto que la bodega tiene actualmente con existencias.
	 * Los productos con stock cero se copian al inventario pero no generan movimiento (no hay nada que trasladar).
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = "INSERT INTO movimientos_bodega (fecha_movimiento, tipo_movimiento, cantidad, stock_inicial, " +
			"stock_final, motivo, id_bodega, id_producto, id_usuario) " +
			"SELECT NOW(), 'IMPORTACION', i.stock, 0, i.stock, :motivo, i.id_bodega, i.id_producto, :idUsuario " +
			"FROM inventario_bodega i WHERE i.id_bodega = :idBodega AND i.stock > 0",
			nativeQuery = true)
	int registrarImportacionMasiva(@Param("idBodega") Integer idBodega,
			@Param("idUsuario") Integer idUsuario, @Param("motivo") String motivo);

}
