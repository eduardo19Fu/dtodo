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
import org.springframework.security.core.Authentication;
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

import xyz.pangosoft.dtodo.dto.DespachoBodegaDto;
import xyz.pangosoft.dtodo.dto.DespachoBodegaRequest;
import xyz.pangosoft.dtodo.model.DespachoBodega;
import xyz.pangosoft.dtodo.service.IBodegaReporteService;
import xyz.pangosoft.dtodo.service.IDespachoBodegaService;
import xyz.pangosoft.dtodo.util.Utils;

@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping(value = "/api")
@RequiredArgsConstructor
@Slf4j
public class DespachoBodegaApiController {

	private final IDespachoBodegaService serviceDespacho;

	private final IBodegaReporteService serviceReporte;

	@Secured(value = { "ROLE_ADMIN", "ROLE_BODEGA" })
	@GetMapping(value = "/despachos-bodega/listado")
	public ResponseEntity<Page<DespachoBodegaDto>> listado(
			@RequestParam(value = "page", defaultValue = "0") Integer page,
			@RequestParam(value = "size", defaultValue = "5") Integer size,
			@RequestParam(value = "filtro", defaultValue = "") String filtro,
			@RequestParam(value = "idBodega", required = false) Integer idBodega,
			@RequestParam(value = "estado", defaultValue = "") String estado) {
		log.info("Listando despachos de bodega paginados con filtro");
		return ResponseEntity.ok(serviceDespacho.findListado(filtro, idBodega, estado,
				PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaRegistro", "idDespacho"))));
	}

	@Secured(value = { "ROLE_ADMIN", "ROLE_BODEGA" })
	@GetMapping(value = "/despachos-bodega/{id}")
	public ResponseEntity<DespachoBodega> getById(@PathVariable("id") Long id) {
		log.info("Buscando despacho de bodega con ID: {}", id);
		return ResponseEntity.ok(serviceDespacho.findById(id));
	}

	@Secured(value = { "ROLE_ADMIN", "ROLE_BODEGA" })
	@PostMapping(value = "/despachos-bodega")
	public ResponseEntity<DespachoBodega> create(@RequestBody DespachoBodegaRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		log.info("********** Registrando despacho de bodega **********");
		return new ResponseEntity<>(serviceDespacho.crear(request, Utils.obtenerIdUsuario(jwt)), HttpStatus.CREATED);
	}

	@Secured(value = { "ROLE_ADMIN" })
	@PutMapping(value = "/despachos-bodega/{id}/aprobar")
	public ResponseEntity<DespachoBodega> aprobar(@PathVariable("id") Long id, @AuthenticationPrincipal Jwt jwt) {
		log.info("Aprobando despacho de bodega: {}", id);
		return ResponseEntity.ok(serviceDespacho.aprobar(id, Utils.obtenerIdUsuario(jwt)));
	}

	@Secured(value = { "ROLE_ADMIN", "ROLE_BODEGA" })
	@PutMapping(value = "/despachos-bodega/{id}/cancelar")
	public ResponseEntity<DespachoBodega> cancelar(@PathVariable("id") Long id,
			@RequestBody Map<String, String> body, @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
		log.info("Cancelando despacho de bodega: {}", id);
		boolean esAdministrador = authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
		return ResponseEntity.ok(serviceDespacho.cancelar(
				id, body.get("motivo"), Utils.obtenerIdUsuario(jwt), esAdministrador));
	}

	@Secured(value = { "ROLE_ADMIN", "ROLE_BODEGA" })
	@GetMapping(value = "/despachos-bodega/{id}/comprobante")
	public ResponseEntity<byte[]> comprobante(@PathVariable("id") Long id) {
		log.info("Generando comprobante del despacho de bodega: {}", id);
		byte[] pdf = serviceReporte.generarComprobanteDespacho(id);
		String disposition = "inline; filename=\"despacho_bodega_" + id + ".pdf\"";
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						new String(disposition.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1))
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.contentLength(pdf.length)
				.body(pdf);
	}

}
