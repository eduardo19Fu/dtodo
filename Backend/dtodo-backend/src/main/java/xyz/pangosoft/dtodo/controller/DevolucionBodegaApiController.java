package xyz.pangosoft.dtodo.controller;

import java.nio.charset.StandardCharsets;
import java.util.Map;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import xyz.pangosoft.dtodo.dto.DevolucionBodegaDto;
import xyz.pangosoft.dtodo.dto.DevolucionBodegaRequest;
import xyz.pangosoft.dtodo.dto.InventarioSucursalDto;
import xyz.pangosoft.dtodo.model.DevolucionBodega;
import xyz.pangosoft.dtodo.service.IBodegaReporteService;
import xyz.pangosoft.dtodo.service.IDevolucionBodegaService;
import xyz.pangosoft.dtodo.util.Utils;

@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping(value = "/api")
@RequiredArgsConstructor
@Slf4j
public class DevolucionBodegaApiController {

	private final IDevolucionBodegaService serviceDevolucion;

	private final IBodegaReporteService serviceReporte;

	@Secured(value = { "ROLE_ADMIN", "ROLE_BODEGA" })
	@GetMapping(value = "/devoluciones-bodega/listado")
	public ResponseEntity<Page<DevolucionBodegaDto>> listado(
			@RequestParam(value = "page", defaultValue = "0") Integer page,
			@RequestParam(value = "size", defaultValue = "5") Integer size,
			@RequestParam(value = "filtro", defaultValue = "") String filtro,
			@RequestParam(value = "idBodega", required = false) Integer idBodega,
			@RequestParam(value = "estado", defaultValue = "") String estado) {
		log.info("Listando devoluciones a bodega paginadas con filtro");
		return ResponseEntity.ok(serviceDevolucion.findListado(filtro, idBodega, estado,
				PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaRegistro", "idDevolucion"))));
	}

	/** Productos de la sucursal que devuelve, con su existencia; no expone costos. */
	@Secured(value = { "ROLE_ADMIN", "ROLE_BODEGA" })
	@GetMapping(value = "/devoluciones-bodega/sucursales/{idSucursal}/productos")
	public ResponseEntity<Page<InventarioSucursalDto>> productosSucursal(
			@PathVariable("idSucursal") Integer idSucursal,
			@RequestParam(value = "page", defaultValue = "0") Integer page,
			@RequestParam(value = "size", defaultValue = "5") Integer size,
			@RequestParam(value = "filtro", defaultValue = "") String filtro) {
		log.info("Listando productos de la sucursal {} para una devolución", idSucursal);
		// El Sort se aplica contra la entidad InventarioSucursal (alias "i" en el @Query)
		return ResponseEntity.ok(serviceDevolucion.findProductosSucursal(idSucursal, filtro,
				PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "producto.nombre"))));
	}

	@Secured(value = { "ROLE_ADMIN", "ROLE_BODEGA" })
	@GetMapping(value = "/devoluciones-bodega/{id}")
	public ResponseEntity<DevolucionBodega> getById(@PathVariable("id") Long id) {
		log.info("Buscando devolución a bodega con ID: {}", id);
		return ResponseEntity.ok(serviceDevolucion.findById(id));
	}

	@Secured(value = { "ROLE_ADMIN", "ROLE_BODEGA" })
	@PostMapping(value = "/devoluciones-bodega")
	public ResponseEntity<DevolucionBodega> create(@RequestBody DevolucionBodegaRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		log.info("********** Registrando devolución a bodega **********");
		return new ResponseEntity<>(serviceDevolucion.crear(request, Utils.obtenerIdUsuario(jwt)), HttpStatus.CREATED);
	}

	@Secured(value = { "ROLE_ADMIN" })
	@PutMapping(value = "/devoluciones-bodega/{id}/aprobar")
	public ResponseEntity<DevolucionBodega> aprobar(@PathVariable("id") Long id, @AuthenticationPrincipal Jwt jwt) {
		log.info("Aprobando devolución a bodega: {}", id);
		return ResponseEntity.ok(serviceDevolucion.aprobar(id, Utils.obtenerIdUsuario(jwt)));
	}

	@Secured(value = { "ROLE_ADMIN", "ROLE_BODEGA" })
	@PutMapping(value = "/devoluciones-bodega/{id}/cancelar")
	public ResponseEntity<DevolucionBodega> cancelar(@PathVariable("id") Long id,
			@RequestBody Map<String, String> body, @AuthenticationPrincipal Jwt jwt) {
		log.info("Rechazando devolución a bodega: {}", id);
		return ResponseEntity.ok(serviceDevolucion.cancelar(id, body.get("motivo"), Utils.obtenerIdUsuario(jwt)));
	}

	@Secured(value = { "ROLE_ADMIN", "ROLE_BODEGA" })
	@GetMapping(value = "/devoluciones-bodega/{id}/comprobante")
	public ResponseEntity<byte[]> comprobante(@PathVariable("id") Long id) {
		log.info("Generando comprobante de la devolución a bodega: {}", id);
		byte[] pdf = serviceReporte.generarComprobanteDevolucion(id);
		String disposition = "inline; filename=\"devolucion_bodega_" + id + ".pdf\"";
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						new String(disposition.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1))
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.contentLength(pdf.length)
				.body(pdf);
	}

}
