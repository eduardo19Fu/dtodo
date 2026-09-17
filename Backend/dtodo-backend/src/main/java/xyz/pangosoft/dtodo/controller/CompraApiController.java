package xyz.pangosoft.dtodo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import xyz.pangosoft.dtodo.dto.CompraDto;
import xyz.pangosoft.dtodo.model.Compra;
import xyz.pangosoft.dtodo.service.ICompraService;

@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping(value = "/api")
@RequiredArgsConstructor
@Slf4j
public class CompraApiController {

	private final ICompraService serviceCompra;

	@Secured(value = { "ROLE_ADMIN" })
	@GetMapping(value = "/compras/listado")
	public ResponseEntity<Page<CompraDto>> listado(
			@RequestParam(value = "page", defaultValue = "0") Integer page,
			@RequestParam(value = "size", defaultValue = "5") Integer size,
			@RequestParam(value = "filtro", defaultValue = "") String filtro,
			@RequestParam(value = "idSucursal", required = false) Integer idSucursal) {
		log.info("Listando compras paginadas con filtro");
		return ResponseEntity.ok(serviceCompra.findListado(
				filtro, idSucursal, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaRegistro"))));
	}

	@Secured(value = { "ROLE_ADMIN" })
	@GetMapping(value = "/compras/{id}")
	public ResponseEntity<Compra> getById(@PathVariable("id") Long id) {
		log.info("Buscando compra con ID: {}", id);
		return ResponseEntity.ok(serviceCompra.findById(id));
	}

	@Secured(value = { "ROLE_ADMIN" })
	@PostMapping(value = "/compras")
	public ResponseEntity<Compra> create(@RequestBody Compra compra) {
		log.info("********** Registrando nueva compra **********");
		return new ResponseEntity<>(serviceCompra.crear(compra), HttpStatus.CREATED);
	}

	@Secured(value = { "ROLE_ADMIN" })
	@PutMapping(value = "/compras/anular/{id}/{idusuario}")
	public ResponseEntity<Compra> anular(@PathVariable("id") Long idCompra, @PathVariable("idusuario") Integer idUsuario) {
		log.info("Anulando compra: {}", idCompra);
		return ResponseEntity.ok(serviceCompra.anular(idCompra, idUsuario));
	}

}
