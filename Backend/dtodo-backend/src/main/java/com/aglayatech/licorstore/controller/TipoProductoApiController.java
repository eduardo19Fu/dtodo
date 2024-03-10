package com.aglayatech.licorstore.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

@CrossOrigin(origins = {"http://localhost:4200", "https://dtodojalapa.xyz"})
@RestController
@RequestMapping(value = "/api")
@RequiredArgsConstructor
@Slf4j
public class TipoProductoApiController {

	private final ITipoProductoService serviceTipo;
	
	@GetMapping(value = "/tipos-producto")
	public ResponseEntity<List<TipoProducto>> index(){
		log.info("Listando Categorías de productos");

		List<TipoProducto> tipos = new ArrayList<>();
		tipos = serviceTipo.findAll();
		return ResponseEntity.ok(tipos);
	}
	
	@GetMapping(value = "/tipos-producto/page/{page}")
	public ResponseEntity<Page<TipoProducto>> index(@PathVariable("page") Integer page) {
		log.info("Listado de categorías paginadas");

		Page<TipoProducto> tiposPaginados = serviceTipo.findAll(PageRequest.of(page, 5));
		return ResponseEntity.ok(tiposPaginados);
	}
	
	@Secured(value = {"ROLE_COBRADOR","ROLE_ADMIN", "ROLE_INVENTARIO"})
	@GetMapping(value = "/tipos-producto/{id}")
	public ResponseEntity<TipoProducto> findById(@PathVariable("id") int id){
		log.info("Buscar categoría de producto con ID: {}", id);

		TipoProducto tipo = null;
		tipo = serviceTipo.findById(id);
		return ResponseEntity.ok(tipo);
	}
	
	@Secured(value = {"ROLE_ADMIN", "ROLE_INVENTARIO"})
	@PostMapping(value = "/tipos-producto")
	public ResponseEntity<TipoProducto> create(@RequestBody TipoProducto tipoProducto){
		log.info("Creando nueva categoría de producto: {}", tipoProducto.getTipoProducto());

		TipoProducto newTipo = null;
		newTipo = serviceTipo.save(tipoProducto);
		return new ResponseEntity<>(newTipo, HttpStatus.CREATED);
	}

	@Secured(value = {"ROLE_ADMIN", "ROLE_INVENTARIO"})
	@PutMapping(value = "/tipos-producto")
	public ResponseEntity<?> update(@RequestBody TipoProducto tipoProducto){
		log.info("Actualizando Categoría de Producto: {}", tipoProducto.getTipoProducto());

		TipoProducto tipoUpdated = null;
		tipoUpdated = serviceTipo.save(tipoProducto);
		return new ResponseEntity<>(tipoUpdated, HttpStatus.CREATED);
	}
	
	@Secured(value = {"ROLE_ADMIN", "ROLE_INVENTARIO"})
	@DeleteMapping(value = "/tipos-producto/{id}")
	public ResponseEntity<Map<String, Object>> delete(@PathVariable("id") Integer idtipo) {
		
		Map<String, Object> response = new HashMap<>();
		TipoProducto tipoProducto = null;
		tipoProducto = serviceTipo.findById(idtipo);
		serviceTipo.delete(tipoProducto);
		
		response.put("mensaje", "¡El registro ha sido eliminado con éxito!");
		response.put("idtipo", idtipo);
		return new ResponseEntity<>(response, HttpStatus.OK);
		
	}
}
