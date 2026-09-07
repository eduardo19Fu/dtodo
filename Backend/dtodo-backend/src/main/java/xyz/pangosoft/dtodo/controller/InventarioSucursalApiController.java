package xyz.pangosoft.dtodo.controller;

import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import xyz.pangosoft.dtodo.dto.InventarioSucursalDto;
import xyz.pangosoft.dtodo.model.InventarioSucursal;
import xyz.pangosoft.dtodo.service.IInventarioSucursalService;

@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping(value = "/api")
@RequiredArgsConstructor
@Slf4j
public class InventarioSucursalApiController {

	private final IInventarioSucursalService serviceInventario;

	@Secured(value = { "ROLE_ADMIN", "ROLE_INVENTARIO" })
	@GetMapping(value = "/inventario-sucursal/{idSucursal}/listado")
	public ResponseEntity<Page<InventarioSucursalDto>> listado(
			@PathVariable("idSucursal") Integer idSucursal,
			@RequestParam(value = "page", defaultValue = "0") Integer page,
			@RequestParam(value = "size", defaultValue = "10") Integer size,
			@RequestParam(value = "filtro", defaultValue = "") String filtro) {
		log.info("Listando inventario de la sucursal: {}", idSucursal);
		// El Sort se aplica contra la entidad InventarioSucursal (alias "i" en el @Query), por lo que debe
		// usar la ruta de asociación real "producto.nombre" y no el nombre del campo del DTO proyectado.
		return ResponseEntity.ok(serviceInventario.findListado(
				idSucursal, filtro, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "producto.nombre"))));
	}

	@Secured(value = { "ROLE_ADMIN", "ROLE_INVENTARIO" })
	@PutMapping(value = "/inventario-sucursal/{idSucursal}/{idProducto}")
	public ResponseEntity<Map<String, Object>> ajustarStock(
			@PathVariable("idSucursal") Integer idSucursal,
			@PathVariable("idProducto") Integer idProducto,
			@RequestBody Map<String, Integer> body) {
		log.info("Ajustando manualmente el stock del producto {} en la sucursal {}", idProducto, idSucursal);

		InventarioSucursal actualizado = serviceInventario.ajustarStock(
				idSucursal, idProducto, body.get("stock"), body.get("stockMinimo"));

		Map<String, Object> response = new HashMap<>();
		response.put("mensaje", "Stock actualizado con éxito");
		response.put("inventario", actualizado);
		return ResponseEntity.ok(response);
	}

}
