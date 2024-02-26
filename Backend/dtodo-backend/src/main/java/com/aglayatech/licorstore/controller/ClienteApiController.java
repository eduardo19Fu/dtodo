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

import com.aglayatech.licorstore.model.Cliente;
import com.aglayatech.licorstore.service.IClienteService;

@CrossOrigin(origins = {"http://localhost:4200", "https://dtodojalapa.xyz", "http://dtodojalapa.xyz"})
@RestController
@RequestMapping(value = "/api")
public class ClienteApiController {

	@Autowired
	private IClienteService serviceCliente;

	@GetMapping(value = "/clientes")
	public List<Cliente> index() {
		return serviceCliente.findAll();
	}
	
	@GetMapping(value = "/clientes/page/{page}")
	public Page<Cliente> index(@PathVariable("page") Integer page) {
		return serviceCliente.findAll(PageRequest.of(page, 5));
	}

	@GetMapping(value = "/clientes/nombre/{name}")
	public List<Cliente> findByName(@PathVariable("name") String name) {
		return serviceCliente.findByName(name);
	}
	
	@GetMapping(value = "/clientes/nit/{nit}")
	public ResponseEntity<?> findByNit(@PathVariable("nit") String nit){
		
		Cliente cliente = null;
		Map<String, Object> response = new HashMap<>();
		
		try {
			cliente = serviceCliente.findByNit(nit);
		} catch (DataAccessException e) {
			response.put("mensaje", "¡Error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		if(cliente == null) {
			response.put("mensaje", "¡El cliente no se encuentra registrado en la base de datos!");
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity<Cliente>(cliente, HttpStatus.OK);
	}

	// @Secured(value = {"ROLE_COBRADOR","ROLE_ADMIN"})
	@GetMapping(value = "/clientes/{id}")
	public ResponseEntity<?> findById(@PathVariable("id") Integer id) {

		Cliente cliente = null;
		Map<String, Object> response = new HashMap<>();

		try {
			cliente = serviceCliente.findById(id);
		} catch (DataAccessException e) {
			response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (cliente == null) {
			response.put("mensaje", "¡El cliente con ID: ".concat(id.toString()).concat(" no se encuentra registrado en la base de datos!"));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity<Cliente>(cliente, HttpStatus.OK);
	}

	@GetMapping(value = "/clientes/cantidad-clientes")
	public ResponseEntity<?> getTotalClientes(){
		Integer total = 0;
		Map<String, Object> response = new HashMap<>();

		try {
			total = this.serviceCliente.totalClientes();
		} catch(DataAccessException e){
			response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<Integer>(total, HttpStatus.OK);
	}

	@Secured(value = {"ROLE_COBRADOR","ROLE_ADMIN"})
	@PostMapping(value = "/clientes")
	public ResponseEntity<?> create(@RequestBody Cliente cliente) {
		
		Cliente newCliente = null;
		Map<String, Object> response = new HashMap<>();
		
		try {
			newCliente = serviceCliente.save(cliente);
		} catch (DataAccessException e) {
			response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		if(newCliente == null) {
			response.put("mensaje", "¡No se ha podido registrar el nuevo cliente!");
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		response.put("mensaje", "¡Cliente registrado con éxito!");
		response.put("cliente", newCliente);
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}
	
	@Secured(value = {"ROLE_ADMIN"})
	@SuppressWarnings("null")
	@PutMapping(value = "/clientes")
	public ResponseEntity<?> update(@RequestBody Cliente cliente) {
		
		Cliente clienteUpdated = null;
		Map<String, Object> response = new HashMap<>();
		
		if(cliente == null) {
			response.put("mensaje", "¡El cliente con ID: ".concat(cliente.getIdCliente().toString())
					.concat(" no existe en la base de datos!"));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}
		
		try {
			clienteUpdated = serviceCliente.save(cliente);
		} catch (DataAccessException e) {
			response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		response.put("mensaje", "¡El cliente ha sido actualizado con éxito!");
		response.put("cliente", clienteUpdated);
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}
	
	@Secured(value = {"ROLE_ADMIN"})
	@DeleteMapping(value = "/clientes/{id}")
	public ResponseEntity<?> delete(@PathVariable("id") Integer idcliente) {
		
		Map<String, Object> response = new HashMap<>();
		
		try {
			serviceCliente.delete(idcliente);
		} catch (DataAccessException e) {
			response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		response.put("mensaje", "¡El cliente ha sido eliminado con éxito!");
		
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.OK);
		
	}

}
