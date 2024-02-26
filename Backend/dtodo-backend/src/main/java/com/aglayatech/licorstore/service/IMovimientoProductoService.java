package com.aglayatech.licorstore.service;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.aglayatech.licorstore.model.MovimientoProducto;
import com.aglayatech.licorstore.model.Producto;

import net.sf.jasperreports.engine.JRException;

public interface IMovimientoProductoService {
	
	public List<MovimientoProducto> findAll();
	
	public List<MovimientoProducto> findByFecha(Date fechaIni, Date fechaFin);
	
	public Page<MovimientoProducto> findAll(Pageable pageble);
	
	public Page<MovimientoProducto> findProductoMoves(Producto producto, Pageable pageable);
	
	public MovimientoProducto save(MovimientoProducto movimientoProducto);
	
	/********* PDF REPORTS SERVICES ***********/
	
	public byte[] inventory(Date fechaIni, Date fechaFin) throws JRException, FileNotFoundException, SQLException;
}
