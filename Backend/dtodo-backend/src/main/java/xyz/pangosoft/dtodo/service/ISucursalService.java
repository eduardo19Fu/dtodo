package xyz.pangosoft.dtodo.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.dto.SucursalDto;
import xyz.pangosoft.dtodo.model.Sucursal;

public interface ISucursalService {

	// Devuelve un listado con todas las sucursales guardadas en la base de datos
	public List<Sucursal> findAll();

	// Devuelve un listado paginable PARA el frontend
	public Page<Sucursal> findAll(Pageable pageable);

	public Page<SucursalDto> findListado(String filtro, Pageable pageable);

	// Devuelve la sucursal encontrada por su id en la base de datos
	public Sucursal findById(Integer idSucursal);

	// Devuelve la sucursal marcada como principal (creada durante la migración de datos existentes)
	public Sucursal findPrincipal();

	// Registra o actualiza una sucursal
	public Sucursal save(Sucursal sucursal);

}
