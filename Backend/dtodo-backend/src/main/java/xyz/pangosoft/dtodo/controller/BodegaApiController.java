package xyz.pangosoft.dtodo.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

import xyz.pangosoft.dtodo.dto.BodegaDto;
import xyz.pangosoft.dtodo.dto.ImportacionInventarioDto;
import xyz.pangosoft.dtodo.dto.InventarioBodegaDto;
import xyz.pangosoft.dtodo.dto.MovimientoBodegaDto;
import xyz.pangosoft.dtodo.dto.MovimientoBodegaRequest;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.model.Bodega;
import xyz.pangosoft.dtodo.model.MovimientoBodega;
import xyz.pangosoft.dtodo.model.enums.OrigenInventarioBodegaEnum;
import xyz.pangosoft.dtodo.model.enums.TipoMovimientoBodegaEnum;
import xyz.pangosoft.dtodo.service.IBodegaService;
import xyz.pangosoft.dtodo.service.IInventarioBodegaService;
import xyz.pangosoft.dtodo.util.InventarioBodegaExcel;
import xyz.pangosoft.dtodo.util.Utils;

@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping(value = "/api")
@RequiredArgsConstructor
@Slf4j
public class BodegaApiController {

	private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private final IBodegaService serviceBodega;

	private final IInventarioBodegaService serviceInventarioBodega;

	@Secured(value = { "ROLE_ADMIN", "ROLE_BODEGA" })
	@GetMapping(value = "/bodegas")
	public ResponseEntity<List<Bodega>> index(
			@RequestParam(value = "soloActivas", defaultValue = "false") boolean soloActivas) {
		log.info("Listando bodegas registradas");
		return ResponseEntity.ok(serviceBodega.findAll(soloActivas));
	}

	@Secured(value = { "ROLE_ADMIN", "ROLE_BODEGA" })
	@GetMapping(value = "/bodegas/listado")
	public ResponseEntity<Page<BodegaDto>> listado(
			@RequestParam(value = "page", defaultValue = "0") Integer page,
			@RequestParam(value = "size", defaultValue = "5") Integer size,
			@RequestParam(value = "filtro", defaultValue = "") String filtro,
			@RequestParam(value = "orden", defaultValue = "nombre") String orden,
			@RequestParam(value = "direccion", defaultValue = "asc") String direccion) {
		log.info("Listando bodegas paginadas con filtro");
		Sort.Direction sentido = "desc".equalsIgnoreCase(direccion) ? Sort.Direction.DESC : Sort.Direction.ASC;
		return ResponseEntity.ok(serviceBodega.findListado(
				filtro, PageRequest.of(page, size, Sort.by(sentido, obtenerPropiedadOrden(orden)))));
	}

	private String obtenerPropiedadOrden(String orden) {
		if ("id".equalsIgnoreCase(orden)) {
			return "idBodega";
		}
		if ("ubicacion".equalsIgnoreCase(orden)) {
			return "ubicacion";
		}
		if ("fecha".equalsIgnoreCase(orden)) {
			return "fechaRegistro";
		}
		return "nombre";
	}

	@Secured(value = { "ROLE_ADMIN", "ROLE_BODEGA" })
	@GetMapping(value = "/bodegas/{id}")
	public ResponseEntity<Bodega> getById(@PathVariable("id") Integer id) {
		log.info("Buscando bodega con ID: {}", id);
		return ResponseEntity.ok(serviceBodega.findById(id));
	}

	@Secured(value = { "ROLE_ADMIN", "ROLE_BODEGA" })
	@PostMapping(value = "/bodegas")
	public ResponseEntity<Bodega> create(@Valid @RequestBody Bodega bodega, @AuthenticationPrincipal Jwt jwt) {
		log.info("Registrando bodega: {}", bodega.getNombre());
		return new ResponseEntity<>(serviceBodega.save(bodega, Utils.obtenerIdUsuario(jwt)), HttpStatus.CREATED);
	}

