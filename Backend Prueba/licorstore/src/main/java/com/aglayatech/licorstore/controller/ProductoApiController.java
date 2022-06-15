package com.aglayatech.licorstore.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aglayatech.licorstore.model.Estado;
import com.aglayatech.licorstore.model.Producto;
import com.aglayatech.licorstore.service.IEstadoService;
import com.aglayatech.licorstore.service.IProductoService;
import com.aglayatech.licorstore.service.IUploadFileService;

import net.sf.jasperreports.engine.JRException;

@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz", "http://dtodojalapa.xyz" })
@RestController
@RequestMapping(value = "/api")
public class ProductoApiController {

	@Autowired
	private IProductoService serviceProducto;

	@Autowired
	private IEstadoService serviceEstado;

	@Autowired
	private IUploadFileService serviceUpload;

	@GetMapping(value = "/productos")
	public List<Producto> index() {
		return serviceProducto.findAll();
	}

	@GetMapping(value = "/productos/page/{page}")
	public Page<Producto> index(@PathVariable("page") Integer page) {
		return serviceProducto.findAll(PageRequest.of(page, 5));
	}
	
	@GetMapping(value = "/productos-activos")
	public List<Producto> findAll(){
		Estado estado = serviceEstado.findById(1);
		return serviceProducto.findAllByEstado(estado);
	}

