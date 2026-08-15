package xyz.pangosoft.dtodo.service;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import xyz.pangosoft.dtodo.dto.MovimientoProductoDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.model.MovimientoProducto;
import xyz.pangosoft.dtodo.model.Producto;

import net.sf.jasperreports.engine.JRException;

public interface IMovimientoProductoService {

	public List<MovimientoProducto> findAll();

	public List<MovimientoProducto> findByFecha(Date fechaIni, Date fechaFin);

	public Page<MovimientoProducto> findAll(Pageable pageble);

	public Page<MovimientoProductoDto> findAllDtoMejorado(Pageable pageable);

	public Page<MovimientoProductoDto> searchMovimientoDtoMejorado(String filtro, Pageable pageable);

	public Page<MovimientoProductoDto> findListado(String filtro, Pageable pageable);

	public Page<MovimientoProducto> findProductoMoves(Producto producto, Pageable pageable);

	public MovimientoProducto save(MovimientoProducto movimientoProducto);

	/********* PDF REPORTS SERVICES ***********/

	public byte[] inventory(Date fechaIni, Date fechaFin) throws JRException, FileNotFoundException, SQLException;
}
