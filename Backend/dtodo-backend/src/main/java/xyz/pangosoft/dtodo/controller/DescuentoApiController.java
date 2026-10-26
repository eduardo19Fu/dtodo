package xyz.pangosoft.dtodo.controller;

import java.math.BigDecimal;
import java.util.List;

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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.pangosoft.dtodo.dto.CategoriaNivelesDto;
import xyz.pangosoft.dtodo.dto.GuardarDescuentosProductoRequest;
import xyz.pangosoft.dtodo.dto.OpcionesDescuentoDto;
import xyz.pangosoft.dtodo.dto.ProductoDescuentosDto;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.enums.CategoriaClienteEnum;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;
import xyz.pangosoft.dtodo.service.IDescuentoService;

/**
 * Descuentos de los clientes mayoristas. Quien arma una proforma consulta las opciones de cada producto; la
 * configuración (niveles por categoría, precio mayorista y descuentos especiales) es solo del administrador.
 */
@CrossOrigin(origins = { "http://localhost:4200", "https://dtodojalapa.xyz" })
@RestController
@RequestMapping(value = "/api")
@RequiredArgsConstructor
@Slf4j
public class DescuentoApiController {

	private final IDescuentoService serviceDescuento;

	private final IAmbitoVendedorService ambito;

	@Secured(value = { "ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO", "ROLE_VENDEDOR" })
	@GetMapping(value = "/descuentos/opciones")
	public ResponseEntity<OpcionesDescuentoDto> opciones(
			@RequestParam("idProducto") Integer idProducto,
			@RequestParam(value = "idCliente", required = false) Integer idCliente,
			@RequestParam(value = "cantidad", defaultValue = "1") Integer cantidad) {
		if (idCliente != null) {
			ambito.exigirClienteEnAmbito(Cliente.builder().idCliente(idCliente).build());
		}
		return ResponseEntity.ok(serviceDescuento.calcularOpciones(idProducto, idCliente, cantidad == null ? 1 : cantidad));
	}

	@Secured(value = { "ROLE_ADMIN" })
	@GetMapping(value = "/descuentos/categorias")
	public ResponseEntity<List<CategoriaNivelesDto>> categorias() {
		return ResponseEntity.ok(serviceDescuento.findCategorias());
	}

	@Secured(value = { "ROLE_ADMIN" })
	@PutMapping(value = "/descuentos/categorias/{categoria}")
	public ResponseEntity<CategoriaNivelesDto> guardarCategoria(
			@PathVariable("categoria") CategoriaClienteEnum categoria, @RequestBody List<BigDecimal> porcentajes) {
		log.info("Actualizando los niveles de descuento de la categoría {}", categoria);
		return ResponseEntity.ok(serviceDescuento.guardarNiveles(categoria, porcentajes));
	}

	@Secured(value = { "ROLE_ADMIN" })
	@GetMapping(value = "/descuentos/productos")
	public ResponseEntity<Page<ProductoDescuentosDto>> productos(
			@RequestParam(value = "page", defaultValue = "0") Integer page,
			@RequestParam(value = "size", defaultValue = "10") Integer size,
			@RequestParam(value = "filtro", defaultValue = "") String filtro,
			@RequestParam(value = "soloConDescuentos", defaultValue = "false") boolean soloConDescuentos) {
		return ResponseEntity.ok(serviceDescuento.findProductos(filtro, soloConDescuentos,
				PageRequest.of(page, size, Sort.by("nombre"))));
	}

	@Secured(value = { "ROLE_ADMIN" })
	@GetMapping(value = "/descuentos/productos/{idProducto}")
	public ResponseEntity<ProductoDescuentosDto> producto(@PathVariable("idProducto") Integer idProducto) {
		return ResponseEntity.ok(serviceDescuento.findProducto(idProducto));
	}

	@Secured(value = { "ROLE_ADMIN" })
	@PutMapping(value = "/descuentos/productos/{idProducto}")
	public ResponseEntity<ProductoDescuentosDto> guardarProducto(@PathVariable("idProducto") Integer idProducto,
			@RequestBody GuardarDescuentosProductoRequest solicitud) {
		log.info("Actualizando precio mayorista y descuentos del producto {}", idProducto);
		return ResponseEntity.ok(serviceDescuento.guardarProducto(idProducto, solicitud));
	}
}
