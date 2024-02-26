package com.aglayatech.licorstore.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.aglayatech.licorstore.model.MovimientoProducto;
import com.aglayatech.licorstore.model.Producto;

public interface IMovimientoProductoRepository extends JpaRepository<MovimientoProducto, Long> {
	
	Page<MovimientoProducto> findByProducto(Producto producto, Pageable pageable);
	
	// Movimientos listados por rango de fechas
	List<MovimientoProducto> findByFechaMovimientoBetween(Date fechaIni, Date fechaFin);

}
