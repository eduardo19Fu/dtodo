package com.aglayatech.licorstore.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
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
import com.aglayatech.licorstore.model.Estado;
import com.aglayatech.licorstore.model.Usuario;
import com.aglayatech.licorstore.service.ICorrelativoService;
import com.aglayatech.licorstore.service.IEstadoService;
import com.aglayatech.licorstore.service.IUsuarioService;

@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz", "http://dtodojalapa.xyz" })
@RestController
@RequestMapping(value = "/api")
public class CorrelativoApiController {

	@Autowired
	private ICorrelativoService serviceCorrelativo;
	
	@Autowired
	private IEstadoService serviceEstado;
	
	@Autowired
	private IUsuarioService serviceUsuario;

	@Secured(value = {"ROLE_ADMIN"})
	@GetMapping(value = "/correlativos")
	public List<Correlativo> index() {
		return serviceCorrelativo.findAll();
	}

	@GetMapping(value = "/correlativos/page/{page}")
	public Page<Correlativo> index(@PathVariable("page") Integer page) {
		return serviceCorrelativo.findAll(PageRequest.of(page, 5));
	}

	@Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
	@GetMapping(value = "/correlativos/{id}")
	public ResponseEntity<?> findById(@PathVariable("id") Long id) {

		Correlativo correlativo = null;
		Map<String, Object> response = new HashMap<>();

		try {
			correlativo = serviceCorrelativo.findById(id);
		} catch (DataAccessException e) {
			response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (correlativo == null) {
			response.put("mensaje", "¡El correlativo no se encuentra registrado en la base de datos!");
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity<Correlativo>(correlativo, HttpStatus.OK);
	}
	
	@Secured(value = {"ROLE_COBRADOR", "ROLE_COBRADOR"})
	@GetMapping(value = "/correlativos/usuario/{id}")
	public ResponseEntity<?> findByUsuario(@PathVariable("id") Integer idusuario){
		
		Usuario usuario = null;
		Correlativo correlativo = null;
		Estado estado = null;
		
		Map<String, Object> response = new HashMap<>();
		
		try {
			estado = serviceEstado.findById(1);
			usuario = serviceUsuario.findById(idusuario);
			
			if(usuario != null) {
				correlativo = serviceCorrelativo.findByUsuario(usuario, estado);
			}
		} catch (DataAccessException e) {
			response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		if (correlativo == null) {
			response.put("mensaje", "¡No existe correlativo activo para este usuario!");
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		} else if (correlativo.getCorrelativoFinal() == correlativo.getCorrelativoActual()){

		}

		return new ResponseEntity<Correlativo>(correlativo, HttpStatus.OK);
	}

	@Secured(value = {"ROLE_ADMIN"})
	@PostMapping(value = "/correlativos")
	public ResponseEntity<?> create(@RequestBody Correlativo correlativo, BindingResult result) {

		Correlativo newCorrelativo = null;
		Estado estado = serviceEstado.findById(1);
		Map<String, Object> response = new HashMap<>();

		if (result.hasErrors()) {
			// tratamiento de errores
			List<String> errors = result.getFieldErrors().stream()
					.map(err -> "El campo '".concat(err.getField().concat("' ")).concat(err.getDefaultMessage()))
					.collect(Collectors.toList());

			response.put("errors", errors);
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.BAD_REQUEST);
		}
		
		if(serviceCorrelativo.findByUsuario(correlativo.getUsuario(), estado) != null) {
			response.put("mensaje", "¡Correlativo Activo!");
			response.put("error", "¡El usuario ".concat(correlativo.getUsuario().getUsuario()).concat(" ya cuenta con un correlativo activo!"));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CONFLICT);
		}

		try {
			correlativo.setCorrelativoActual(correlativo.getCorrelativoInicial());
			correlativo.setEstado(serviceEstado.findById(1));
			
			newCorrelativo = serviceCorrelativo.save(correlativo);
		} catch (DataAccessException e) {
			response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		response.put("mensaje", "¡Correlativo creado con éxito!");
		response.put("correlativo", newCorrelativo);
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}

	@Secured(value = {"ROLE_ADMIN"})
	@PutMapping(value = "/correlativos")
	public ResponseEntity<?> update(@RequestBody Correlativo correlativo, BindingResult result) {

		Correlativo corrUpdated = null;
		Map<String, Object> response = new HashMap<>();

		if (result.hasErrors()) {
			// tratamiento de errores
			List<String> errors = result.getFieldErrors().stream()
					.map(err -> "El campo '".concat(err.getField().concat("' ")).concat(err.getDefaultMessage()))
					.collect(Collectors.toList());

			response.put("errors", errors);
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.BAD_REQUEST);
		}

		try {
			corrUpdated = serviceCorrelativo.save(correlativo);
		} catch (DataAccessException e) {
			response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		response.put("mensaje", "¡El correlativo ha sido actualizado con éxito!");
		response.put("correlativo", corrUpdated);
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}

	@Secured(value = {"ROLE_ADMIN"})
	@DeleteMapping(value = "/correlativos/{id}")
	public ResponseEntity<?> delete(@PathVariable("id") Long id) {
		
		Correlativo correlativoAnulado = null, correlativo = null;
		Estado estado = serviceEstado.findByEstado("ANULADO");
		Map<String, Object> response = new HashMap<>();

		try {
			correlativo = serviceCorrelativo.findById(id);
			correlativo.setEstado(estado);
			correlativoAnulado = serviceCorrelativo.save(correlativo);
			// serviceCorrelativo.delete(id);
		} catch (DataAccessException e) {
			response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		response.put("mensaje", "¡El correlativo ha sido eliminado con éxito!");
		response.put("correlativo", correlativoAnulado);
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.OK);
	}

}