	@Secured({ "ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO" })
	@GetMapping(value = "/productos/{id}")
	public ResponseEntity<?> findById(@PathVariable("id") int idproducto) {

		Producto producto = null;
		Map<String, Object> response = new HashMap<>();

		try {
			producto = serviceProducto.findById(idproducto);
		} catch (DataAccessException e) {
			response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (producto == null) {
			response.put("mensaje", "¡El producto buscado no se encuentra registrado en la base de datos!");
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity<Producto>(producto, HttpStatus.OK);
	}

	@Secured(value = {"ROLE_ADMIN", "ROLE_INVENTARIO"})
	@GetMapping(value = "/productos/cantidad-productos")
	public ResponseEntity<?> getTotalProductos(){
		Integer total = 0;
		Map<String, Object> response = new HashMap<>();

		try {
			total = this.serviceProducto.totalProductos();
		} catch (DataAccessException e) {
			response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<Integer>(total, HttpStatus.OK);
	}

	@Secured(value = { "ROLE_ADMIN", "ROLE_INVENTARIO"})
	@PostMapping(value = "/productos")
	public ResponseEntity<?> create(@RequestBody Producto producto, BindingResult result) {

		Producto newProducto = null;
		Map<String, Object> response = new HashMap<>();

		// manejador de errores desde el backend
		if (result.hasErrors()) {
			List<String> errors = result.getFieldErrors().stream()
					.map(err -> "El campo '".concat(err.getField().concat("' ")).concat(err.getDefaultMessage()))
					.collect(Collectors.toList());

			response.put("errors", errors);
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.BAD_REQUEST);
		}

		try {
			Estado estado = serviceEstado.findById(1);
			producto.setEstado(estado);
			newProducto = serviceProducto.save(producto);
		} catch (DataAccessException e) {
			response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (newProducto == null) {
			response.put("mensaje", "¡El producto no pudo ser registrado!");
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		response.put("mensaje", "¡Producto registrado con éxito!");
		response.put("producto", newProducto);
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}

	@Secured(value = { "ROLE_ADMIN", "ROLE_INVENTARIO" })
	@PutMapping(value = "/productos")
	public ResponseEntity<?> update(@RequestBody Producto producto, BindingResult result) {

		Producto productoUpdated = null;
		Map<String, Object> response = new HashMap<>();

		// Manejo de errores en actualización
		if (result.hasErrors()) {
			List<String> errors = result.getFieldErrors().stream()
					.map(err -> "El campo '".concat(err.getField().concat("' ")).concat(err.getDefaultMessage()))
					.collect(Collectors.toList());

			response.put("errors", errors);
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.BAD_REQUEST);
		}

		if (producto == null) {
			response.put("mensaje", "¡El producto no existe en la base de datos!");
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}

		try {
			productoUpdated = serviceProducto.save(producto);
		} catch (DataAccessException e) {
			response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		response.put("mensaje", "¡El producto ha sido actualizado con éxito!");
		response.put("producto", productoUpdated);
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}

	@Secured(value = { "ROLE_ADMIN" })
	@DeleteMapping(value = "/productos/{id}")
	public ResponseEntity<?> delete(@PathVariable("id") Integer idproducto) {

		Map<String, Object> response = new HashMap<>();

		try {
			Producto producto = serviceProducto.findById(idproducto);
			String nombreImagenAnterior = producto.getImagen();

			// Eliminar foto antigua cuando se sube nueva foto
			serviceUpload.eliminar(nombreImagenAnterior);

			serviceProducto.delete(idproducto);

		} catch (DataAccessException e) {
			response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		response.put("mensaje", "¡El producto ha sido eliminado con éxito!");

		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.OK);
	}

	@Secured(value = { "ROLE_COBRADOR", "ROLE_ADMIN" })
	@PostMapping(value = "/productos/upload")
	public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, @RequestParam("id") Integer id) {

		Producto producto = serviceProducto.findById(id);
		Map<String, Object> response = new HashMap<>();

		// Subir foto a base de datos y archivo a servidor
		if (!file.isEmpty()) {

			String nombreArchivo = null;

			try {
				nombreArchivo = serviceUpload.copiar(file);
			} catch (IOException e) {
				response.put("mensaje", "¡Error al subir la imagen!");
				response.put("error", e.getMessage().concat(": ").concat(e.getCause().getMessage()));
				return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
			}

			String nombreImagenAnterior = producto.getImagen();

			// Eliminar foto antigua cuando se sube nueva foto
			serviceUpload.eliminar(nombreImagenAnterior);

			producto.setImagen(nombreArchivo);

			serviceProducto.save(producto);

		}

		response.put("mensaje", "Imagen subida con éxito");
		response.put("producto", producto);
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.OK);

	}

	@GetMapping(value = "/uploads/img/{nombreImagen:.+}")
	public ResponseEntity<?> verImagen(@PathVariable String nombreImagen) {

		Resource recurso = null;
		Map<String, Object> response = new HashMap<>();

		try {
			recurso = serviceUpload.cargar(nombreImagen);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			response.put("message", "¡Error al subir imagen!");
			response.put("error", e.getMessage().concat(": ").concat(e.getCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		HttpHeaders cabecera = new HttpHeaders();
		cabecera.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + recurso.getFilename() + "\"");

		return new ResponseEntity<Resource>(recurso, cabecera, HttpStatus.OK);
	}
	
	@GetMapping(value = "/productos/name/{name}")
	@ResponseStatus(HttpStatus.OK)
	public List<Producto> findByName(@PathVariable("name") String name){
		return serviceProducto.findByName(name);
	}

	@Secured({ "ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO" })
	@GetMapping(value = "/productos/codigo/{codigo}")
	public ResponseEntity<?> findByCodigo(@PathVariable("codigo") String codigo) {
		
		Producto producto = null;
		Map<String, Object> response = new HashMap<>();
		
		try {
			producto = serviceProducto.findByCodigo(codigo);
		} catch (DataAccessException e) {
			response.put("mensaje", "¡Ha ocurrido un error en la base de datos!");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		if(producto == null) {
			response.put("mensaje", "¡Producto no se encuentra registrado en la base de datos!");
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity<Producto>(producto, HttpStatus.OK);
	}
	
	/*************** PDF REPORTS CONTROLLERS *****************/

	@GetMapping(value = "/productos/expired")	// REPORTE DE PRODUCTOS CADUCADOS
	public String expired() throws FileNotFoundException, JRException, SQLException{
		
		return serviceProducto.reportExpired();
	}

}
