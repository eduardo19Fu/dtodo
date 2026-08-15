package xyz.pangosoft.dtodo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RequestParam;

import xyz.pangosoft.dtodo.dto.UsuarioDto;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.service.IUsuarioService;

@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz", "http://dtodojalapa.xyz" })
@RestController
@RequestMapping(value = "/api")
@RequiredArgsConstructor
@Slf4j
public class UsuarioApiController {

	private final IUsuarioService serviceUsuario;

	@Secured(value = { "ROLE_ADMIN", "ROLE_COBRADOR" })
	@GetMapping(value = "/usuarios")
	public ResponseEntity<List<Usuario>> index() {
		log.info("Listando usuarios");
		return ResponseEntity.ok(serviceUsuario.findAll());
	}

	@GetMapping(value = "/usuarios/cajero")
	public ResponseEntity<List<Usuario>> findCajeros() {
		log.info("Listando usuarios cajeros");
		return ResponseEntity.ok(serviceUsuario.cajeros());
	}

	@GetMapping(value = "/usuarios/page/{page}")
	public ResponseEntity<Page<Usuario>> index(@PathVariable("page") Integer page) {
		log.info("Listando usuarios pagina: {}", page);
		return ResponseEntity.ok(serviceUsuario.findAll(PageRequest.of(page, 5)));
	}

	@Secured(value = { "ROLE_ADMIN" })
	@GetMapping(value = "/usuarios/listado")
	public ResponseEntity<Page<UsuarioDto>> listado(
			@RequestParam(value = "page", defaultValue = "0") Integer page,
			@RequestParam(value = "size", defaultValue = "5") Integer size,
			@RequestParam(value = "filtro", defaultValue = "") String filtro,
			@RequestParam(value = "orden", defaultValue = "id") String orden,
			@RequestParam(value = "direccion", defaultValue = "asc") String direccion) {
		Sort.Direction sentido = "desc".equalsIgnoreCase(direccion)
				? Sort.Direction.DESC : Sort.Direction.ASC;
		return ResponseEntity.ok(serviceUsuario.findListado(
				filtro, PageRequest.of(page, size, Sort.by(sentido, obtenerPropiedadOrden(orden)))));
	}

	private String obtenerPropiedadOrden(String orden) {
		switch (orden == null ? "" : orden.toLowerCase()) {
			case "usuario":
				return "usuario";
			case "nombre":
				return "primerNombre";
			case "apellido":
				return "apellido";
			case "fecha":
				return "fechaRegistro";
			case "estado":
				return "enabled";
			default:
				return "idUsuario";
		}
	}

	@Secured(value = { "ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO" })
	@GetMapping(value = "/usuarios/{id}")
	public ResponseEntity<Usuario> findById(@PathVariable("id") Integer idUsuario) {
		log.info("Buscando usuario con ID: {}", idUsuario);
		return ResponseEntity.ok(serviceUsuario.findById(idUsuario));
	}

	@Secured(value = { "ROLE_ADMIN" })
	@GetMapping(value = "/usuarios/cantidad-usuarios")
	public ResponseEntity<Integer> getTotalUsuarios() {
		log.info("Obteniendo cantidad de usuarios");
		return ResponseEntity.ok(serviceUsuario.totalUsuarios());
	}

	@Secured(value = { "ROLE_ADMIN" })
	@PostMapping(value = "/usuarios")
	public ResponseEntity<Usuario> create(@Valid @RequestBody Usuario usuario) {
		log.info("Registrando usuario: {}", usuario.getUsuario());
		return new ResponseEntity<>(serviceUsuario.save(usuario), HttpStatus.CREATED);
	}

	@Secured(value = { "ROLE_ADMIN" })
	@PutMapping(value = "/usuarios/{id}")
	public ResponseEntity<Usuario> update(@Valid @RequestBody Usuario usuario, @PathVariable("id") Integer idUsuario) {
		log.info("Actualizando usuario con ID: {}", idUsuario);
		return new ResponseEntity<>(serviceUsuario.update(usuario, idUsuario), HttpStatus.CREATED);
	}

	@Secured(value = { "ROLE_ADMIN" })
	@DeleteMapping(value = "/usuarios/{id}")
	public ResponseEntity<Map<String, Object>> delete(@PathVariable("id") Integer idUsuario) {
		log.info("Eliminando usuario con ID: {}", idUsuario);

		serviceUsuario.delete(idUsuario);

		Map<String, Object> response = new HashMap<>();
		response.put("mensaje", "¡Usuario eliminado con éxito!");
		response.put("idusuario", idUsuario);
		return ResponseEntity.ok(response);
	}

}
