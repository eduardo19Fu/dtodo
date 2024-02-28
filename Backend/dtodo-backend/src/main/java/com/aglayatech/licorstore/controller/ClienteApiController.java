package com.aglayatech.licorstore.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

@CrossOrigin(origins = {"http://localhost:4200", "https://dtodojalapa.xyz"})
@RestController
@RequestMapping(value = "/api")
@Slf4j
public class ClienteApiController {

	private final IClienteService serviceCliente;

	public ClienteApiController(IClienteService serviceCliente) {
		this.serviceCliente = serviceCliente;
	}

	@GetMapping(value = "/clientes")
	public ResponseEntity<List<Cliente>> listarClientes () {
		log.info("Listando Clientes");

		List<Cliente> listado = new ArrayList<>();
		listado = serviceCliente.findAll();
		return ResponseEntity.ok(listado);
	}
	
	@GetMapping(value = "/clientes/page/{page}")
	public ResponseEntity<Page<Cliente>> listarClientesPaginados (@PathVariable("page") Integer page) {
		log.info("Buscando clientes pagina: {}", page);

		Page<Cliente> listadoPaginado = null;
		listadoPaginado = serviceCliente.findAll(PageRequest.of(page, 5));
		return ResponseEntity.ok(listadoPaginado);
	}

	@GetMapping(value = "/clientes/nombre/{name}")
	public ResponseEntity<List<Cliente>> findByName(@PathVariable("name") String name) {
		log.info("Buscando clientes con nombre: {}", name);

		List<Cliente> listadoClientes = new ArrayList<>();
		listadoClientes = serviceCliente.findByName(name);
		return ResponseEntity.ok(listadoClientes);
	}
	
	@GetMapping(value = "/clientes/nit/{nit}")
	public ResponseEntity<?> findByNit(@PathVariable("nit") String nit){
		log.info("Buscando cliente con nit: {}", nit);

		Cliente cliente = null;
		cliente = serviceCliente.findByNit(nit);
		return ResponseEntity.ok(cliente);
	}

	@GetMapping(value = "/clientes/{id}")
	public ResponseEntity<?> findById(@PathVariable("id") Integer id) {
		log.info("Buscando cliente con ID: {}", id);

		Cliente cliente = null;
		cliente = serviceCliente.findById(id);
		return ResponseEntity.ok(cliente);
	}

	@GetMapping(value = "/clientes/cantidad-clientes")
	public ResponseEntity<Integer> getTotalClientes(){
		log.info("Obteniendo cantidad de clientes");

		Integer totalClientes = 0;
		totalClientes = serviceCliente.totalClientes();
		return ResponseEntity.ok(totalClientes);
	}

	@Secured(value = {"ROLE_COBRADOR","ROLE_ADMIN"})
	@PostMapping(value = "/clientes")
	public ResponseEntity<Cliente> create(@RequestBody Cliente cliente) {
		log.info("Registrando cliente con NIT: {}", cliente.getNit());

		Cliente newCliente = null;
		newCliente = serviceCliente.save(cliente);
		return new ResponseEntity<>(newCliente, HttpStatus.CREATED);
	}
	
	@Secured(value = {"ROLE_ADMIN"})
	@SuppressWarnings("null")
	@PutMapping(value = {"/clientes", "/clientes/update/{id}"})
	public ResponseEntity<Cliente> update(@RequestBody Cliente cliente, @PathVariable(value = "id", required = false) Integer idcliente) {
		log.info("Actualizando cliente: ", cliente.getIdCliente());

		Cliente clienteUpdated = null;
		clienteUpdated = serviceCliente.save(cliente);
		return new ResponseEntity<>(clienteUpdated, HttpStatus.CREATED);
	}
	
	@Secured(value = {"ROLE_ADMIN"})
	@DeleteMapping(value = "/clientes/{id}")
	public ResponseEntity<?> delete(@PathVariable("id") Integer idcliente) {
		log.info("Eliminado cliente ID: ", idcliente);

		Map<String, Object> response = new HashMap<>();
		Cliente clienteEliminar = null;
		clienteEliminar = serviceCliente.findById(idcliente);
		serviceCliente.delete(clienteEliminar);

		response.put("mensaje", "Cliente eliminado con éxito");
		response.put("idcliente", idcliente);
		return ResponseEntity.ok(response);
	}

}
