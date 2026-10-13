package xyz.pangosoft.dtodo.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.dto.BodegaDto;
import xyz.pangosoft.dtodo.model.Bodega;

public interface IBodegaService {

	// Devuelve las bodegas ordenadas por nombre; con soloActivas únicamente las que están en estado ACTIVO
	public List<Bodega> findAll(boolean soloActivas);

	// Devuelve el listado paginado de bodegas para el frontend, con el resumen de su inventario
	public Page<BodegaDto> findListado(String filtro, Pageable pageable);

	// Devuelve la bodega encontrada por su id o lanza NotFoundException
	public Bodega findById(Integer idBodega);

	// Igual que findById, pero rechaza (BadRequestException) las bodegas inactivas: no admiten operaciones nuevas
	public Bodega findActivaById(Integer idBodega);

	// Registra una bodega nueva (idBodega nulo) o actualiza una existente; idUsuario es quien registra
	public Bodega save(Bodega bodega, Integer idUsuario);

}
