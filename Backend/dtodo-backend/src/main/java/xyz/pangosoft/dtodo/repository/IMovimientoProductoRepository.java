package xyz.pangosoft.dtodo.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import xyz.pangosoft.dtodo.model.MovimientoProducto;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.dto.MovimientoProductoDto;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IMovimientoProductoRepository extends JpaRepository<MovimientoProducto, Long> {
	
	Page<MovimientoProducto> findByProducto(Producto producto, Pageable pageable);
	
	// Movimientos listados por rango de fechas
	List<MovimientoProducto> findByFechaMovimientoBetween(Date fechaIni, Date fechaFin);

	@Query(value = "select mp.id_movimiento," +
			"mp.fecha_movimiento," +
			"mp.stock_inicial, " +
			"mp.tipo_movimiento," +
			"mp.cantidad, " +
			"mp.id_producto, " +
			"mp.id_usuario, " +
			"p.nombre, " +
			"u.usuario " +
			"from movimientos_producto as mp " +
			"inner join productos as p on p.id_producto = mp.id_producto " +
			"inner join usuarios as u on u.id_usuario = mp.id_usuario " +
			"order by mp.fecha_movimiento desc " +
			"limit 1000 ", nativeQuery = true)
	List<MovimientoProducto> findAllMovimientosProducto();

	@Query(value = "SELECT mp.id_movimiento, " +
				"mp.fecha_movimiento, " +
				"mp.stock_inicial, " +
				"mp.tipo_movimiento, " +
				"mp.cantidad, " +
				"p.nombre, " +
				"u.usuario " +
				"FROM movimientos_producto AS mp " +
				"INNER JOIN productos AS p ON p.id_producto = mp.id_producto " +
				"INNER JOIN usuarios AS u ON u.id_usuario = mp.id_usuario " +
				"ORDER BY mp.fecha_movimiento DESC",
			countQuery = "SELECT COUNT(*) FROM movimientos_producto",
			nativeQuery = true)
	Page<Object[]> findAllMovimientosDto(Pageable pageable);

	@Query(value = "SELECT mp.id_movimiento, " +
				"mp.fecha_movimiento, " +
				"mp.stock_inicial, " +
				"mp.tipo_movimiento, " +
				"mp.cantidad, " +
				"p.nombre, " +
				"u.usuario " +
				"FROM movimientos_producto AS mp " +
				"INNER JOIN productos AS p ON p.id_producto = mp.id_producto " +
				"INNER JOIN usuarios AS u ON u.id_usuario = mp.id_usuario " +
				"WHERE p.nombre LIKE %:filtro% " +
				"OR u.usuario LIKE %:filtro% " +
				"OR mp.tipo_movimiento LIKE %:filtro% " +
				"ORDER BY mp.fecha_movimiento DESC",
			countQuery = "SELECT COUNT(*) " +
				"FROM movimientos_producto AS mp " +
				"INNER JOIN productos AS p ON p.id_producto = mp.id_producto " +
				"INNER JOIN usuarios AS u ON u.id_usuario = mp.id_usuario " +
				"WHERE p.nombre LIKE %:filtro% " +
				"OR u.usuario LIKE %:filtro% " +
				"OR mp.tipo_movimiento LIKE %:filtro%",
			nativeQuery = true)
	Page<Object[]> searchMovimientosDto(@Param("filtro") String filtro, Pageable pageable);

	@Query(value = "SELECT new xyz.pangosoft.dtodo.dto.MovimientoProductoDto(" +
			"m.idMovimiento, m.fechaMovimiento, m.stockInicial, m.tipoMovimiento, " +
			"m.cantidad, p.nombre, u.usuario) FROM MovimientoProducto m " +
			"JOIN m.producto p JOIN m.usuario u WHERE (:filtro = '' " +
			"OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(u.usuario) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(STR(m.tipoMovimiento)) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR STR(m.idMovimiento) LIKE CONCAT('%', :filtro, '%') " +
			"OR STR(m.stockInicial) LIKE CONCAT('%', :filtro, '%') " +
			"OR STR(m.cantidad) LIKE CONCAT('%', :filtro, '%'))",
			countQuery = "SELECT COUNT(m) FROM MovimientoProducto m " +
					"JOIN m.producto p JOIN m.usuario u WHERE (:filtro = '' " +
					"OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(u.usuario) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(STR(m.tipoMovimiento)) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR STR(m.idMovimiento) LIKE CONCAT('%', :filtro, '%') " +
					"OR STR(m.stockInicial) LIKE CONCAT('%', :filtro, '%') " +
					"OR STR(m.cantidad) LIKE CONCAT('%', :filtro, '%'))")
	Page<MovimientoProductoDto> findListado(@Param("filtro") String filtro, Pageable pageable);

}
