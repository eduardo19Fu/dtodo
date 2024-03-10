package com.aglayatech.licorstore.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.aglayatech.licorstore.error.exceptions.BadRequestException;
import com.aglayatech.licorstore.error.exceptions.NoContentException;
import com.aglayatech.licorstore.error.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import com.aglayatech.licorstore.model.TipoProducto;
import com.aglayatech.licorstore.repository.ITipoProductoRepository;
import com.aglayatech.licorstore.service.ITipoProductoService;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TipoProductoServiceImpl implements ITipoProductoService {

	private final ITipoProductoRepository repoTipo;

	@Transactional(readOnly = true)
	@Override
	public List<TipoProducto> findAll() {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		List<TipoProducto> tipos = new ArrayList<>();

		try {
			tipos = repoTipo.findAll(Sort.by(Direction.ASC, "tipoProducto"));

			if(!tipos.isEmpty()) {
				log.info("Buscando todos las categorías de productos");
				return tipos;
			} else {
				log.warn("No existen categorías registradas");
				throw new NoContentException("No existen categorias registradas");
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

	@Transactional(readOnly = true)
	@Override
	public Page<TipoProducto> findAll(Pageable pageable) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		Page<TipoProducto> tipos = null;

		try {
			tipos = repoTipo.findAll(pageable);

			if(tipos.hasContent()) {
				log.info("Devolviendo categorias de productos paginados: {}", tipos.getTotalPages());
				return tipos;
			} else {
				log.warn("No existen categorías registradas");
				throw new NoContentException("No existen categorias registradas");
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

	@Transactional(readOnly = true)
	@Override
	public List<TipoProducto> findByTipo(String tipo) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		List<TipoProducto> tipos = new ArrayList<>();

		try {
			tipos = repoTipo.findByTipoProducto(tipo);

			if(!tipos.isEmpty()) {
				log.info("Listando Categorías por nombre: {}", tipo);
				return tipos;
			} else {
				log.warn("No existen categorías registradas con el nombre: {}", tipo);
				throw new NoContentException("No existen categorias registradas con el nombre: ".concat(tipo));
			}
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} finally {
			log.debug("{} Exit", __method);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public TipoProducto findById(Integer id) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		Optional<TipoProducto> tipoProducto = null;

		try {
			tipoProducto = repoTipo.findById(id);

			if(tipoProducto.isPresent()) {
				log.info("Se encontro categoría de producto con id: {}", id);
				return tipoProducto.get();
			} else {
				log.error("Categoría de producto {}, no se encuentra registrada", id);
				throw new NotFoundException("Categoría de Producto con ID: " + id + " no se encuentra registrada");
			}
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} finally {
			log.debug("{} Exit", __method);
		}
	}

	@Transactional
	@Override
	public TipoProducto save(TipoProducto tipo) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		TipoProducto saveTipo = null;

		try {
			if(tipo.getIdTipoProducto() != null) {
				log.info("Actualizando Categoría de Producto: {}", tipo.getTipoProducto());

				TipoProducto oldTipo = new TipoProducto();
				oldTipo.setIdTipoProducto(tipo.getIdTipoProducto());
				oldTipo.setTipoProducto(tipo.getTipoProducto());
				oldTipo.setUsuario(tipo.getUsuario());
				oldTipo.setFechaRegistro(tipo.getFechaRegistro());

				saveTipo = repoTipo.save(oldTipo);
			} else {
				log.info("Registrando nueva categoria de producto: {}", tipo.getTipoProducto());
				saveTipo = repoTipo.save(tipo);
			}
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} finally {
			log.debug("{} Exit", __method);
			return saveTipo;
		}
	}

	@Transactional
	@Override
	public void delete(TipoProducto tipo) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		try {
			repoTipo.deleteById(tipo.getIdTipoProducto());
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} finally {
			log.debug("{} Exit", __method);
		}
	}

}