	@Secured(value = { "ROLE_ADMIN", "ROLE_BODEGA" })
	@PutMapping(value = "/bodegas")
	public ResponseEntity<Bodega> update(@Valid @RequestBody Bodega bodega, @AuthenticationPrincipal Jwt jwt) {
		log.info("Actualizando bodega: {}", bodega.getNombre());
		return ResponseEntity.ok(serviceBodega.save(bodega, Utils.obtenerIdUsuario(jwt)));
	}

	/*********** INVENTARIO ***********/

	@Secured(value = { "ROLE_ADMIN", "ROLE_BODEGA" })
	@GetMapping(value = "/bodegas/{id}/inventario")
	public ResponseEntity<Page<InventarioBodegaDto>> inventario(
			@PathVariable("id") Integer idBodega,
			@RequestParam(value = "page", defaultValue = "0") Integer page,
			@RequestParam(value = "size", defaultValue = "10") Integer size,
			@RequestParam(value = "filtro", defaultValue = "") String filtro) {
		log.info("Listando inventario de la bodega: {}", idBodega);
		// El Sort se aplica contra la entidad InventarioBodega (alias "i" en el @Query): se usa la ruta de asociación real.
		return ResponseEntity.ok(serviceInventarioBodega.findListado(
				idBodega, filtro, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "producto.nombre"))));
	}

	@Secured(value = { "ROLE_ADMIN", "ROLE_BODEGA" })
	@GetMapping(value = "/bodegas/{id}/inventario/codigo/{codigo}")
	public ResponseEntity<InventarioBodegaDto> inventarioPorCodigo(
			@PathVariable("id") Integer idBodega, @PathVariable("codigo") String codigo) {
		return ResponseEntity.ok(serviceInventarioBodega.findPorCodigo(idBodega, codigo));
	}

	@Secured(value = { "ROLE_ADMIN" })
	@DeleteMapping(value = "/bodegas/{id}/inventario/{idProducto}")
	public ResponseEntity<Map<String, Object>> eliminarProducto(
			@PathVariable("id") Integer idBodega,
			@PathVariable("idProducto") Integer idProducto,
			@RequestParam(value = "motivo", defaultValue = "") String motivo,
			@AuthenticationPrincipal Jwt jwt) {
		log.info("Eliminando el producto {} de la bodega {}", idProducto, idBodega);
		MovimientoBodega movimiento = serviceInventarioBodega.eliminarProducto(
				idBodega, idProducto, motivo, Utils.obtenerIdUsuario(jwt));
		return ResponseEntity.ok(respuestaMovimiento("Producto eliminado de la bodega", movimiento));
	}

	@Secured(value = { "ROLE_ADMIN", "ROLE_BODEGA" })
	@PostMapping(value = "/bodegas/{id}/movimientos/agregar")
	public ResponseEntity<Map<String, Object>> agregarProducto(
			@PathVariable("id") Integer idBodega,
			@RequestBody MovimientoBodegaRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		log.info("Agregando producto {} a la bodega {}", request.getIdProducto(), idBodega);
		MovimientoBodega movimiento = serviceInventarioBodega.agregarProducto(
				idBodega, request, Utils.obtenerIdUsuario(jwt));
		return new ResponseEntity<>(respuestaMovimiento("Producto agregado a la bodega", movimiento), HttpStatus.CREATED);
	}

	@Secured(value = { "ROLE_ADMIN" })
	@PostMapping(value = "/bodegas/{id}/movimientos/reducir")
	public ResponseEntity<Map<String, Object>> reducirExistencias(
			@PathVariable("id") Integer idBodega,
			@RequestBody MovimientoBodegaRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		log.info("Reduciendo existencias del producto {} en la bodega {}", request.getIdProducto(), idBodega);
		MovimientoBodega movimiento = serviceInventarioBodega.reducirExistencias(
				idBodega, request, Utils.obtenerIdUsuario(jwt));
		return new ResponseEntity<>(respuestaMovimiento("Existencias reducidas", movimiento), HttpStatus.CREATED);
	}

	@Secured(value = { "ROLE_ADMIN", "ROLE_BODEGA" })
	@GetMapping(value = "/bodegas/{id}/movimientos")
	public ResponseEntity<Page<MovimientoBodegaDto>> movimientos(
			@PathVariable("id") Integer idBodega,
			@RequestParam(value = "page", defaultValue = "0") Integer page,
			@RequestParam(value = "size", defaultValue = "10") Integer size,
			@RequestParam(value = "fechaIni", defaultValue = "") String fechaIni,
			@RequestParam(value = "fechaFin", defaultValue = "") String fechaFin,
			@RequestParam(value = "tipo", required = false) TipoMovimientoBodegaEnum tipo,
			@RequestParam(value = "filtro", defaultValue = "") String filtro) {
		log.info("Listando movimientos de la bodega: {}", idBodega);
		return ResponseEntity.ok(serviceInventarioBodega.findMovimientos(idBodega, fechaIni, fechaFin, tipo, filtro,
				PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaMovimiento", "idMovimiento"))));
	}

	/*********** IMPORTACIÓN DE INVENTARIO ***********/

	@Secured(value = { "ROLE_ADMIN", "ROLE_BODEGA" })
	@PostMapping(value = "/bodegas/{id}/clonar-inventario")
	public ResponseEntity<Map<String, Object>> clonarInventario(
			@PathVariable("id") Integer idBodegaDestino,
			@RequestParam("origen") OrigenInventarioBodegaEnum origen,
			@RequestParam("idOrigen") Integer idOrigen,
			@AuthenticationPrincipal Jwt jwt) {
		log.info("Copiando inventario de {} {} hacia la bodega {}", origen, idOrigen, idBodegaDestino);
		int copiados = serviceInventarioBodega.clonarInventario(
				idBodegaDestino, origen, idOrigen, Utils.obtenerIdUsuario(jwt));

		Map<String, Object> response = new HashMap<>();
		response.put("mensaje", "Inventario copiado con éxito");
		response.put("idBodega", idBodegaDestino);
		response.put("productosCopiados", copiados);
		return ResponseEntity.ok(response);
	}

	@Secured(value = { "ROLE_ADMIN", "ROLE_BODEGA" })
	@PostMapping(value = "/bodegas/{id}/importar-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ImportacionInventarioDto> importarExcel(
			@PathVariable("id") Integer idBodega,
			@RequestParam("archivo") MultipartFile archivo,
			@AuthenticationPrincipal Jwt jwt) throws IOException {
		log.info("Importando inventario desde Excel hacia la bodega {}", idBodega);
		String nombre = archivo.getOriginalFilename();
		if (archivo.isEmpty()) {
			throw new BadRequestException("Debe seleccionar un archivo Excel con el inventario.", null);
		}
		if (nombre == null || !nombre.toLowerCase().endsWith(".xlsx")) {
			throw new BadRequestException("El archivo debe ser un Excel con formato .xlsx.", null);
		}
		try (java.io.InputStream contenido = archivo.getInputStream()) {
			return ResponseEntity.ok(serviceInventarioBodega.importarDesdeExcel(
					idBodega, contenido, Utils.obtenerIdUsuario(jwt)));
		}
	}

	@Secured(value = { "ROLE_ADMIN", "ROLE_BODEGA" })
	@GetMapping(value = "/bodegas/plantilla-importacion")
	public ResponseEntity<byte[]> plantillaImportacion() {
		byte[] plantilla = InventarioBodegaExcel.generarPlantilla();
		return ResponseEntity.ok()
				.contentType(XLSX_MEDIA_TYPE)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plantilla_inventario_bodega.xlsx\"")
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.contentLength(plantilla.length)
				.body(plantilla);
	}

	private Map<String, Object> respuestaMovimiento(String mensaje, MovimientoBodega movimiento) {
		Map<String, Object> response = new HashMap<>();
		response.put("mensaje", mensaje);
		response.put("idMovimiento", movimiento.getIdMovimiento());
		response.put("stockInicial", movimiento.getStockInicial());
		response.put("stockFinal", movimiento.getStockFinal());
		return response;
	}

}
