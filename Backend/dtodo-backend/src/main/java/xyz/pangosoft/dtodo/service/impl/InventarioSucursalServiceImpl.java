package xyz.pangosoft.dtodo.service.impl;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import xyz.pangosoft.dtodo.dto.InventarioSucursalDto;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.model.InventarioSucursal;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.repository.IInventarioSucursalRepository;
import xyz.pangosoft.dtodo.service.IInventarioSucursalService;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventarioSucursalServiceImpl implements IInventarioSucursalService {

	private final IInventarioSucursalRepository inventarioRepo;

	@Transactional(readOnly = true)
	@Override
	public Page<InventarioSucursalDto> findListado(Integer idSucursal, String filtro, Pageable pageable) {
		try {
			return inventarioRepo.findListado(idSucursal, filtro == null ? "" : filtro.trim(), pageable);
		} catch (DataAccessException e) {
			log.error("Error al consultar el inventario de la sucursal {}: {}", idSucursal, e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
					"Ha ocurrido un error al consultar el inventario de la sucursal", e);
		}
	}

	@Transactional
	@Override
	public InventarioSucursal obtenerOCrear(Sucursal sucursal, Producto producto) {
		return inventarioRepo.findBySucursal_IdSucursalAndProducto_IdProducto(
				sucursal.getIdSucursal(), producto.getIdProducto())
				.orElseGet(() -> {
					log.info("Creando fila de inventario para producto {} en sucursal {}",
							producto.getIdProducto(), sucursal.getIdSucursal());
					InventarioSucursal nuevo = InventarioSucursal.builder()
							.sucursal(sucursal)
							.producto(producto)
							.stock(0)
							.build();
					return inventarioRepo.save(nuevo);
				});
	}

	@Transactional(readOnly = true)
	@Override
	public int obtenerStock(Integer idSucursal, Integer idProducto) {
		return inventarioRepo.findBySucursal_IdSucursalAndProducto_IdProducto(idSucursal, idProducto)
				.map(InventarioSucursal::getStock)
				.orElse(0);
	}

	@Transactional
	@Override
	public InventarioSucursal guardar(InventarioSucursal inventarioSucursal) {
		inventarioSucursal.setFechaActualizacion(LocalDateTime.now());
		try {
			return inventarioRepo.save(inventarioSucursal);
		} catch (DataAccessException e) {
			log.error("Error al guardar el inventario de sucursal: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
					"Ha ocurrido un error al guardar el inventario de la sucursal", e);
		}
	}

	@Transactional
	@Override
	public InventarioSucursal ajustarStock(Integer idSucursal, Integer idProducto, Integer nuevoStock, Integer stockMinimo) {
		if (nuevoStock == null || nuevoStock < 0) {
			throw new BadRequestException("El stock ingresado no es válido.", null);
		}

		InventarioSucursal inventario = inventarioRepo
				.findBySucursal_IdSucursalAndProducto_IdProducto(idSucursal, idProducto)
				.orElseThrow(() -> new xyz.pangosoft.dtodo.error.exceptions.NotFoundException(
						"El producto " + idProducto + " no tiene inventario registrado en la sucursal " + idSucursal));

		inventario.setStock(nuevoStock);
		inventario.setStockMinimo(stockMinimo);
		return guardar(inventario);
	}

	@Transactional(readOnly = true)
	@Override
	public int contarPorSucursal(Integer idSucursal) {
		return (int) inventarioRepo.countBySucursal_IdSucursal(idSucursal);
	}

	@Transactional
	@Override
	public void clonarInventario(Integer idSucursalOrigen, Integer idSucursalDestino) {
		if (inventarioRepo.existsBySucursal_IdSucursal(idSucursalDestino)) {
			throw new BadRequestException(
					"La sucursal destino ya cuenta con inventario registrado; no se puede clonar de nuevo.", null);
		}

		log.info("Clonando inventario de la sucursal {} hacia la sucursal {}", idSucursalOrigen, idSucursalDestino);
		try {
			inventarioRepo.clonarInventario(idSucursalOrigen, idSucursalDestino);
		} catch (DataAccessException e) {
			log.error("Error al clonar el inventario: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
					"Ha ocurrido un error al clonar el inventario hacia la nueva sucursal", e);
		}
	}

}
