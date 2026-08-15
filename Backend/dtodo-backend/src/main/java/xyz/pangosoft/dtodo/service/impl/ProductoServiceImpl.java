package xyz.pangosoft.dtodo.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import xyz.pangosoft.dtodo.dto.ProductoDto;
import xyz.pangosoft.dtodo.dto.ProductoDtoMejorado;
import xyz.pangosoft.dtodo.error.exceptions.NoContentException;
import xyz.pangosoft.dtodo.error.exceptions.ReportGenerationException;
import xyz.pangosoft.dtodo.service.IEstadoService;
import xyz.pangosoft.dtodo.service.IUploadFileService;
import xyz.pangosoft.dtodo.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.repository.IProductoRepository;
import xyz.pangosoft.dtodo.service.IProductoService;

import org.springframework.web.multipart.MultipartFile;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;

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
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e.getMessage());
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
	public List<ProductoDto> findAllDto() {
		List<ProductoDto> productos = new ArrayList<>();
		try {
			log.info("Devolviendo productos con dtos");
			productos = repoProducto.listarPorEstadoSPDto(0);
			return productos;
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos al consultar productos desde procedimiento almacenado: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Page<ProductoDtoMejorado> findAllDtoMejorado(String orden, String direccion, Pageable pageable) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		try {
			log.debug("Consultando productos desde la base de datos...");
			Page<Object[]> results = repoProducto.findAllProductosDto(orden, direccion, pageable);

			if (results.isEmpty()) {
				log.warn("No existen productos registrados");
				throw new NoContentException("No existen productos registrados");
			}

			return mapPageToProductoDtoMejorado(results);
		} catch (DataAccessException dax) {
			log.error("Ha ocurrido un error al intentar consultar los productos: {}", dax.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error al consultar los productos => ", dax);
		} finally {
			log.debug("{} Exit", __method);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Page<ProductoDtoMejorado> searchProductoDtoMejorado(
			String filtro, String orden, String direccion, Pageable pageable) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		try {
			log.debug("Consultando productos desde la base de datos que conicidan con la busqueda...");
			String filtroAdaptado = filtro == null ? "" : filtro.trim().replaceAll("\\s+", "%");
			Page<Object[]> results = repoProducto.searchProductosDto(filtroAdaptado, orden, direccion, pageable);

			return mapPageToProductoDtoMejorado(results);
		} catch (DataAccessException dax) {
			log.error("Ha ocurrido un error al intentar consultar los productos con busqueda {}: {}", filtro, dax.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error al consultar los productos => ", dax);
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
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => " + e.getMessage(), e.getCause());
		} catch (Exception e) {
			log.error("Ha ocurrido un error inesperado: {}", e.getMessage());
			throw new RuntimeException("Ha ocurrido un error inesperado: " + e.getMessage(), e.getCause());
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
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
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
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
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
	public List<ProductoDto> findAllByEstado(Estado estado) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		List<ProductoDto> productos = new ArrayList<>();

		try {
			productos = repoProducto.listarPorEstadoSPDto(estado.getIdEstado());

			log.info("Listando productos con estado Activo");
			return productos;
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
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
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => " + e.getMessage(), e.getCause());
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
		producto.setNombre(producto.getNombre().trim().toUpperCase());

		if (producto.getFechaRegistro() == null) {
			producto.setFechaRegistro(LocalDateTime.now());
		}

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
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
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
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
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

	@Transactional(readOnly = true)
	@Override
	public byte[] productosExcel() {
		try (Connection connection = localDataSource.getConnection();
			 InputStream template = getClass().getResourceAsStream("/reports/productos_excel.jrxml");
			 ByteArrayOutputStream output = new ByteArrayOutputStream()) {

			if (template == null) {
				throw new ReportGenerationException("No se encontró la plantilla del reporte de productos", null);
			}

			Map<String, Object> parameters = new HashMap<>();
			parameters.put(JRParameter.IS_IGNORE_PAGINATION, true);

			JasperReport report = JasperCompileManager.compileReport(template);
			JasperPrint print = JasperFillManager.fillReport(report, parameters, connection);

			JRXlsxExporter exporter = new JRXlsxExporter();
			exporter.setExporterInput(new SimpleExporterInput(print));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(output));

			SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
			configuration.setDetectCellType(true);
			configuration.setRemoveEmptySpaceBetweenRows(true);
			configuration.setWhitePageBackground(false);
			configuration.setOnePagePerSheet(false);
			configuration.setSheetNames(new String[] { "Productos" });
			exporter.setConfiguration(configuration);
			exporter.exportReport();

			return output.toByteArray();
		} catch (JRException e) {
			log.error("No fue posible generar el reporte Excel de productos: {}", e.getMessage());
			throw new ReportGenerationException("No fue posible generar el reporte Excel de productos", e);
		} catch (SQLException e) {
			log.error("No fue posible consultar los productos para el reporte: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.SQLException(
					"No fue posible consultar los productos para el reporte", e);
		} catch (IOException e) {
			log.error("No fue posible leer la plantilla del reporte de productos: {}", e.getMessage());
			throw new ReportGenerationException("No fue posible leer la plantilla del reporte de productos", e);
		}
	}

	protected Page<ProductoDtoMejorado> mapPageToProductoDtoMejorado(Page<Object[]> results) {
		return results.map(result -> ProductoDtoMejorado.builder()
				.idProducto((int) result[0])
				.codProducto((String) result[1])
				.nombre((String) result[2])
				.precioCompra((BigDecimal) result[3])
				.precioVenta((BigDecimal) result[4])
				.porcentajeGanancia((float) result[5])
				.descripcion((String) result[6])
				.fechaVencimiento(result[7] == null ? null : ((java.sql.Date) result[7]).toLocalDate())
				.fechaIngreso(result[8] == null ? null : ((java.sql.Date) result[8]).toLocalDate())
				.fechaRegistro(result[9] == null ? null : ((java.sql.Timestamp) result[9]).toLocalDateTime())
				.stock((int) result[10])
				.imagen((String) result[11])
				.idestado((int) result[12])
				.marcaProducto((String) result[13])
				.tipoProducto((String) result[14])
				.estado((String) result[15])
				.build());
	}

}
