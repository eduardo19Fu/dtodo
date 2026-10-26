package xyz.pangosoft.dtodo.controller;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.pangosoft.dtodo.dto.ClienteDto;
import xyz.pangosoft.dtodo.dto.RutaDto;
import xyz.pangosoft.dtodo.model.Ruta;
import xyz.pangosoft.dtodo.service.IRutaService;
import xyz.pangosoft.dtodo.util.Utils;

/**
 * Catálogo de rutas de venta. Solo el administrador las crea y modifica; las consultas de apoyo (selectores y
 * cartera) las usan también los roles que atienden clientes.
 */
@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping(value = "/api")
@RequiredArgsConstructor
@Slf4j
public class RutaApiController {

	private final IRutaService serviceRuta;

	@Secured(value = { "ROLE_ADMIN" })
	@GetMapping(value = "/rutas/listado")
	public ResponseEntity<Page<RutaDto>> listado(
			@RequestParam(value = "page", defaultValue = "0") Integer page,
			@RequestParam(value = "size", defaultValue = "5") Integer size,
			@RequestParam(value = "filtro", defaultValue = "") String filtro,
			@RequestParam(value = "orden", defaultValue = "nombre") String orden,
			@RequestParam(value = "direccion", defaultValue = "asc") String direccion) {
		log.info("Listando rutas paginadas con filtro");
		Sort.Direction sentido = "desc".equalsIgnoreCase(direccion) ? Sort.Direction.DESC : Sort.Direction.ASC;
		return ResponseEntity.ok(serviceRuta.findListado(filtro,
				PageRequest.of(page, size, Sort.by(sentido, obtenerPropiedadOrden(orden)))));
	}

	/** Las propiedades con alias de unión ({@code v}, {@code s}, {@code e}) son las de la consulta del listado. */
	private String obtenerPropiedadOrden(String orden) {
		switch (orden == null ? "" : orden.toLowerCase()) {
			case "id": return "idRuta";
			case "zona": return "zona";
			case "dias": return "diasVisita";
			case "vendedor": return "v.primerNombre";
			case "sucursal": return "s.nombre";
			case "estado": return "e.estado";
			default: return "nombre";
		}
	}

	@Secured(value = { "ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO" })
	@GetMapping(value = "/rutas/activas")
	public ResponseEntity<List<RutaDto>> activas() {
		return ResponseEntity.ok(serviceRuta.findActivas());
	}

	/** Rutas activas del vendedor de la sesión; el id sale del token, no de la petición. */
	@Secured(value = { "ROLE_VENDEDOR" })
	@GetMapping(value = "/rutas/mias")
	public ResponseEntity<List<RutaDto>> mias(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(serviceRuta.findActivasDeVendedor(Utils.obtenerIdUsuario(jwt)));
	}

	@Secured(value = { "ROLE_ADMIN" })
	@GetMapping(value = "/rutas/{id}")
	public ResponseEntity<RutaDto> getById(@PathVariable("id") Integer id) {
		log.info("Buscando ruta con ID: {}", id);
		return ResponseEntity.ok(serviceRuta.findDtoById(id));
	}

	@Secured(value = { "ROLE_ADMIN" })
	@GetMapping(value = "/rutas/{id}/clientes")
	public ResponseEntity<List<ClienteDto>> clientes(@PathVariable("id") Integer id) {
		log.info("Listando la cartera de la ruta {}", id);
		return ResponseEntity.ok(serviceRuta.findClientes(id));
	}

	@Secured(value = { "ROLE_ADMIN" })
	@PostMapping(value = "/rutas")
	public ResponseEntity<RutaDto> create(@Valid @RequestBody Ruta ruta) {
		log.info("Registrando ruta: {}", ruta.getNombre());
		RutaDto creada = serviceRuta.save(ruta);
		return ResponseEntity.created(URI.create("/api/rutas/" + creada.getIdRuta())).body(creada);
	}

	@Secured(value = { "ROLE_ADMIN" })
	@PutMapping(value = "/rutas/{id}")
	public ResponseEntity<RutaDto> update(@PathVariable("id") Integer id, @Valid @RequestBody Ruta ruta) {
		log.info("Actualizando ruta con ID: {}", id);
		ruta.setIdRuta(id);
		return ResponseEntity.ok(serviceRuta.save(ruta));
	}
}
