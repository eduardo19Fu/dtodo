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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

import com.aglayatech.licorstore.model.Usuario;
import com.aglayatech.licorstore.service.IUsuarioService;

@CrossOrigin(origins = { "http://localhost:4200",  "https://dtodojalapa.xyz", "http://dtodojalapa.xyz" })
@RestController
@RequestMapping(value = "/api")
public class UsuarioApiController {
	
	@Autowired
	private IUsuarioService serviceUsuario;
	
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR"})
	@GetMapping(value = "/usuarios")
	public List<Usuario> index(){
		return this.serviceUsuario.findAll();
	}
	
	@GetMapping(value = "/usuarios/cajero")
	public List<Usuario> findCajeros(){
		return this.serviceUsuario.cajeros();
	}
	
	@GetMapping(value = "/usuarios/page/{page}")
	public Page<Usuario> index(@PathVariable("page") Integer page){
		return this.serviceUsuario.findAll(PageRequest.of(page, 5));
	}
	
	@Secured(value = {"ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO"})
	@GetMapping(value = "/usuarios/{id}")
	public ResponseEntity<?> findById(@PathVariable("id") Integer idusuario){
		
		Usuario usuario = null;
		Map<String, Object> response = new HashMap<>();
		int a = 0;
		System.out.println(a);
		
		try {
			usuario = serviceUsuario.findById(idusuario);
		} catch (DataAccessException e) {
			response.put("mensaje", "Error ocurrido en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		if(usuario == null) {
			response.put("mensaje", "¡El usuario deseado con ID " + idusuario + " no se encuentra registrado en la base de datos!");
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity<Usuario>(usuario, HttpStatus.OK);
	}

	@Secured(value = {"ROLE_ADMIN"})
	@GetMapping(value = "/usuarios/cantidad-usuarios")
	public ResponseEntity<?> getTotalUsuarios(){
		Integer total = 0;
		Map<String, Object> response = new HashMap<>();

		try {
			total = this.serviceUsuario.totalUsuarios();
		} catch (DataAccessException e) {
			response.put("mensaje", "Error ocurrido en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<Integer>(total, HttpStatus.OK);
	}
	
	@Secured(value = {"ROLE_ADMIN"})
	@PostMapping(value = "/usuarios")
	public ResponseEntity<?> create(@Valid @RequestBody Usuario usuario, BindingResult result){
		
		Usuario newUsuario = null;
		Map<String, Object> response = new HashMap<>();
		
		if(result.hasErrors()) {
			// tratamiento de errores
			List<String> errors = result.getFieldErrors().stream()
					.map(err -> "El campo '".concat(err.getField().concat("' ")).concat(err.getDefaultMessage()))
					.collect(Collectors.toList());

			response.put("errors", errors);
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.BAD_REQUEST);
		}
		
		try {
			usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
			newUsuario = serviceUsuario.save(usuario);
		} catch (DataAccessException e) {
			response.put("mensaje", "Error ocurrido en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		response.put("mensaje", "Usuario creado con éxito!");
		response.put("usuario", newUsuario);
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}
	
	@Secured(value = {"ROLE_ADMIN"})
	@PutMapping(value = "/usuarios/{id}")
	public ResponseEntity<?> update(@Valid @RequestBody Usuario usuario, BindingResult result, @PathVariable("id") Integer idusuario){
		
		Usuario usuarioActual = serviceUsuario.findById(idusuario);
		Usuario usuarioUpdated = null;
		
		Map<String, Object> response = new HashMap<>();
		
		if(usuarioActual == null) {
			response.put("mensaje", "El usuario a actualizar no existe en la base de datos!");
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}
		
		if(result.hasErrors()) {
			List<String> errors = result.getFieldErrors().stream()
					.map(err -> "El campo '".concat(err.getField().concat("' ")).concat(err.getDefaultMessage()))
					.collect(Collectors.toList());
			
			response.put("errors", errors);
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.BAD_REQUEST);
		}
		
		try {
			
			usuarioActual.setPrimerNombre(usuario.getPrimerNombre());
			usuarioActual.setSegundoNombre(usuario.getSegundoNombre());
			usuarioActual.setApellido(usuario.getApellido());
			usuarioActual.setUsuario(usuario.getUsuario());
			usuarioActual.setRoles(usuario.getRoles());
			
			if(!usuarioActual.getPassword().equals(usuario.getPassword())) {
				usuarioActual.setPassword(passwordEncoder.encode(usuario.getPassword()));
			} else {
				usuarioActual.setPassword(usuario.getPassword());
			}
			
			usuarioUpdated = serviceUsuario.save(usuarioActual);
		} catch (DataAccessException e) {
			response.put("mensaje", "Error ocurrido en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		response.put("mensaje", "¡El usuario ".concat(usuarioUpdated.getUsuario()).concat(" ha sido actualizado con éxito!"));
		response.put("usuario", usuarioUpdated);
		
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}
	
	@Secured(value = {"ROLE_ADMIN"})
	@DeleteMapping(value = "/usuarios/{id}")
	public ResponseEntity<?> delete(@PathVariable("id") Integer idusuario){
		Map<String, Object> response = new HashMap<>();

		try {
			serviceUsuario.delete(idusuario);
		} catch (DataAccessException e) {
			response.put("mensaje", "Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		response.put("mensaje", "¡Usuario eliminado con éxito!");

		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.OK);
	}
	

}
