package xyz.pangosoft.dtodo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.dto.DespachoBodegaDto;
import xyz.pangosoft.dtodo.dto.DespachoBodegaRequest;
import xyz.pangosoft.dtodo.model.DespachoBodega;

public interface IDespachoBodegaService {

	// Devuelve el listado paginado de despachos; bodega y estado (PENDIENTE, REALIZADO, CANCELADO) son filtros opcionales
	public Page<DespachoBodegaDto> findListado(String filtro, Integer idBodega, String estado, Pageable pageable);

	// Devuelve un despacho con su detalle o lanza NotFoundException
	public DespachoBodega findById(Long idDespacho);

	// Registra un despacho PENDIENTE y descuenta de la bodega las existencias despachadas (quedan reservadas)
	public DespachoBodega crear(DespachoBodegaRequest request, Integer idUsuario);

	// Aprueba un despacho pendiente: las existencias ingresan al inventario de la sucursal destino
	public DespachoBodega aprobar(Long idDespacho, Integer idUsuario);

	// Cancela un despacho pendiente (las existencias reservadas regresan a la bodega) o, solo un administrador,
	// revierte uno aprobado (las unidades salen de la sucursal destino y regresan a la bodega)
	public DespachoBodega cancelar(Long idDespacho, String motivo, Integer idUsuario, boolean esAdministrador);

}
