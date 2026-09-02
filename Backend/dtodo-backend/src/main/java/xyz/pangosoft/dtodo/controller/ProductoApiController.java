package xyz.pangosoft.dtodo.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.pangosoft.dtodo.dto.ProductoDto;
import xyz.pangosoft.dtodo.dto.ProductoDtoMejorado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.InventarioSucursal;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.service.IEstadoService;
import xyz.pangosoft.dtodo.service.IInventarioSucursalService;
import xyz.pangosoft.dtodo.service.IProductoService;
import xyz.pangosoft.dtodo.service.ISucursalService;
import xyz.pangosoft.dtodo.service.IUploadFileService;

@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" },
		exposedHeaders = { HttpHeaders.CONTENT_DISPOSITION })
@RestController
@RequestMapping(value = "/api")
@RequiredArgsConstructor
@Slf4j
public class ProductoApiController {

	private final IProductoService serviceProducto;

	private final IEstadoService serviceEstado;

	private final IUploadFileService serviceUpload;

	private final ISucursalService serviceSucursal;

	private final IInventarioSucursalService serviceInventarioSucursal;

	@GetMapping(value = "/productos")
	public ResponseEntity<List<Producto>> listado(@RequestParam(value = "idSucursal", required = false) Integer idSucursal) {
		log.info("Listando productos registrados");

		List<Producto> productos = new ArrayList<>();
		productos = serviceProducto.findAll(resolverSucursal(idSucursal));
		return ResponseEntity.ok(productos);
	}

	@GetMapping(value = "/productos/dto")
	public ResponseEntity<List<ProductoDto>> getAllDto(@RequestParam(value = "idSucursal", required = false) Integer idSucursal) {
		log.info("Listando productos registrados con dtos");
		return ResponseEntity.ok(serviceProducto.findAllDto(resolverSucursal(idSucursal)));
	}

	@GetMapping(value = "/productos-dto/page/{page}")
	public ResponseEntity<Page<ProductoDtoMejorado>> getPageDto(@PathVariable("page") Integer page,
																@RequestParam(value = "size", defaultValue = "5") Integer size,
																@RequestParam(value = "orden", defaultValue = "nombre") String orden,
																@RequestParam(value = "direccion", defaultValue = "asc") String direccion,
																@RequestParam(value = "idSucursal", required = false) Integer idSucursal)
	{
		return ResponseEntity.ok(serviceProducto.findAllDtoMejorado(
				validarOrden(orden), validarDireccion(direccion), resolverSucursal(idSucursal), PageRequest.of(page, size)));
	}

	@GetMapping(value = "/productos-dto/search/{page}")
	public ResponseEntity<Page<ProductoDtoMejorado>> searchProductosDto(
																@PathVariable("page") Integer page,
																@RequestParam(required = false) String filtro,
																@RequestParam(value = "size", defaultValue = "5") Integer size,
																@RequestParam(value = "orden", defaultValue = "nombre") String orden,
																@RequestParam(value = "direccion", defaultValue = "asc") String direccion,
																@RequestParam(value = "idSucursal", required = false) Integer idSucursal)
	{
		return ResponseEntity.ok(serviceProducto.searchProductoDtoMejorado(
				filtro, validarOrden(orden), validarDireccion(direccion), resolverSucursal(idSucursal), PageRequest.of(page, size)));
	}

	/**
	 * Mientras el frontend no envíe explícitamente la sucursal activa del usuario (ver plan de
	 * implementación, milestone M8), las consultas de productos caen por defecto en la sucursal
	 * principal para no romper las pantallas existentes.
	 */
	private Integer resolverSucursal(Integer idSucursal) {
		if (idSucursal != null) {
			return idSucursal;
		}
		return serviceSucursal.findPrincipal().getIdSucursal();
	}

	private String validarOrden(String orden) {
		switch (orden == null ? "" : orden.toLowerCase()) {
			case "codigo":
				return "codigo";
			case "nombre":
				return "nombre";
			case "preciocompra":
				return "precioCompra";
			case "precioventa":
				return "precioVenta";
			case "stock":
				return "stock";
			case "tipo":
				return "tipo";
			case "marca":
				return "marca";
			case "estado":
				return "estado";
			default:
				return "nombre";
		}
	}

	private String validarDireccion(String direccion) {
		return "desc".equalsIgnoreCase(direccion) ? "desc" : "asc";
	}

	@GetMapping(value = "/productos/page/{page}")
	public ResponseEntity<Page<Producto>> index(@PathVariable("page") Integer page) {
		log.info("Listando productos paginados:");

		Page<Producto> productosPaginados = serviceProducto.findAll(PageRequest.of(page, 5));
		return ResponseEntity.ok(productosPaginados);
	}
	
	@GetMapping(value = "/productos-activos")
	public ResponseEntity<List<ProductoDto>> findAll(@RequestParam(value = "idSucursal", required = false) Integer idSucursal) {
		log.info("Listando productos activos");

		Estado estado = serviceEstado.findById(1);
		List<ProductoDto> productosActivos = serviceProducto.findAllByEstado(estado, resolverSucursal(idSucursal));
		return ResponseEntity.ok(productosActivos);
	}

