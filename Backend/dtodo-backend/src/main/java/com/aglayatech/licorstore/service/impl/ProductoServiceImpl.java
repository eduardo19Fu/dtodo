package com.aglayatech.licorstore.service.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import com.aglayatech.licorstore.service.IEstadoService;
import com.aglayatech.licorstore.service.IUploadFileService;
import com.aglayatech.licorstore.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;

import com.aglayatech.licorstore.error.exceptions.NotFoundException;
import com.aglayatech.licorstore.model.Estado;
import com.aglayatech.licorstore.model.Producto;
import com.aglayatech.licorstore.repository.IProductoRepository;
import com.aglayatech.licorstore.service.IProductoService;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductoServiceImpl implements IProductoService {

	private final IProductoRepository repoProducto;

	private final IUploadFileService uploadFileService;

	private final IEstadoService estadoService;

	protected final DataSource localDataSource;

	@Transactional(readOnly = true)
	@Override
	public List<Producto> findAll() {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		List<Producto> productos = new ArrayList<>();

		try {
			productos = repoProducto.listarPorEstadoSP(0);
			log.info("Devolviendo listado de productos disponibles");
			return productos;
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
	public Page<Producto> findAll(Pageable pageable) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		Page<Producto> productosPaginados = null;

		try {
			productosPaginados = repoProducto.findAll(pageable);
			log.info("Devolviendo productos paginados: {}", productosPaginados.getTotalPages());
			return productosPaginados;
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
	public Producto findById(Integer idproducto) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		Optional<Producto> producto = null;

		try {
			producto = repoProducto.findById(idproducto);

			if (producto.isPresent()) {
				log.info("Devolviendo Producto con ID: {}", idproducto);
				return producto.get();
			} else {
				log.warn("No existe un producto registrado con ID: {}", idproducto);
				throw new NotFoundException("El producto con ID: " + idproducto + " no se encuentra registrado");
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
	public List<Producto> findByName(String name) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		List<Producto> productos = new ArrayList<>();

		try {
			productos = repoProducto.findByNombreContaining(name);

			if (!productos.isEmpty()) {
				log.info("Retornando listado de productos con nombre: {}", name);
				return productos;
			} else {
				log.warn("No existen productos registrados con el nombre: {}", name);
				throw new NotFoundException("No existen productos registrados con el nombre " + name);
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
	public Producto findByCodigo(String codigo) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		Optional<Producto> producto;

		try {
			producto = repoProducto.findByCodigo(codigo);

			if(producto.isPresent()) {
				log.info("Devolviendo Producto: {}", producto.get());
				return producto.get();
			} else {
				log.warn("Producto con codigo {}, no se encuentra registrado", codigo);
				throw new NotFoundException("Producto con codigo " + codigo + " no se encuentra registrado en la base de datos");
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
	public List<Producto> findCaducados() {
		return repoProducto.findCaducados(new Date());
	}

	@Override
	public Producto upload(MultipartFile file, Integer idProducto) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		Producto producto = null;
		String nombreArchivo = "";

		try {
			producto = findById(idProducto);

			if (!file.isEmpty()) {
				nombreArchivo = uploadFileService.copiar(file);
			}

			String nombreImagenAnterior = producto.getImagen();

			// Eliminar foto antigua cuando se sube nueva foto
			uploadFileService.eliminar(nombreImagenAnterior);
			producto.setImagen(nombreArchivo);

			return save(producto);
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} catch (IOException e) {
			log.error("Ha ocurrido un error con el acceso a los archivos: {}", e);
			throw new RuntimeException("Ha ocurrido un error respectivo a los archivos");
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e);
			throw new RuntimeException("Ha ocurrido un error inesperado: ", e);
		}
	}

	@Override
	public Resource cargar(String nombreImagen) {
		Resource resource = null;

		try {
			resource = uploadFileService.cargar(nombreImagen);
		} catch (MalformedURLException e) {
			log.error("Hay un error en la url proporcionada para cargar la imagen: {}", e);
			throw new RuntimeException("Hay un error en la url proporcionada para cargar la imagen", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e);
			throw new RuntimeException("Ha ocurrido un error inesperado: ", e);
		}
		return resource;
	}

	@Transactional(readOnly = true)
	@Override
	public List<Producto> findAllByEstado(Estado estado) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		List<Producto> productos = new ArrayList<>();

		try {
			productos = repoProducto.listarPorEstadoSP(estado.getIdEstado());

			log.info("Listando productos con estado Activo");
			return productos;
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e);
			throw new RuntimeException("Ha ocurrido un error inesperado: ", e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Integer totalProductos() {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		try {
			log.info("Obteniendo cantidad de productos registrados");
			return repoProducto.getCantProductos() == null ? 0 : repoProducto.getCantProductos();
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e.getMessage());
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => " + e.getMessage(), e.getCause());
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e);
			throw new RuntimeException("Ha ocurrido un error inesperado: " + e.getMessage());
		} finally {
			log.debug("{} Exit", __method);
		}
	}

	@Transactional
	@Override
	public Producto save(Producto producto) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);
		Producto productoSaved = null;

		try {
			if(producto.getIdProducto() != null) {
				log.info("Actualizando Producto con ID: {}", producto.getIdProducto());
                productoSaved = repoProducto.save(producto);
			} else {
				log.info("Registrando nuevo producto: {}", producto);
				Estado estado = estadoService.findById(1);
				producto.setEstado(estado);
				productoSaved = repoProducto.save(producto);
			}

			return productoSaved;
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e.getMessage());
			throw new RuntimeException("Ha ocurrido un error inesperado: " + e.getMessage());
		}
	}

	@Transactional
	@Override
	public void delete(Producto producto) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		try {
			if(producto != null) {
				String nombreImagenAnterior = producto.getImagen();
				uploadFileService.eliminar(nombreImagenAnterior);
				repoProducto.deleteById(producto.getIdProducto());
			} else {
				log.error("===========> El producto a eliminar no se encuentra registrado o no existe.");
			}
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e);
			throw new RuntimeException("Ha ocurrido un error inesperado: ", e);
		}
	}


	/************** SERVICIOS PARA REPORTES **************/
	@Override
	public byte[] inventarioPDF(String fechaIni, String fechaFin) {
		// TODO: Hay que determinar si es necesario crear un reporte de inventario diferente o si es el mismo que los movimientos de producto
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		Date fecha1;
		Date fecha2;

		try {
			fecha1 = Utils.stringToDate(fechaIni);
			fecha2 = Utils.stringToDate(fechaFin);

			Connection connection = localDataSource.getConnection();
		} catch (ParseException e) {} catch (SQLException e) {}
		return new byte[0];
	}

}
