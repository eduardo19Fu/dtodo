package xyz.pangosoft.dtodo.controller;

import java.util.ArrayList;
import java.util.List;

import xyz.pangosoft.dtodo.error.exceptions.MethodArgumentTypeMismatchException;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import xyz.pangosoft.dtodo.dto.CorrelativoDto;
import xyz.pangosoft.dtodo.model.Correlativo;
import xyz.pangosoft.dtodo.service.ICorrelativoService;

@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping(value = "/api")
@RequiredArgsConstructor
@Slf4j
public class CorrelativoApiController {

	private final ICorrelativoService serviceCorrelativo;

	@Secured(value = {"ROLE_ADMIN"})
	@GetMapping(value = "/correlativos")
	public ResponseEntity<List<Correlativo>> listarCorrelativos() {
		log.info("Listando Correlativos");

		List<Correlativo> listadoCorrelativo = new ArrayList<>();
		listadoCorrelativo = serviceCorrelativo.findAll();
		return ResponseEntity.ok(listadoCorrelativo);
	}

	@GetMapping(value = "/correlativos/page/{page}")
	public ResponseEntity<Page<Correlativo>> index(@PathVariable("page") Integer page) {
		log.info("Buscando correlativos pagina: {}", page);

		Page<Correlativo> correlativos = serviceCorrelativo.findAll(PageRequest.of(page, 5));
		return ResponseEntity.ok(correlativos);
	}

	@Secured(value = {"ROLE_ADMIN"})
	@GetMapping(value = "/correlativos/listado")
	public ResponseEntity<Page<CorrelativoDto>> listado(
			@RequestParam(value = "page", defaultValue = "0") Integer page,
			@RequestParam(value = "size", defaultValue = "5") Integer size,
			@RequestParam(value = "filtro", defaultValue = "") String filtro,
			@RequestParam(value = "orden", defaultValue = "id") String orden,
			@RequestParam(value = "direccion", defaultValue = "asc") String direccion) {
		Sort.Direction sentido = "desc".equalsIgnoreCase(direccion)
				? Sort.Direction.DESC : Sort.Direction.ASC;
		return ResponseEntity.ok(serviceCorrelativo.findListado(
				filtro, PageRequest.of(page, size, Sort.by(sentido, obtenerPropiedadOrden(orden)))));
	}

	private String obtenerPropiedadOrden(String orden) {
		switch (orden == null ? "" : orden.toLowerCase()) {
			case "inicial":
				return "correlativoInicial";
			case "final":
				return "correlativoFinal";
			case "actual":
				return "correlativoActual";
			case "serie":
				return "serie";
			case "fecha":
				return "fechaCreacion";
			case "usuario":
				return "usuario.usuario";
			case "estado":
				return "estado.estado";
			default:
				return "idCorrelativo";
		}
	}

	@Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
	@GetMapping(value = "/correlativos/{id}")
	public ResponseEntity<Correlativo> findById(@PathVariable("id") Long id) throws MethodArgumentTypeMismatchException {
		log.info("Buscando correlativo con ID: {}", id);

		Correlativo correlativo = null;
		correlativo = serviceCorrelativo.findById(id);
		return ResponseEntity.ok(correlativo);
	}
	
	@Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
	@GetMapping(value = "/correlativos/usuario/{id}")
	public ResponseEntity<Correlativo> findByUsuario(@PathVariable("id") Integer idusuario){
		log.info("Correlativo Activo para el usuario: {}", idusuario);

		Correlativo correlativo = null;
		correlativo = serviceCorrelativo.findByUsuario(idusuario);
		return ResponseEntity.ok(correlativo);
	}

	@Secured(value = {"ROLE_ADMIN"})
	@PostMapping(value = "/correlativos")
	public ResponseEntity<Correlativo> create(@RequestBody Correlativo correlativo) {
		log.info("Registrando nuevo Correlativo");
		return new ResponseEntity<>(serviceCorrelativo.save(correlativo), HttpStatus.CREATED);
	}

	@Secured(value = {"ROLE_ADMIN"})
	@PutMapping(value = "/correlativos")
	public ResponseEntity<Correlativo> update(@RequestBody Correlativo correlativo, BindingResult result) {
		log.info("Actualizando correlativo: {}", correlativo.getIdCorrelativo());

		Correlativo corrUpdated = null;
		corrUpdated = serviceCorrelativo.update(correlativo);
		return new ResponseEntity<>(corrUpdated, HttpStatus.CREATED);
	}

	@Secured(value = {"ROLE_ADMIN"})
	@DeleteMapping(value = "/correlativos/{id}")
	public ResponseEntity<Correlativo> delete(@PathVariable("id") Long id) {
		log.info("Anulando correlativo: {}", id);

		Correlativo corrAnulado = null;
		corrAnulado = serviceCorrelativo.anular(id);
		return ResponseEntity.ok(corrAnulado);
	}

}
