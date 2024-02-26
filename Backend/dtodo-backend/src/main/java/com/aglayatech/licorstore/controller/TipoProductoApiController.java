package com.aglayatech.licorstore.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aglayatech.licorstore.model.TipoProducto;
import com.aglayatech.licorstore.service.ITipoProductoService;

@CrossOrigin(origins = {"http://localhost:4200", "https://dtodojalapa.xyz", "http://dtodojalapa.xyz"})
@RestController
@RequestMapping(value = "/api")
public class TipoProductoApiController {
	
	@Autowired
	private ITipoProductoService serviceTipo;
	
	@GetMapping(value = "/tipos-producto")
	public List<TipoProducto> index(){
		return serviceTipo.findAll();
	}
	
	@GetMapping(value = "/tipos-producto/page/{page}")
	public Page<TipoProducto> index(@PathVariable("page") Integer page) {
		return serviceTipo.findAll(PageRequest.of(page, 5));
	}
	
	@Secured(value = {"ROLE_COBRADOR","ROLE_ADMIN", "ROLE_INVENTARIO"})
	@GetMapping(value = "/tipos-producto/{id}")
	public ResponseEntity<?> findById(@PathVariable("id") int id){
		
		TipoProducto tipo = null;
		Map<String, Object> response = new HashMap<>();
		
		try {
			tipo = serviceTipo.findById(id);
		} catch(DataAccessException e) {
			response.put("mensaje", "¡La busqueda en la base de datos no pudo llevarse a cabo!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		if(tipo == null) {
			response.put("mensaje", "¡El valor de la busqueda no se encuentra registrado en la base de datos!");
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity<TipoProducto>(tipo, HttpStatus.OK);
	}
	
	@Secured(value = {"ROLE_ADMIN", "ROLE_INVENTARIO"})
	@PostMapping(value = "/tipos-producto")
	public ResponseEntity<?> create(@RequestBody TipoProducto tipoProducto){
		
		TipoProducto newTipo = null;
		Map<String, Object> response = new HashMap<>();
		
		try {
			newTipo = serviceTipo.save(tipoProducto); // recibe un objeto de TipoProducto, como respuesta por el registro llevado a cabo por jpa
		} catch (DataAccessException e) {
			response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return null;
		}
		
		if(newTipo == null) {
			response.put("mensaje", "¡No se pudo llevar a cabo el registro del Tipo de Producto!");
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		response.put("mensaje", "¡El registro se llevó a cabo con éxito!");
		response.put("tipoProducto", newTipo);
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}
	
	@SuppressWarnings("null")
	@Secured(value = {"ROLE_ADMIN", "ROLE_INVENTARIO"})
	@PutMapping(value = "/tipos-producto")
	public ResponseEntity<?> update(@RequestBody TipoProducto tipoProducto){
		
		TipoProducto tipoUpdated = null;
		Map<String, Object> response = new HashMap<>();
		
		if(tipoProducto == null) {
			response.put("mensaje", "El tipo de producto con ID: ".concat(tipoProducto.getIdTipoProducto().toString())
					.concat(" no existe en la base de datos."));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}
		
		try {
			tipoUpdated = serviceTipo.save(tipoProducto);
		} catch (DataAccessException e) {
			response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		if(tipoUpdated == null) {
			response.put("mensaje", "¡No se pudo llevar a cabo la actualización!");
			return new ResponseEntity<Map<String, Object>>(response, null);
		}
		
		response.put("mensaje", "¡La actualización se llevó a cabo con éxito!");
		response.put("tipoUpdated", tipoUpdated);
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}
	
	@Secured(value = {"ROLE_ADMIN", "ROLE_INVENTARIO"})
	@DeleteMapping(value = "/tipos-producto/{id}")
	public ResponseEntity<?> delete(@PathVariable("id") Integer idtipo) {
		
		Map<String, Object> response = new HashMap<>();
		
		try {
			serviceTipo.delete(idtipo);
		} catch (DataAccessException e) {
			response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		response.put("mensaje", "¡El registro ha sido eliminado con éxito!");
		
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.OK);
		
	}
}
