package com.aglayatech.licorstore.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.aglayatech.licorstore.model.MovimientoProducto;
import com.aglayatech.licorstore.model.Producto;
import org.springframework.data.jpa.repository.Query;

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

}
