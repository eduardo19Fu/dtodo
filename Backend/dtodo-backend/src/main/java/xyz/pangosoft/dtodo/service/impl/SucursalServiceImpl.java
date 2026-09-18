package xyz.pangosoft.dtodo.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import xyz.pangosoft.dtodo.error.exceptions.NoContentException;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.dto.SucursalDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.repository.ISucursalRepository;
import xyz.pangosoft.dtodo.service.IEstadoService;
import xyz.pangosoft.dtodo.service.ISucursalService;

@Service
@RequiredArgsConstructor
@Slf4j
public class SucursalServiceImpl implements ISucursalService {

	private final ISucursalRepository sucursalRepo;

	private final IEstadoService estadoService;

	@Transactional(readOnly = true)
	@Override
	public List<Sucursal> findAll() {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		List<Sucursal> listadoSucursales = new ArrayList<>();

		try {
			log.info("Consultando sucursales registradas: ");
			listadoSucursales = sucursalRepo.findAll(Sort.by(Direction.ASC, "nombre"));

			if(!listadoSucursales.isEmpty()) {
				log.info("Sucursales encontradas: {}", listadoSucursales);
				return listadoSucursales;
			} else {
				log.warn("No existen sucursales registradas");
				throw new NoContentException("No existen sucursales registradas");
			}
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e);
			throw new RuntimeException("Ha ocurrido un error inesperado: ", e);
		} finally {
			log.debug("{} Exit", __method);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Page<Sucursal> findAll(Pageable pageable) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);
		Page<Sucursal> sucursalesPaginadas = null;

		try {
			sucursalesPaginadas = sucursalRepo.findAll(pageable);

			if(!sucursalesPaginadas.isEmpty()) {
				log.info("Devolviendo sucursales paginadas: {}", sucursalesPaginadas.getTotalPages());
				return sucursalesPaginadas;
			} else {
				log.warn("No existen sucursales registradas");
				throw new NoContentException("No existen sucursales registradas");
			}
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e);
			throw new RuntimeException("Ha ocurrido un error inesperado: ", e);
		} finally {
			log.debug("{} Exit", __method);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Page<SucursalDto> findListado(String filtro, Pageable pageable) {
		try {
			return sucursalRepo.findListado(filtro == null ? "" : filtro.trim(), pageable);
		} catch (DataAccessException e) {
			log.error("Error al consultar el listado paginado de sucursales: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
					"Ha ocurrido un error al consultar las sucursales", e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Long totalSucursales() {
		try {
			return sucursalRepo.count();
		} catch (DataAccessException e) {
			log.error("Error al contar las sucursales: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
					"Ha ocurrido un error al contar las sucursales", e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Sucursal findById(Integer idSucursal) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		Optional<Sucursal> sucursal = null;

		try {
			sucursal = sucursalRepo.findById(idSucursal);

			if(sucursal.isPresent()) {
				log.info("Obteniendo sucursal: {}", sucursal.get());
				return sucursal.get();
			} else {
				log.warn("La sucursal: {}, no se encuentra registrada en la base de datos", idSucursal);
				throw new NotFoundException("La sucursal:" + idSucursal + ", no se encuentra registrada en la base de datos");
			}
		} catch (NotFoundException e) {
			// Se relanza para que el handler global la mapee a 404 Not Found
			throw e;
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e);
			throw new RuntimeException("Ha ocurrido un error inesperado: ", e);
		} finally {
			log.debug("{} Exit", __method);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Sucursal findPrincipal() {
		Sucursal principal = sucursalRepo.findByEsPrincipalTrue();

		if (principal == null) {
			log.error("No existe ninguna sucursal marcada como principal");
			throw new NotFoundException("No existe ninguna sucursal marcada como principal");
		}

		return principal;
	}

	@Transactional
	@Override
	public Sucursal save(Sucursal sucursal) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		Sucursal newSucursal = null;

		try {
			if(sucursal.getIdSucursal() != null) {
				log.info("Actualizando sucursal: {}", sucursal.getIdSucursal());
				Sucursal sucursalExistente = findById(sucursal.getIdSucursal());
				sucursal.setFechaRegistro(sucursalExistente.getFechaRegistro());
				sucursal.setUsuario(sucursalExistente.getUsuario());
				sucursal.setEsPrincipal(sucursalExistente.isEsPrincipal());
				if (sucursal.getEstado() == null) {
					sucursal.setEstado(sucursalExistente.getEstado());
				}
			} else {
				log.info("Registrando sucursal nueva: {}", sucursal.getNombre());
				sucursal.setEstado(estadoService.findByEstado("ACTIVO"));
			}

			newSucursal = sucursalRepo.save(sucursal);
		} catch (NotFoundException e) {
			// Se relanza para que el handler global la mapee a 404 Not Found
			throw e;
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e);
			throw new RuntimeException("Ha ocurrido un error inesperado: ", e);
		}

		return newSucursal;
	}

}
