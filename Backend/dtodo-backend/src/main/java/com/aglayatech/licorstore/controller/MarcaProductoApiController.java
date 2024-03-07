package com.aglayatech.licorstore.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

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

import com.aglayatech.licorstore.model.MarcaProducto;
import com.aglayatech.licorstore.service.IMarcaProductoService;

@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping(value = "/api")
@RequiredArgsConstructor
@Slf4j
public class MarcaProductoApiController {

	private final IMarcaProductoService serviceMarca;

	@GetMapping(value = "/marcas")
	public ResponseEntity<List<MarcaProducto>> index() {
		log.info("Listando marcas de producto registradas");

		List<MarcaProducto> marcasProducto = new ArrayList<>();
		marcasProducto = serviceMarca.findAll();
		return ResponseEntity.ok(marcasProducto);
	}
	
	@GetMapping(value = "/marcas/page/{page}")
	public ResponseEntity<Page<MarcaProducto>> index(@PathVariable("page") Integer page) {
		log.info("Listando marcas por páginas: {}");

		Page<MarcaProducto> marcasPaginadas = null;
		marcasPaginadas = serviceMarca.findAll(PageRequest.of(page, 5));
		return ResponseEntity.ok(marcasPaginadas);
	}

	@Secured(value = {"ROLE_COBRADOR","ROLE_ADMIN", "ROLE_INVENTARIO"})
	@GetMapping(value = "/marcas/{id}")
	public ResponseEntity<MarcaProducto> getById(@PathVariable("id") Integer id) {
		log.info("Buscando marca de Producto con ID: {}", id);

		MarcaProducto marca = null;
		marca = serviceMarca.findById(id);
		return ResponseEntity.ok(marca);
	}

	@Secured(value = {"ROLE_ADMIN", "ROLE_INVENTARIO"})
	@PostMapping(value = "/marcas")
	public ResponseEntity<MarcaProducto> create(@Valid @RequestBody MarcaProducto marca, BindingResult result) {
		log.info("Registrando marca: {}", marca.getMarca());

		MarcaProducto newMarca = null;
		newMarca = serviceMarca.save(marca);
		return new ResponseEntity<>(newMarca, HttpStatus.CREATED);
	}

	@Secured(value = {"ROLE_ADMIN", "ROLE_INVENTARIO"})
	@PutMapping(value = "/marcas")
	public ResponseEntity<MarcaProducto> update(@Valid @RequestBody MarcaProducto marca, BindingResult result) {
		log.info("Actualizando marca: {}", marca.getMarca());

		MarcaProducto marcaUpdated = new MarcaProducto();
		marcaUpdated = serviceMarca.save(marca);
		return new ResponseEntity<>(marcaUpdated, HttpStatus.CREATED);
	}

	@Secured(value = {"ROLE_ADMIN", "ROLE_INVENTARIO"})
	@DeleteMapping(value = "/marcas/{id}")
	public ResponseEntity<Map<String, Object>> delete(@PathVariable("id") int id) {
		log.info("Eliminando marca de producto: {}", id);

		Map<String, Object> response = new HashMap<>();
		MarcaProducto marcaProducto = null;
		marcaProducto = serviceMarca.findById(id);
		serviceMarca.deleteMarca(marcaProducto);

		response.put("mensaje", "Marca ha sido eliminado con éxito");
		response.put("idmarca", id);
		return new ResponseEntity<>(response, HttpStatus.OK);

	}

}
