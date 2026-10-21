package xyz.pangosoft.dtodo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.dto.DevolucionBodegaDto;
import xyz.pangosoft.dtodo.dto.DevolucionBodegaRequest;
import xyz.pangosoft.dtodo.dto.InventarioSucursalDto;
import xyz.pangosoft.dtodo.model.DevolucionBodega;

public interface IDevolucionBodegaService {

	// Devuelve el listado paginado de devoluciones; bodega y estado (PENDIENTE, REALIZADO, CANCELADO) son filtros opcionales
	public Page<DevolucionBodegaDto> findListado(String filtro, Integer idBodega, String estado, Pageable pageable);

	// Productos con su existencia en la sucursal que devuelve; es lo que se puede elegir al registrar una devolución
	public Page<InventarioSucursalDto> findProductosSucursal(Integer idSucursal, String filtro, Pageable pageable);

	// Devuelve una devolución con su detalle o lanza NotFoundException
	public DevolucionBodega findById(Long idDevolucion);

	// Registra una devolución PENDIENTE y descuenta de la sucursal las existencias devueltas (quedan reservadas)
	public DevolucionBodega crear(DevolucionBodegaRequest request, Integer idUsuario);

	// Aprueba una devolución pendiente: las existencias ingresan al inventario de la bodega
	public DevolucionBodega aprobar(Long idDevolucion, Integer idUsuario);

	// Rechaza o cancela una devolución pendiente: las existencias reservadas regresan a la sucursal
	public DevolucionBodega cancelar(Long idDevolucion, String motivo, Integer idUsuario);

}
