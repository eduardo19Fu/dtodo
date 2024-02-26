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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aglayatech.licorstore.model.Role;
import com.aglayatech.licorstore.service.IRoleService;

@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz", "http://dtodojalapa.xyz" })
@RestController
@RequestMapping(value = "/api")
public class RoleApiController {
	
	@Autowired
	private IRoleService serviceRole;
	
	@GetMapping(value = "/roles")
	public List<Role> index(){
		return this.serviceRole.findAll();
	}
	
	@GetMapping(value = "/roles/page/{page}")
	public Page<Role> index(@PathVariable("page") Integer page){
		return this.serviceRole.findAll(PageRequest.of(page, 5));
	}
	
	@Secured(value = {"ROLE_ADMIN"})
	@GetMapping(value = "/roles/name/{role}")
	public ResponseEntity<?> buscarPorNombre(@PathVariable("role") String role){
		
		Role foundRole = null;
		Map<String, Object> response = new HashMap<>();
		
		try {
			foundRole = serviceRole.findByName(role);
		} catch (DataAccessException e) {
			response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		return new ResponseEntity<Role>(foundRole, HttpStatus.OK);
	}
	
	@Secured(value = {"ROLE_ADMIN"})
	@GetMapping(value = "/roles/{id}")
	public ResponseEntity<?> getById(@PathVariable("id") Integer idrole){
		
		Role role = null;
		Map<String, Object> response = new HashMap<>();
		
		try {
			role = serviceRole.findById(idrole);
		} catch (DataAccessException e) {
			response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		if(role == null) {
			response.put("mensaje", "¡El role no se encuentra registrado en la base de datos!");
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity<Role>(role, HttpStatus.OK);
	}
	
	@Secured(value = {"ROLE_ADMIN"})
	@PostMapping(value = "/roles")
	public ResponseEntity<?> create(@RequestBody Role role, BindingResult result){
		
		Role newRole = null;
		Map<String, Object> response = new HashMap<>();
		
		if(result.hasErrors()) {
			List<String> errors = result.getFieldErrors().stream()
					.map(err -> "El campo '".concat(err.getField().concat("' ")).concat(err.getDefaultMessage()))
					.collect(Collectors.toList());

			response.put("errors", errors);
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.BAD_REQUEST);
		}
		
		try {
			newRole = serviceRole.save(role);
		} catch (DataAccessException e) {
			response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	
		response.put("mensaje", "¡El Role ha sido creado registrado con éxito!");
		response.put("role", newRole);
		
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}

}
