package xyz.pangosoft.dtodo.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import xyz.pangosoft.dtodo.dto.ProveedorDto;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Proveedor;
import xyz.pangosoft.dtodo.repository.IProveedorRepository;
import xyz.pangosoft.dtodo.service.IEstadoService;
import xyz.pangosoft.dtodo.service.IProveedorService;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProveedorServiceImpl implements IProveedorService {

	private final IProveedorRepository proveedorRepository;

	private final IEstadoService estadoService;

	@Transactional(readOnly = true)
	@Override
	public List<Proveedor> findAll() {
		try {
			return proveedorRepository.findAll(Sort.by(Direction.ASC, "nombre"));
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Page<ProveedorDto> findListado(String filtro, Pageable pageable) {
		try {
			return proveedorRepository.findListado(filtro == null ? "" : filtro.trim(), pageable);
		} catch (DataAccessException e) {
			log.error("Error al consultar el listado paginado de proveedores: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error al consultar los proveedores", e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Proveedor findById(Integer idProveedor) {
		Optional<Proveedor> proveedor = proveedorRepository.findById(idProveedor);
		if (proveedor.isPresent()) {
			return proveedor.get();
		}
		throw new NotFoundException("El proveedor: " + idProveedor + ", no se encuentra registrado en la base de datos");
	}

	@Transactional
	@Override
	public Proveedor save(Proveedor proveedor) {
		try {
			if (proveedor.getIdProveedor() != null) {
				log.info("Actualizando proveedor: {}", proveedor.getIdProveedor());
				Proveedor proveedorExistente = findById(proveedor.getIdProveedor());
				proveedor.setFechaRegistro(proveedorExistente.getFechaRegistro());
				proveedor.setUsuario(proveedorExistente.getUsuario());
				if (proveedor.getEstado() == null) {
					proveedor.setEstado(proveedorExistente.getEstado());
				}
			} else {
				log.info("Registrando proveedor nuevo: {}", proveedor.getNombre());
				proveedor.setEstado(estadoService.findByEstado("ACTIVO"));
			}
			return proveedorRepository.save(proveedor);
		} catch (NotFoundException e) {
			throw e;
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		}
	}

}
