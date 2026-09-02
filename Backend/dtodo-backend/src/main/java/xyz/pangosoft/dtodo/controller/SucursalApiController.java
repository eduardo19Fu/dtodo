package xyz.pangosoft.dtodo.controller;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import xyz.pangosoft.dtodo.dto.SucursalDto;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.service.ISucursalService;

@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping(value = "/api")
@RequiredArgsConstructor
@Slf4j
public class SucursalApiController {

	private final ISucursalService serviceSucursal;

	@Secured(value = {"ROLE_ADMIN"})
	@GetMapping(value = "/sucursales")
	public ResponseEntity<List<Sucursal>> index() {
		log.info("Listando sucursales registradas");

		List<Sucursal> sucursales = new ArrayList<>();
		sucursales = serviceSucursal.findAll();
		return ResponseEntity.ok(sucursales);
	}

	@Secured(value = {"ROLE_ADMIN"})
	@GetMapping(value = "/sucursales/page/{page}")
	public ResponseEntity<Page<Sucursal>> index(@PathVariable("page") Integer page) {
		log.info("Listando sucursales por página: {}", page);

		Page<Sucursal> sucursalesPaginadas = null;
		sucursalesPaginadas = serviceSucursal.findAll(PageRequest.of(page, 5));
		return ResponseEntity.ok(sucursalesPaginadas);
	}

	@Secured(value = {"ROLE_ADMIN"})
	@GetMapping(value = "/sucursales/listado")
	public ResponseEntity<Page<SucursalDto>> listado(
			@RequestParam(value = "page", defaultValue = "0") Integer page,
			@RequestParam(value = "size", defaultValue = "5") Integer size,
			@RequestParam(value = "filtro", defaultValue = "") String filtro,
			@RequestParam(value = "orden", defaultValue = "nombre") String orden,
			@RequestParam(value = "direccion", defaultValue = "asc") String direccion) {
		log.info("Listando sucursales paginadas con filtro");
		String propiedad = obtenerPropiedadOrden(orden);
		Sort.Direction sentido = "desc".equalsIgnoreCase(direccion)
				? Sort.Direction.DESC : Sort.Direction.ASC;
		return ResponseEntity.ok(serviceSucursal.findListado(
				filtro, PageRequest.of(page, size, Sort.by(sentido, propiedad))));
	}

	private String obtenerPropiedadOrden(String orden) {
		if ("id".equalsIgnoreCase(orden)) {
			return "idSucursal";
		}
		if ("creador".equalsIgnoreCase(orden)) {
			return "usuario.usuario";
		}
		if ("direccion".equalsIgnoreCase(orden)) {
			return "direccion";
		}
		return "nombre";
	}

	@Secured(value = {"ROLE_ADMIN"})
	@GetMapping(value = "/sucursales/principal")
	public ResponseEntity<Sucursal> principal() {
		log.info("Consultando la sucursal principal");
		return ResponseEntity.ok(serviceSucursal.findPrincipal());
	}

	@Secured(value = {"ROLE_ADMIN"})
	@GetMapping(value = "/sucursales/{id}")
	public ResponseEntity<Sucursal> getById(@PathVariable("id") Integer id) {
		log.info("Buscando sucursal con ID: {}", id);

		Sucursal sucursal = null;
		sucursal = serviceSucursal.findById(id);
		return ResponseEntity.ok(sucursal);
	}

	@Secured(value = {"ROLE_ADMIN"})
	@PostMapping(value = "/sucursales")
	public ResponseEntity<Sucursal> create(@Valid @RequestBody Sucursal sucursal, BindingResult result) {
		log.info("Registrando sucursal: {}", sucursal.getNombre());

		Sucursal newSucursal = null;
		newSucursal = serviceSucursal.save(sucursal);
		return new ResponseEntity<>(newSucursal, HttpStatus.CREATED);
	}

	@Secured(value = {"ROLE_ADMIN"})
	@PutMapping(value = "/sucursales")
	public ResponseEntity<Sucursal> update(@Valid @RequestBody Sucursal sucursal, BindingResult result) {
		log.info("Actualizando sucursal: {}", sucursal.getNombre());

		Sucursal sucursalUpdated = serviceSucursal.save(sucursal);
		return new ResponseEntity<>(sucursalUpdated, HttpStatus.CREATED);
	}

}
