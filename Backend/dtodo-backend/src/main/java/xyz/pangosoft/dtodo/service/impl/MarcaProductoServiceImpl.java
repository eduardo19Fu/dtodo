package xyz.pangosoft.dtodo.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import xyz.pangosoft.dtodo.error.exceptions.NoContentException;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.dto.MarcaProductoDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import xyz.pangosoft.dtodo.model.MarcaProducto;
import xyz.pangosoft.dtodo.repository.IMarcaProductoRepository;
import xyz.pangosoft.dtodo.service.IMarcaProductoService;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarcaProductoServiceImpl implements IMarcaProductoService {

	private final IMarcaProductoRepository marcaProductoRepo;

	@Transactional(readOnly = true)
	@Override
	public List<MarcaProducto> findAll() {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		List<MarcaProducto> listadoMarcas = new ArrayList<>();

		try {
			log.info("Consultando marcas de producto registradas: ");
			listadoMarcas = marcaProductoRepo.findAll(Sort.by(Direction.ASC, "marca"));

			if(!listadoMarcas.isEmpty()) {
				log.info("Marcas encontradas: {}", listadoMarcas);
				return listadoMarcas;
			} else {
				log.warn("No existen marcas de producto registradas");
				throw new NoContentException("No existen marcas registradas");
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
	public Page<MarcaProducto> findAll(Pageable pageable) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);
		Page<MarcaProducto> marcasPaginadas = null;

		try {
			marcasPaginadas = marcaProductoRepo.findAll(pageable);

			if(!marcasPaginadas.isEmpty()) {
				log.info("Devolviendo marcas de producto paginadas: {}", marcasPaginadas.getTotalPages());
				return marcasPaginadas;
			} else {
				log.warn("No existen marcas registradas");
				throw new NoContentException("No existen marcas de producto registradas");
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
	public Page<MarcaProductoDto> findListado(String filtro, Pageable pageable) {
		try {
			return marcaProductoRepo.findListado(filtro == null ? "" : filtro.trim(), pageable);
		} catch (DataAccessException e) {
			log.error("Error al consultar el listado paginado de marcas: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
					"Ha ocurrido un error al consultar las marcas", e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public MarcaProducto findById(Integer idMarca) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		Optional<MarcaProducto> marcaProducto = null;

		try {
			marcaProducto = marcaProductoRepo.findById(idMarca);

			if(marcaProducto.isPresent()) {
				log.info("Obteniendo marca de producto: {}", marcaProducto.get());
				return marcaProducto.get();
			} else {
				log.warn("La marca de producto: {}, no se encuentra registrada en la base de datos", idMarca);
				throw new NotFoundException("La marca de producto:" + idMarca + ", no se encuentra registrada en la base de datos");
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
	public List<MarcaProducto> findByMarca(String marca) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		List<MarcaProducto> marcasPorNombre = new ArrayList<>();

		try {
			marcasPorNombre = marcaProductoRepo.findByMarca(marca);

			if(!marcasPorNombre.isEmpty()) {
				log.info("Se encontraron coincidencias para la marca: {}", marca);
				return marcasPorNombre;
			} else {
				log.warn("No se encontraron coincidencias para la marca: {}", marca);
				throw new NotFoundException("No se encontraron coincidencias para la marca: " + marca);
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

	@Transactional
	@Override
	public MarcaProducto save(MarcaProducto marca) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		MarcaProducto newMarca = null;

		try {

			if(marca.getIdMarcaProducto() != null) {
				log.info("Actualizando marca de producto: {}", marca.getIdMarcaProducto());
				MarcaProducto oldMarca = new MarcaProducto();
				oldMarca.setIdMarcaProducto(marca.getIdMarcaProducto());
				oldMarca.setMarca(marca.getMarca());
				oldMarca.setFechaRegistro(marca.getFechaRegistro());
				oldMarca.setUsuario(marca.getUsuario());

				newMarca = marcaProductoRepo.save(oldMarca);
			} else {
				newMarca = marcaProductoRepo.save(marca);
			}
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e);
			throw new RuntimeException("Ha ocurrido un error inesperado: ", e);
		}

		return newMarca;
	}

	@Transactional
	@Override
	public void deleteMarca(MarcaProducto marca) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		try {
			marcaProductoRepo.deleteById(marca.getIdMarcaProducto());
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e);
			throw new RuntimeException("Ha ocurrido un error inesperado: ", e);
		}
	}

}
