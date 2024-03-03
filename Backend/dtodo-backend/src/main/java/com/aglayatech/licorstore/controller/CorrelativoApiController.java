package com.aglayatech.licorstore.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aglayatech.licorstore.error.exceptions.MethodArgumentTypeMismatchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

import com.aglayatech.licorstore.model.Correlativo;
import com.aglayatech.licorstore.service.ICorrelativoService;

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

	@Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
	@GetMapping(value = "/correlativos/{id}")
	public ResponseEntity<Correlativo> findById(@PathVariable("id") Long id) throws MethodArgumentTypeMismatchException {
		log.info("Buscando correlativo con ID: {}", id);

		Correlativo correlativo = null;
		correlativo = serviceCorrelativo.findById(id);
		return ResponseEntity.ok(correlativo);
	}
	
	@Secured(value = {"ROLE_COBRADOR", "ROLE_COBRADOR"})
	@GetMapping(value = "/correlativos/usuario/{id}")
	public ResponseEntity<Correlativo> findByUsuario(@PathVariable("id") Integer idusuario){
		log.info("Correlativo Activo para el usuario: {}", idusuario);

		Correlativo correlativo = null;
		correlativo = serviceCorrelativo.findByUsuario(idusuario);
		return ResponseEntity.ok(correlativo);
	}

	@Secured(value = {"ROLE_ADMIN"})
	@PostMapping(value = "/correlativos")
	public ResponseEntity<Map<String, Object>> create(@RequestBody Correlativo correlativo, BindingResult result) {
		log.info("Registrando nuevo Correlativo");

		Map<String, Object> response = new HashMap<>();
		Correlativo newCorrelativo = null;

		newCorrelativo = serviceCorrelativo.save(correlativo);

		if(newCorrelativo == null) {
			response.put("mensaje", "El usuario ya cuenta con un correlativo activo");
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		response.put("mensaje", "¡Correlativo creado con éxito!");
		response.put("correlativo", newCorrelativo);
		return new ResponseEntity<>(response, HttpStatus.CREATED);
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
