package xyz.pangosoft.dtodo.controller;

import java.util.List;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import xyz.pangosoft.dtodo.model.Pais;
import xyz.pangosoft.dtodo.service.IPaisService;

@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping(value = "/api")
@RequiredArgsConstructor
@Slf4j
public class PaisApiController {

	private final IPaisService servicePais;

	@Secured(value = { "ROLE_ADMIN" })
	@GetMapping(value = "/paises")
	public ResponseEntity<List<Pais>> index() {
		log.info("Listando países registrados");
		return ResponseEntity.ok(servicePais.findAll());
	}

	@Secured(value = { "ROLE_ADMIN" })
	@PostMapping(value = "/paises")
	public ResponseEntity<Pais> create(@Valid @RequestBody Pais pais, BindingResult result) {
		log.info("Registrando país: {}", pais.getNombre());
		return new ResponseEntity<>(servicePais.save(pais), HttpStatus.CREATED);
	}

}
