package com.aglayatech.licorstore.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.aglayatech.licorstore.error.exceptions.NoContentException;
import com.aglayatech.licorstore.error.exceptions.NotFoundException;
import com.aglayatech.licorstore.service.IEstadoService;
import com.aglayatech.licorstore.service.IUsuarioService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aglayatech.licorstore.model.Correlativo;
import com.aglayatech.licorstore.model.Estado;
import com.aglayatech.licorstore.model.Usuario;
import com.aglayatech.licorstore.repository.ICorrelativoRepository;
import com.aglayatech.licorstore.service.ICorrelativoService;

@Service
@RequiredArgsConstructor
@Slf4j
public class CorrelativoServiceImpl implements ICorrelativoService {

	private final ICorrelativoRepository correlativoRepository;

	private final IUsuarioService usuarioService;

	private final IEstadoService estadoService;
	
	@Override
	public List<Correlativo> findAll() {

		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		List<Correlativo> correlativos = new ArrayList<>();
		log.debug("Enter {}", __method);

		try {
			log.info("Listando correlativos registrados");
			correlativos = correlativoRepository.findAll(Sort.by(Direction.ASC, "idCorrelativo"));

			if(correlativos != null && correlativos.size() > 0) {
				log.info("Listando correlativos creados: ", correlativos.size());
				return correlativos;
			} else {
				log.error("No existen correlativos registrados en la base de datos");
				throw new NoContentException("No existen correlativos registrados en la base de datos");
			}

		} catch (Exception e) {
			log.error("Error => {}", e);
			throw new RuntimeException("Ha ocurrido un error inesperado", e);
		}

	}

	@Override
	public Page<Correlativo> findAll(Pageable pageable) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		try {
			Page<Correlativo> correlativos = correlativoRepository.findAll(pageable);

			if(correlativos != null && correlativos.hasContent()) {
				return correlativos;
			} else {
				log.error("No hay contenido disponible para listar los correlativos");
				throw new NoContentException("No hay contenido disponible para listar correlativos");
			}
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado");
			throw new RuntimeException("Ha ocurrido un error inesperado");
		} finally {
			log.debug("{} Exit", __method);
		}
	}

	@Override
	public Correlativo findById(Long idcorrelativo) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		Optional<Correlativo> correlativo = null;
		log.debug("Enter {}", __method);

		try {
			correlativo = correlativoRepository.findById(idcorrelativo);

			if(correlativo.isPresent()) {
				log.info("Correlativo encontrado ID: {}", correlativo.get().getIdCorrelativo());
				return correlativo.get();
			} else {
				log.error("Correlativo con id: {}, no se encuentra registrado en la base de datos", idcorrelativo);
				throw new NotFoundException("Correlativo con ID: " + idcorrelativo + " no se encuentra registrado");
			}
		} catch(Exception e) {
			log.error("Ha ocurrido un error inesperado");
			throw new RuntimeException("Ha ocurrido un error inesperado", e);
		} finally {
			log.debug("{} Exit", __method);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Correlativo findByUsuario(Integer idusuario) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		Optional<Correlativo> correlativo = null;
		Usuario usuario = null;
		Estado estado = null;

		try {
			usuario = usuarioService.findById(idusuario);
			estado = estadoService.findById(1);

			if(usuario != null && estado != null) {
				log.info("Buscando el correlativo activo para el usuario: {}", usuario.getUsuario());
				correlativo = correlativoRepository.findByUsuarioAndEstado(usuario, estado);

				if(correlativo.isPresent()) {
					log.info("Correlativo activo: {} para el usuario: {}", correlativo.get().getIdCorrelativo(), usuario.getUsuario());
					return correlativo.get();
				} else {
					log.warn("No existen correlativos activos para este usuario: {}", usuario.getUsuario());
					return null;
				}
			} else {
				log.error("No existe el usuario con ID: {}", idusuario);
				throw new NotFoundException("No existe el usuario con ID: " + idusuario);
			}
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e);
			throw new RuntimeException("Ha ocurrido un error inesperado", e);
		} finally {
			log.debug("{} Exit", __method);
		}
	}

	@Transactional
	@Override
	public Correlativo save(Correlativo correlativo) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		Correlativo newCorrelativo = null;

		try {

			if(findByUsuario(correlativo.getUsuario().getIdUsuario()) != null) {
				log.warn("El usuario {} ya cuenta con un correlativo activo", correlativo.getUsuario().getIdUsuario());
				return null;
			} else {
				correlativo.setCorrelativoActual(correlativo.getCorrelativoInicial());
				correlativo.setEstado(estadoService.findById(1));

				newCorrelativo = correlativoRepository.save(correlativo);
				return newCorrelativo;
			}
		} catch(DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e);
			throw new RuntimeException("Ha ocurrido un error inesperado", e);
		}
	}

	@Transactional
	@Override
	public Correlativo update(Correlativo correlativo) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		Correlativo correlativoUpdate = null;

		try {
			log.info("Actualizando correlativo: {}", correlativo.getIdCorrelativo());
			correlativoUpdate = correlativoRepository.save(correlativo);
			return correlativoUpdate;
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e);
			throw new RuntimeException("Ha ocurrido un error inesperado", e);
		}
	}

	@Transactional
	@Override
	public Correlativo anular(Long id) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		Correlativo corrAnulado = null;
		Correlativo correlativo = null;
		Estado estado = null;

		try {
			estado = estadoService.findByEstado("ANULADO");

			if(estado != null) {
				correlativo = findById(id);

				if(correlativo != null) {
					correlativo.setEstado(estado);
					corrAnulado = update(correlativo);
					return corrAnulado;
				} else {
					log.error("Correlativo ID: {}, no se encuentra registrado", id);
					throw new NotFoundException("Correlativo " + id + " no se encuentra registrado");
				}
			} else {
				log.error("Estado \"ANULADO\" no esta disponible");
				throw new NotFoundException("Estado \"ANULADO\" no esta disponible");
			}

		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e);
			throw new RuntimeException("Ha ocurrido un error inesperado", e);
		}
	}

}
