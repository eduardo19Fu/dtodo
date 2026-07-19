package xyz.pangosoft.dtodo.controller;

import java.util.List;

import javax.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import xyz.pangosoft.dtodo.model.Role;
import xyz.pangosoft.dtodo.service.IRoleService;

@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz", "http://dtodojalapa.xyz" })
@RestController
@RequestMapping(value = "/api")
@RequiredArgsConstructor
@Slf4j
public class RoleApiController {

	private final IRoleService serviceRole;

	@GetMapping(value = "/roles")
	public ResponseEntity<List<Role>> index() {
		log.info("Listando roles");
		return ResponseEntity.ok(serviceRole.findAll());
	}

	@GetMapping(value = "/roles/page/{page}")
	public ResponseEntity<Page<Role>> index(@PathVariable("page") Integer page) {
		log.info("Listando roles pagina: {}", page);
		return ResponseEntity.ok(serviceRole.findAll(PageRequest.of(page, 5)));
	}

	@Secured(value = { "ROLE_ADMIN" })
	@GetMapping(value = "/roles/name/{role}")
	public ResponseEntity<Role> buscarPorNombre(@PathVariable("role") String role) {
		log.info("Buscando rol con nombre: {}", role);
		return ResponseEntity.ok(serviceRole.findByName(role));
	}

	@Secured(value = { "ROLE_ADMIN" })
	@GetMapping(value = "/roles/{id}")
	public ResponseEntity<Role> getById(@PathVariable("id") Integer idRole) {
		log.info("Buscando rol con ID: {}", idRole);
		return ResponseEntity.ok(serviceRole.findById(idRole));
	}

	@Secured(value = { "ROLE_ADMIN" })
	@PostMapping(value = "/roles")
	public ResponseEntity<Role> create(@Valid @RequestBody Role role) {
		log.info("Registrando rol: {}", role.getRole());
		return new ResponseEntity<>(serviceRole.save(role), HttpStatus.CREATED);
	}

}
