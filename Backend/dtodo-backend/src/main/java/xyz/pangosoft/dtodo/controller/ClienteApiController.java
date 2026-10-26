package xyz.pangosoft.dtodo.controller;

import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.pangosoft.dtodo.dto.ClienteDto;
import xyz.pangosoft.dtodo.dto.ClienteFiltro;
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

import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.enums.TipoClienteEnum;
import xyz.pangosoft.dtodo.util.Utils;
import xyz.pangosoft.dtodo.service.IClienteService;

@CrossOrigin(origins = {"http://localhost:4200", "https://dtodojalapa.xyz"})
@RestController
@RequestMapping(value = "/api")
@RequiredArgsConstructor
@Slf4j
public class ClienteApiController {

	private final IClienteService serviceCliente;

	private final IAmbitoVendedorService ambito;

	@GetMapping(value = "/clientes")
	public ResponseEntity<List<ClienteDto>> listarClientes () {
		log.info("Listando Clientes");
		List<ClienteDto> clientes = serviceCliente.findAllDto();
		List<Integer> permitidas = ambito.idsRutasPermitidas();
		if (permitidas != null) {
			clientes = clientes.stream()
					.filter(cliente -> cliente.getIdRuta() != null && permitidas.contains(cliente.getIdRuta()))
					.collect(Collectors.toList());
		}
		return ResponseEntity.ok(clientes);
	}

	@GetMapping(value = "/clientes/listado")
	public ResponseEntity<Page<ClienteDto>> listarClientes(
			@RequestParam(value = "page", defaultValue = "0") Integer page,
			@RequestParam(value = "size", defaultValue = "5") Integer size,
			@RequestParam(value = "filtro", defaultValue = "") String filtro,
			@RequestParam(value = "orden", defaultValue = "nombre") String orden,
			@RequestParam(value = "direccion", defaultValue = "asc") String direccion,
			@RequestParam(value = "tipoCliente", required = false) TipoClienteEnum tipoCliente,
			@RequestParam(value = "idRuta", required = false) Integer idRuta,
			@RequestParam(value = "nitInvalido", defaultValue = "false") boolean nitInvalido) {
		log.info("Listando clientes paginados con filtro");
		Sort.Direction sentido = "desc".equalsIgnoreCase(direccion)
				? Sort.Direction.DESC : Sort.Direction.ASC;
		ClienteFiltro criterios = ClienteFiltro.builder().texto(filtro).tipoCliente(tipoCliente).idRuta(idRuta)
				.idsRutasPermitidas(ambito.idsRutasPermitidas())
				.soloNitInvalido(nitInvalido && Utils.esAdministrador()).build();
		return ResponseEntity.ok(serviceCliente.findListado(criterios,
				PageRequest.of(page, size, Sort.by(sentido, obtenerPropiedadOrden(orden)))));
	}

	private String obtenerPropiedadOrden(String orden) {
		switch (orden == null ? "" : orden.toLowerCase()) {
			case "id": return "idCliente";
			case "nit": return "nit";
			case "telefono": return "telefono";
			case "direccion": return "direccion";
			case "fecha": return "fechaRegistro";
			case "codigo": return "codigoCliente";
			case "tipo": return "tipoCliente";
			default: return "nombre";
		}
	}
	
	@GetMapping(value = "/clientes/page/{page}")
	public ResponseEntity<Page<Cliente>> listarClientesPaginados (@PathVariable("page") Integer page) {
		log.info("Buscando clientes pagina: {}", page);

		rechazarVendedor();

		Page<Cliente> listadoPaginado = null;
		listadoPaginado = serviceCliente.findAll(PageRequest.of(page, 5));
		return ResponseEntity.ok(listadoPaginado);
	}

	@GetMapping(value = "/clientes/nombre/{name}")
	public ResponseEntity<List<Cliente>> findByName(@PathVariable("name") String name) {
		log.info("Buscando clientes con nombre: {}", name);

		rechazarVendedor();

		List<Cliente> listadoClientes = new ArrayList<>();
		listadoClientes = serviceCliente.findByName(name);
		return ResponseEntity.ok(listadoClientes);
	}
	