	@Secured({ "ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO" })
	@GetMapping(value = "/productos/{id}")
	public ResponseEntity<Producto> findById(@PathVariable("id") int idproducto) {
		log.info("Buscando Producto por ID: {}", idproducto);

		Producto producto = serviceProducto.findById(idproducto);
		return ResponseEntity.ok(producto);
	}

	@Secured(value = {"ROLE_ADMIN", "ROLE_INVENTARIO"})
	@GetMapping(value = "/productos/cantidad-productos")
	public ResponseEntity<Integer> getTotalProductos(){
		log.info("Obteniendo total de Productos registrados");

		Integer total = 0;
		total = serviceProducto.totalProductos();
		return ResponseEntity.ok(total);
	}

	@Secured(value = { "ROLE_ADMIN" })
	@PostMapping(value = "/productos")
	public ResponseEntity<Producto> create(@RequestBody Producto producto, BindingResult result,
			@RequestParam(value = "idSucursal", required = false) Integer idSucursal) {
		log.info("Registrando nuevo producto con codigo: {}", producto.getCodProducto());

		int stockInicial = producto.getStock();
		Producto newProducto = serviceProducto.save(producto);

		if (stockInicial > 0) {
			Sucursal sucursal = serviceSucursal.findById(resolverSucursal(idSucursal));
			InventarioSucursal inventario = serviceInventarioSucursal.obtenerOCrear(sucursal, newProducto);
			inventario.setStock(stockInicial);
			serviceInventarioSucursal.guardar(inventario);
		}

		return new ResponseEntity<>(newProducto, HttpStatus.CREATED);
	}

	@Secured(value = { "ROLE_ADMIN" })
	@PutMapping(value = "/productos")
	public ResponseEntity<Producto> update(@RequestBody Producto producto, BindingResult result) {
		log.info("Actualizando producto con ID: {}", producto.getIdProducto());

		Producto productoUpdated = null;
		productoUpdated = serviceProducto.save(producto);
		return new ResponseEntity<>(productoUpdated, HttpStatus.CREATED);
	}

	@Secured(value = { "ROLE_ADMIN" })
	@DeleteMapping(value = "/productos/{id}")
	public ResponseEntity<Map<String, Object>> delete(@PathVariable("id") Integer idproducto) {
		log.info("Eliminando producto con ID: {}", idproducto);

		Producto producto = serviceProducto.findById(idproducto);
		serviceProducto.delete(producto);
		Map<String, Object> response = new HashMap<>();

		response.put("mensaje", "¡El producto ha sido eliminado con éxito!");
		return ResponseEntity.ok(response);
	}

	@Secured(value = { "ROLE_COBRADOR", "ROLE_ADMIN" })
	@PostMapping(value = "/productos/upload")
	public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file, @RequestParam("id") Integer id) {
		log.info("Subiendo imagen para producto ID: {}", id);

		Producto producto = null;
		Map<String, Object> response = new HashMap<>();
		producto = serviceProducto.upload(file, id);

		response.put("mensaje", "Imagen subida con éxito");
		response.put("producto", producto);
		return ResponseEntity.ok(response);

	}

	@GetMapping(value = "/uploads/img/{nombreImagen:.+}")
	public ResponseEntity<Resource> verImagen(@PathVariable String nombreImagen) {
		HttpHeaders cabecera = new HttpHeaders();
		Resource recurso = serviceProducto.cargar(nombreImagen);

		cabecera.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + recurso.getFilename() + "\"");
		return new ResponseEntity<Resource>(recurso, cabecera, HttpStatus.OK);
	}
	
	@GetMapping(value = "/productos/name/{name}")
	public ResponseEntity<List<Producto>> findByName(@PathVariable("name") String name){
		log.info("Buscando listado de productos con nombre: {}", name);

		List<Producto> productos = serviceProducto.findByName(name);
		return ResponseEntity.ok(productos);
	}

	@Secured({ "ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO" })
	@GetMapping(value = "/productos/codigo/{codigo}")
	public ResponseEntity<Producto> findByCodigo(@PathVariable("codigo") String codigo) {
		log.info("Buscando producto por codigo: {}", codigo);

		Producto producto = serviceProducto.findByCodigo(codigo);
		return ResponseEntity.ok(producto);
	}
	
	/*************** PDF REPORTS CONTROLLERS *****************/

	@Secured(value = {"ROLE_ADMIN"})
	@GetMapping(value = "/productos/pdf/inventario")
	public ResponseEntity<byte[]> generarInventarioPDF(@RequestParam String fechaIni, @RequestParam String fechaFin) {
		return null;
	}

	@Secured(value = {"ROLE_ADMIN"})
	@GetMapping(value = "/productos/excel")
	public ResponseEntity<byte[]> generarProductosExcel(@RequestParam(value = "idSucursal", required = false) Integer idSucursal) {
		log.info("Generando reporte Excel de productos");
		byte[] reporte = serviceProducto.productosExcel(resolverSucursal(idSucursal));

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=productos.xlsx")
				.contentType(MediaType.parseMediaType(
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.contentLength(reporte.length)
				.body(reporte);
	}
}
