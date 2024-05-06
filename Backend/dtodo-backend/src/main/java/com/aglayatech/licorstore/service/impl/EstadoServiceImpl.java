package com.aglayatech.licorstore.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.aglayatech.licorstore.error.exceptions.DataAccessException;
import com.aglayatech.licorstore.error.exceptions.NoContentException;
import com.aglayatech.licorstore.error.exceptions.NotFoundException;
import com.aglayatech.licorstore.model.Estado;
import com.aglayatech.licorstore.repository.IEstadoRepository;
import com.aglayatech.licorstore.service.IEstadoService;

@Service
@RequiredArgsConstructor
@Slf4j
public class EstadoServiceImpl implements IEstadoService {

	private final IEstadoRepository repoEstado;
	
	@Override
	public List<Estado> findAll() {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		List<Estado> estados = new ArrayList<>();

		try {
			estados = repoEstado.findAll(Sort.by(Direction.ASC, "idEstado"));

			if(!estados.isEmpty()) {
				log.info("Devolviendo estados disponibles: {}", estados);
				return estados;
			} else {
				log.warn("Listado de estados se encuentra vacío");
				throw new NoContentException("Listado de estados se encuentra vacío. No existen estados registrados");
			}
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e.getMessage());
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Error al acceder a la base de datos", e);
		} catch (Exception e) {
			log.error("Un error ha ocurrido => {}", e);
			throw new RuntimeException("Error => ", e);
		} finally {
			log.debug("{} Exit ", __method);
		}
	}

	@Override
	public Estado findById(Integer idestado) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		Optional<Estado> estado;
		try {
			estado = repoEstado.findById(idestado);

			if (estado.isPresent()) {
				return estado.get();
			} else {
				log.warn("Estado con ID: {}, no se encuentra registrado", idestado);
				throw new NotFoundException("Estado con ID: " + idestado + " no se encuentra registrado en la base de datos");
			}
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e);
			throw new RuntimeException("Ha ocurrido un error inesperado: ", e);
		} finally {
			log.debug("{} Exit ", __method);
		}
	}
	
	@Override
	public Estado findByEstado(String nombre) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		Optional<Estado> estado;

		try {
			estado = repoEstado.findByEstado(nombre);

			if(estado.isPresent()) {
				log.info("Devolviendo estado con nombre: {}", estado);
				return estado.get();
			} else {
				log.warn("Estado con nombre: {}, no se encuentra registrado", estado);
				throw new NotFoundException("Estado con nombre: " + estado + ", no se encuentra registrado en la base de datos");
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
	public Estado save(Estado estado) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		try {
			if(estado.getIdEstado() == null) {
				log.info("Registrando nuevo Estado: {}", estado);
                return repoEstado.save(estado);
			} else {
				log.info("Actualizando estado: {}", estado);
                return repoEstado.save(estado);
			}
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e);
			throw new RuntimeException("Ha ocurrido un error inesperado: ", e);
		}
	}

	@Override
	public void delete(Estado estado) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		try {
			if(estado != null) {
				log.info("Eliminando estado: {}", estado);
				repoEstado.deleteById(estado.getIdEstado());
			}
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e);
			throw new RuntimeException("Ha ocurrido un error inesperado: ", e);
		}
	}

}
