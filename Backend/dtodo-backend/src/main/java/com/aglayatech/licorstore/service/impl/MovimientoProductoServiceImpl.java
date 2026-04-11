package com.aglayatech.licorstore.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.aglayatech.licorstore.dto.MovimientoProductoDto;
import com.aglayatech.licorstore.error.exceptions.DataAccessException;
import com.aglayatech.licorstore.error.exceptions.NoContentException;
import com.aglayatech.licorstore.model.Estado;
import com.aglayatech.licorstore.model.enums.TipoMovimientoEnum;
import com.aglayatech.licorstore.service.IEstadoService;
import com.aglayatech.licorstore.service.IProductoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aglayatech.licorstore.model.MovimientoProducto;
import com.aglayatech.licorstore.model.Producto;
import com.aglayatech.licorstore.repository.IMovimientoProductoRepository;
import com.aglayatech.licorstore.service.IMovimientoProductoService;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovimientoProductoServiceImpl implements IMovimientoProductoService {

	private final IMovimientoProductoRepository repoMovimiento;
	private final IEstadoService estadoService;
	private final IProductoService productoService;

	private final DataSource localDateSource;
	
	@Override
	public List<MovimientoProducto> findAll() {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		try {
			List<MovimientoProducto> movimientoProductos = repoMovimiento.findAllMovimientosProducto();
			if(!movimientoProductos.isEmpty()) {
				log.info("Devolviendo listado de movimientos");
				return movimientoProductos;
			} else {
				log.warn("No existen movimientos registrados");
				throw new NoContentException("No existen movimientos registrados");
			}
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e.getMessage());
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => " + e.getMessage(), e.getCause());
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e.getMessage());
			throw new RuntimeException("Ha ocurrido un error inesperado: " + e.getMessage());
		} finally {
			log.debug("{} Exit", __method);
		}
	}

	@Override
	public Page<MovimientoProducto> findAll(Pageable pageable) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		try {
			Page<MovimientoProducto> movimientoProductos = repoMovimiento.findAll(pageable);
			if(!movimientoProductos.isEmpty()) {
				log.info("Devolviendo listado de movimientos");
				return movimientoProductos;
			} else {
				log.warn("No existen movimientos registrados");
				throw new NoContentException("No existen movimientos registrados");
			}
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e);
			throw new RuntimeException("Ha ocurrido un error inesperado: " + e.getMessage());
		} finally {
			log.debug("{} Exit", __method);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Page<MovimientoProductoDto> findAllDtoMejorado(Pageable pageable) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		try {
			log.debug("Consultando movimientos desde la base de datos...");
			Page<Object[]> results = repoMovimiento.findAllMovimientosDto(pageable);

			if (results.isEmpty()) {
				log.warn("No existen movimientos registrados");
				throw new NoContentException("No existen movimientos registrados");
			}

			return mapPageToMovimientoProductoDto(results);
		} catch (DataAccessException dax) {
			log.error("Ha ocurrido un error al intentar consultar los movimientos: {}", dax.getMessage());
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error al consultar los movimientos => ", dax);
		} finally {
			log.debug("{} Exit", __method);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Page<MovimientoProductoDto> searchMovimientoDtoMejorado(String filtro, Pageable pageable) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		try {
			log.debug("Consultando movimientos desde la base de datos que coincidan con la busqueda...");
			Page<Object[]> results = repoMovimiento.searchMovimientosDto(filtro, pageable);

			return mapPageToMovimientoProductoDto(results);
		} catch (DataAccessException dax) {
			log.error("Ha ocurrido un error al intentar consultar los movimientos con busqueda {}: {}", filtro, dax.getMessage());
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error al consultar los movimientos => ", dax);
		} finally {
			log.debug("{} Exit", __method);
		}
	}

	@Override
	public Page<MovimientoProducto> findProductoMoves(Producto producto, Pageable pageable) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		try {
			Page<MovimientoProducto> movimientoProductos = repoMovimiento.findByProducto(producto, pageable);
			if(!movimientoProductos.isEmpty()) {
				log.info("Devolviendo listado de movimientos para el producto: {}", producto.getCodProducto());
				return movimientoProductos;
			} else {
				log.warn("No existen movimientos registrados para el producto: {}", producto.getCodProducto());
				throw new NoContentException("No existen movimientos registrados");
			}
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e);
			throw new RuntimeException("Ha ocurrido un error inesperado: ", e);
		} finally {
			log.debug("{} Exit", __method);
		}
	}

	@Override
	public MovimientoProducto save(MovimientoProducto movimientoProducto) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		try {
			if(calcularStock(movimientoProducto)) {
				MovimientoProducto movimientoProductoSaved = null;
				movimientoProductoSaved = repoMovimiento.save(movimientoProducto);
				return movimientoProductoSaved;
			} else {
				log.warn("No se ha podido llevar a cabo el registro del movimiento");
				throw new RuntimeException("No se ha podido llevar a cabo el registro");
			}
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e.getMessage());
			throw new RuntimeException("Ha ocurrido un error inesperado: " + e.getMessage());
		}
	}

	@Override
	public List<MovimientoProducto> findByFecha(Date fechaIni, Date fechaFin) {
		return repoMovimiento.findByFechaMovimientoBetween(fechaIni, fechaFin);
	}

	/**
	 * Método encargado de llevar a cabo la suma o resta de existencias dependiendo del tipo de movimiento que se
	 * esté llevando a cabo.
	 * @param movimientoProducto Objeto de tipo MovimientoProducto que guarda los datos a operar
	 * @return boolean Devuelve un valor verdadero si el procedimiento de guardado de las nuevas existencias del producto se ha
	 *         llevado a cabo con éxito.
	 *
	 * */
	public boolean calcularStock(MovimientoProducto movimientoProducto) {
		int tmpStock = 0;
		Producto producto = null;
		Producto productoSaved = null;
		Estado estado = null;

		try {
			tmpStock = movimientoProducto.getProducto().getStock();
			producto = movimientoProducto.getProducto();
			movimientoProducto.setStockInicial(tmpStock);

			switch (movimientoProducto.getTipoMovimiento()) {
				case VENTA:
				case SALIDA:
				case ELIMINAR_COMPRA:
				case ENTREGA_PRODUCTO_NOTA:
					log.debug("Operando salidas al stock por operaciones de tipo VENTA, SALIDA");
					producto.setStock(tmpStock - movimientoProducto.getCantidad());

//					if(producto.getStock() <= 12 && producto.getStock() > 0)
//						estado = estadoService.findByEstado("POR AGOTARSE");
//					else if(producto.getStock() == 0)
//						estado = estadoService.findByEstado("AGOTADO");
//					else
//						estado = estadoService.findByEstado("ACTIVO");

//					producto.setEstado(estado);
					break;
				case COMPRA:
				case ENTRADA:
				case ANULACION_FACTURA:
				case ANULACION_NOTA:
					log.debug("Operando suma al stock por operaciones de tipo COMPRA, ENTRADA");
					producto.setStock(tmpStock + movimientoProducto.getCantidad());

//					if(producto.getStock() > 12)
//						estado = estadoService.findByEstado("ACTIVO");
//					else if(producto.getStock() > 0)
//						estado = estadoService.findByEstado("POR AGOTARSE");

//					producto.setEstado(estado);
					break;
				default:
					log.debug("No existe la operación deseada");
					break;
			}

			productoSaved = productoService.save(producto);
		} catch (Exception ex) {
			log.error("Error: {}", ex.getMessage());
		}
		return (productoSaved != null);
	}

	/********* PDF REPORTS SERVICES ***********/
	
	@Override
	public byte[] inventory(Date fechaIni, Date fechaFin) 
			throws JRException, FileNotFoundException, SQLException { // REPORTE DE INVENTARIO
		
		Connection con = localDateSource.getConnection();
		Map<String, Object> params = new HashMap<>();
		InputStream file = getClass().getResourceAsStream("/reports/rpt_inventario.jrxml");

		params.put("fechaIni", fechaIni);
		params.put("fechaFin", fechaFin);
		
		JasperReport jasperReport = JasperCompileManager.compileReport(file);
		JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, con);
		
		ByteArrayOutputStream byteArrayOutputStream = getByteArrayOutputStream(jasperPrint);
		
		con.close();
		return byteArrayOutputStream.toByteArray();
	}
	
	protected ByteArrayOutputStream getByteArrayOutputStream(JasperPrint jasperPrint) throws JRException {
	    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	    JasperExportManager.exportReportToPdfStream(jasperPrint, byteArrayOutputStream);
	    return byteArrayOutputStream;
	}

	protected Page<MovimientoProductoDto> mapPageToMovimientoProductoDto(Page<Object[]> results) {
		return results.map(result -> MovimientoProductoDto.builder()
				.idMovimiento(((Number) result[0]).longValue())
				.fechaMovimiento(result[1] == null ? null : ((java.sql.Timestamp) result[1]).toLocalDateTime())
				.stockInicial(result[2] == null ? null : ((Number) result[2]).intValue())
				.tipoMovimiento(result[3] == null ? null : TipoMovimientoEnum.valueOf((String) result[3]))
				.cantidad(result[4] == null ? null : ((Number) result[4]).intValue())
				.productoNombre((String) result[5])
				.usuarioNombre((String) result[6])
				.build());
	}

}
