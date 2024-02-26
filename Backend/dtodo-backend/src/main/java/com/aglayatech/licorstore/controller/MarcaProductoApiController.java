package com.aglayatech.licorstore.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.Valid;

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

import com.aglayatech.licorstore.model.MarcaProducto;
import com.aglayatech.licorstore.service.IMarcaProductoService;

@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz", "http://dtodojalapa.xyz" })
@RestController
@RequestMapping(value = "/api")
public class MarcaProductoApiController {

	@Autowired
	private IMarcaProductoService serviceMarca;

	@GetMapping(value = "/marcas")
	public List<MarcaProducto> index() {
		List<MarcaProducto> list = serviceMarca.findAll();
		return list;
	}
	
	@GetMapping(value = "/marcas/page/{page}")
	public Page<MarcaProducto> index(@PathVariable("page") Integer page) {
		return serviceMarca.findAll(PageRequest.of(page, 5));
	}

	@Secured(value = {"ROLE_COBRADOR","ROLE_ADMIN", "ROLE_INVENTARIO"})
	@GetMapping(value = "/marcas/{id}")
	public ResponseEntity<?> getById(@PathVariable("id") Integer id) {

		MarcaProducto marca = null;
		Map<String, Object> response = new HashMap<>();

		try {
			marca = serviceMarca.findById(id);
		} catch (DataAccessException e) {
			response.put("mensaje", "La busqueda en la base de datos no pudo llevarse a cabo!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (marca == null) {
			response.put("mensaje", "¡La marca buscada no se encuentra registrada en la base de datos!");
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity<MarcaProducto>(marca, HttpStatus.OK);
	}

	@Secured(value = {"ROLE_ADMIN", "ROLE_INVENTARIO"})
	@PostMapping(value = "/marcas")
	public ResponseEntity<?> create(@Valid @RequestBody MarcaProducto marca, BindingResult result) {

		MarcaProducto newMarca = null;
		Map<String, Object> response = new HashMap<>();

		// manejador de errores desde el backend
		if (result.hasErrors()) {

			/* Primera forma:
			 * 
			 * List<String> errors = new ArrayList<>();
			 * 
			 * for(FieldError err : result.getFieldErrors()) {
			 * errors.add("El campo '".concat(err.getField().concat("' ")).concat(err.
			 * getDefaultMessage())); }
			 */

			List<String> errors = result.getFieldErrors().stream()
					.map(err -> "El campo '".concat(err.getField().concat("' ")).concat(err.getDefaultMessage()))
					.collect(Collectors.toList());

			response.put("errors", errors);
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.BAD_REQUEST);
		}

		try {
			newMarca = serviceMarca.save(marca);
		} catch (DataAccessException ex) {
			response.put("mensaje", "Ha ocurrido un error en la base de datos!");
			response.put("error", ex.getMessage().concat(": ").concat(ex.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		response.put("mensaje", "El Regimen ha sido registrado con éxito!");
		response.put("marca", newMarca);
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}

	// Controlador que permite actualizar el registro enviado por protocolo Http PUT
	@Secured(value = {"ROLE_ADMIN", "ROLE_INVENTARIO"})
	@PutMapping(value = "/marcas")
	public ResponseEntity<?> update(@Valid @RequestBody MarcaProducto marca, BindingResult result) {

		Map<String, Object> response = new HashMap<>();

		if (result.hasErrors()) {

			/*
			 * List<String> errors = new ArrayList<>();
			 * 
			 * for(FieldError err : result.getFieldErrors()) {
			 * errors.add("El campo '".concat(err.getField().concat("' ")).concat(err.
			 * getDefaultMessage())); }
			 */

			List<String> errors = result.getFieldErrors().stream()
					.map(err -> "El campo '".concat(err.getField().concat("' ")).concat(err.getDefaultMessage()))
					.collect(Collectors.toList());

			response.put("errors", errors);
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.BAD_REQUEST);
		}

		try {
			serviceMarca.save(marca);
		} catch (DataAccessException e) {
			response.put("mensaje", "Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (marca.getIdMarcaProducto() == null) {
			response.put("mensaje", "¡La marca que desea acutualizar no se encuentra registrada en la base de datos!");
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}

		response.put("mensaje", "El Regimen ha sido registrado con éxito!");
		response.put("marca", marca);
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}

	@Secured(value = {"ROLE_ADMIN", "ROLE_INVENTARIO"})
	@DeleteMapping(value = "/marcas/{id}")
	public ResponseEntity<?> delete(@PathVariable("id") int id) {

		Map<String, Object> response = new HashMap<>();

		try {
			serviceMarca.deleteMarca(id);
		} catch (DataAccessException e) {
			response.put("mensaje", "Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		response.put("mensaje", "¡Marca eliminada con éxito!");

		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.OK);

	}

}