	@GetMapping(value = "/clientes/nit/{nit}")
	public ResponseEntity<?> findByNit(@PathVariable("nit") String nit){
		log.info("Buscando cliente con nit: {}", nit);

		Cliente cliente = null;
		cliente = ambito.limitarLectura(serviceCliente.findByNit(nit));
		return ResponseEntity.ok(cliente);
	}

	/** Cuántos clientes tienen un NIT escrito que no es válido; sirve para depurar datos antes de facturar. */
	@Secured(value = {"ROLE_ADMIN"})
	@GetMapping(value = "/clientes/cantidad-nit-invalidos")
	public ResponseEntity<Long> cantidadNitInvalidos() {
		return ResponseEntity.ok(serviceCliente.totalNitInvalidos());
	}

	@GetMapping(value = "/clientes/codigo/{codigo}")
	public ResponseEntity<Cliente> findByCodigo(@PathVariable("codigo") String codigo) {
		log.info("Buscando cliente con código: {}", codigo);
		return ResponseEntity.ok(ambito.limitarLectura(serviceCliente.findByCodigo(codigo)));
	}

	@GetMapping(value = "/clientes/{id}")
	public ResponseEntity<?> findById(@PathVariable("id") Integer id) {
		log.info("Buscando cliente con ID: {}", id);

		Cliente cliente = null;
		cliente = ambito.limitarLectura(serviceCliente.findById(id));
		return ResponseEntity.ok(cliente);
	}

	@GetMapping(value = "/clientes/cantidad-clientes")
	public ResponseEntity<Integer> getTotalClientes(){
		log.info("Obteniendo cantidad de clientes");

		Integer totalClientes = 0;
		totalClientes = serviceCliente.totalClientes();
		return ResponseEntity.ok(totalClientes);
	}

	@Secured(value = {"ROLE_COBRADOR","ROLE_ADMIN","ROLE_VENDEDOR"})
	@PostMapping(value = "/clientes")
	public ResponseEntity<Cliente> create(@RequestBody Cliente cliente) {
		log.info("Registrando cliente con NIT: {}", cliente.getNit());

		restringirDatosMayoristas(cliente);
		Cliente newCliente = null;
		newCliente = serviceCliente.save(ambito.prepararAlta(cliente));
		return new ResponseEntity<>(newCliente, HttpStatus.CREATED);
	}
	
	/** Los listados sin alcance por ruta no se ofrecen al vendedor: usa el listado filtrado o la búsqueda por NIT. */
	private void rechazarVendedor() {
		if (ambito.esVendedor()) {
			throw new AccessDeniedException("Esta consulta no está disponible para el rol de vendedor.");
		}
	}

	/**
	 * Convertir a un cliente en mayorista (tipo, categoría, ruta y crédito) es una decisión administrativa:
	 * quien no es administrador solo puede registrar clientes minoristas.
	 */
	static void restringirDatosMayoristas(Cliente cliente) {
		if (Utils.esAdministrador() || Utils.esVendedorAcotado()) {
			return;
		}
		cliente.setTipoCliente(TipoClienteEnum.MINORISTA);
		cliente.setCategoriaDescuento(null);
		cliente.setRuta(null);
		cliente.setLimiteCredito(null);
		cliente.setDiasCredito(null);
	}

	@Secured(value = {"ROLE_ADMIN", "ROLE_VENDEDOR"})
	@SuppressWarnings("null")
	@PutMapping(value = {"/clientes", "/clientes/update/{id}"})
	public ResponseEntity<Cliente> update(@RequestBody Cliente cliente, @PathVariable(value = "id", required = false) Integer idcliente) {
		log.info("Actualizando cliente: ", cliente.getIdCliente());

		Cliente clienteUpdated = null;
		clienteUpdated = serviceCliente.save(ambito.prepararEdicion(cliente));
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
