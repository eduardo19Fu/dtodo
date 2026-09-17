package xyz.pangosoft.dtodo.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import xyz.pangosoft.dtodo.dto.ProveedorDto;
import xyz.pangosoft.dtodo.model.Proveedor;
import xyz.pangosoft.dtodo.service.IProveedorService;

@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping(value = "/api")
@RequiredArgsConstructor
@Slf4j
public class ProveedorApiController {

	private final IProveedorService serviceProveedor;

	@Secured(value = { "ROLE_ADMIN" })
	@GetMapping(value = "/proveedores")
	public ResponseEntity<List<Proveedor>> index() {
		log.info("Listando proveedores registrados");
		return ResponseEntity.ok(serviceProveedor.findAll());
	}

	@Secured(value = { "ROLE_ADMIN" })
	@GetMapping(value = "/proveedores/listado")
	public ResponseEntity<Page<ProveedorDto>> listado(
			@RequestParam(value = "page", defaultValue = "0") Integer page,
			@RequestParam(value = "size", defaultValue = "5") Integer size,
			@RequestParam(value = "filtro", defaultValue = "") String filtro,
			@RequestParam(value = "orden", defaultValue = "nombre") String orden,
			@RequestParam(value = "direccion", defaultValue = "asc") String direccion) {
		log.info("Listando proveedores paginados con filtro");
		String propiedad = "id".equalsIgnoreCase(orden) ? "idProveedor" : "nombre";
		Sort.Direction sentido = "desc".equalsIgnoreCase(direccion) ? Sort.Direction.DESC : Sort.Direction.ASC;
		return ResponseEntity.ok(serviceProveedor.findListado(filtro, PageRequest.of(page, size, Sort.by(sentido, propiedad))));
	}

	@Secured(value = { "ROLE_ADMIN" })
	@GetMapping(value = "/proveedores/{id}")
	public ResponseEntity<Proveedor> getById(@PathVariable("id") Integer id) {
		log.info("Buscando proveedor con ID: {}", id);
		return ResponseEntity.ok(serviceProveedor.findById(id));
	}

	@Secured(value = { "ROLE_ADMIN" })
	@PostMapping(value = "/proveedores")
	public ResponseEntity<Proveedor> create(@Valid @RequestBody Proveedor proveedor, BindingResult result) {
		log.info("Registrando proveedor: {}", proveedor.getNombre());
		return new ResponseEntity<>(serviceProveedor.save(proveedor), HttpStatus.CREATED);
	}

	@Secured(value = { "ROLE_ADMIN" })
	@PutMapping(value = "/proveedores")
	public ResponseEntity<Proveedor> update(@Valid @RequestBody Proveedor proveedor, BindingResult result) {
		log.info("Actualizando proveedor: {}", proveedor.getNombre());
		return new ResponseEntity<>(serviceProveedor.save(proveedor), HttpStatus.CREATED);
	}

}
