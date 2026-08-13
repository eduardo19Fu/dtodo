package xyz.pangosoft.dtodo.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import xyz.pangosoft.dtodo.model.MovimientoProducto;
import xyz.pangosoft.dtodo.model.Producto;
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

}
