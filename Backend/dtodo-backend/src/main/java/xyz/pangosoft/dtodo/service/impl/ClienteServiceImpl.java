package xyz.pangosoft.dtodo.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import xyz.pangosoft.dtodo.dto.ClienteDto;
import xyz.pangosoft.dtodo.error.exceptions.NoContentException;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import xyz.pangosoft.dtodo.model.Cliente;
import java.util.Set;
import xyz.pangosoft.dtodo.util.NitValidator;
import xyz.pangosoft.dtodo.dto.ClienteFiltro;
import xyz.pangosoft.dtodo.repository.IRutaRepository;
import xyz.pangosoft.dtodo.model.enums.TipoClienteEnum;
import xyz.pangosoft.dtodo.model.Ruta;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.repository.IClienteRepository;
import xyz.pangosoft.dtodo.service.IClienteService;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClienteServiceImpl implements IClienteService {

	private final IClienteRepository clienteRepository;
	private final IRutaRepository rutaRepository;

	@Transactional(readOnly = true)
	@Override
	public List<Cliente> findAll() {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		List<Cliente> clientes = new ArrayList<>();
		log.debug("Enter {}", __method);

		try {
			log.info("Listando Clientes Registrados");
			clientes = clienteRepository.findAll();

			if(clientes != null && clientes.size() > 0) {
				log.info("Listado de clientes obtenidos: ", clientes.size());
				return clientes;
			} else {
				log.warn("No existen clientes registrados");
				throw new NoContentException("No existe ningún cliente registrado en la base de datos");
			}
		} catch (Exception e) {
			log.error("Un error ha ocurrido => {}", e);
			throw new RuntimeException("Error => ", e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public List<ClienteDto> findAllDto() {
		List<ClienteDto> clientes = new ArrayList<>();
		try {
			log.info("Devolviendo listado de clientes...");
			clientes = clienteRepository.consultarClientesDto();

			if (clientes != null && clientes.size() > 0) {
				log.info("Listado de clientes obtenidos: ", clientes.size());
				return clientes;
			} else {
				log.warn("No existen clientes registrados");
				throw new NoContentException("No existe ningún cliente registrado en la base de datos");
			}
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos", e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Page<ClienteDto> findListado(String filtro, Pageable pageable) {
		return findListado(filtro, null, null, pageable);
	}

	@Transactional(readOnly = true)
	@Override
	public Page<ClienteDto> findListado(String filtro, TipoClienteEnum tipoCliente, Integer idRuta, Pageable pageable) {
		return findListado(ClienteFiltro.builder().texto(filtro).tipoCliente(tipoCliente).idRuta(idRuta).build(), pageable);
	}

	@Transactional(readOnly = true)
	@Override
	public Page<ClienteDto> findListado(ClienteFiltro filtro, Pageable pageable) {
		try {
			log.info("Listando clientes paginados con filtro");
			List<String> terminos = obtenerTerminosBusqueda(filtro.getTexto());
			Set<Integer> idsNitInvalido = filtro.isSoloNitInvalido() ? idsConNitInvalido() : null;
			Page<Cliente> clientes = clienteRepository.findAll(
					crearEspecificacionBusqueda(terminos, filtro, idsNitInvalido), pageable);
			return clientes.map(this::mapClienteToDto);
		} catch (DataAccessException e) {
			log.error("Error al consultar el listado paginado de clientes", e);
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
					"Ha ocurrido un error al consultar los clientes", e);
		}
	}

	/**
	 * Ids de los clientes con un NIT escrito que no es válido (un teléfono, un dígito verificador incorrecto...).
	 * Los que no tienen NIT no cuentan: es opcional hasta que se factura. El NIT se valida en memoria porque el
	 * dígito verificador no se puede calcular en la consulta.
	 */
	@Transactional(readOnly = true)
	Set<Integer> idsConNitInvalido() {
		return clienteRepository.findIdsYNit().stream()
				.filter(fila -> fila[1] != null && !((String) fila[1]).trim().isEmpty())
				.filter(fila -> !NitValidator.esValido((String) fila[1]))
				.map(fila -> (Integer) fila[0])
				.collect(Collectors.toSet());
	}

	@Transactional(readOnly = true)
	@Override
	public long totalNitInvalidos() {
		return idsConNitInvalido().size();
	}

	static List<String> obtenerTerminosBusqueda(String filtro) {
		if (filtro == null || filtro.trim().isEmpty()) {
			return new ArrayList<>();
		}

		return Arrays.stream(filtro.trim().toLowerCase(Locale.ROOT).split("\\s+"))
				.distinct()
				.collect(Collectors.toList());
	}

	private Specification<Cliente> crearEspecificacionBusqueda(List<String> terminos, ClienteFiltro filtro,
			Set<Integer> idsNitInvalido) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> condiciones = new ArrayList<>();

			for (String termino : terminos) {
				String patron = "%" + escaparLike(termino) + "%";
				condiciones.add(criteriaBuilder.or(
						criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("nombre"), "")), patron, '\\'),
						criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("nit"), "")), patron, '\\'),
						criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("telefono"), "")), patron, '\\'),
						criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("direccion"), "")), patron, '\\'),
						criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("codigoCliente"), "")), patron, '\\'),
						criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("nombreNegocio"), "")), patron, '\\'),
						criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("contacto"), "")), patron, '\\'),
						criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("telefonoContacto"), "")), patron, '\\')));
			}

			if (filtro.getTipoCliente() != null) {
				condiciones.add(criteriaBuilder.equal(root.get("tipoCliente"), filtro.getTipoCliente()));
			}
			if (filtro.getIdRuta() != null) {
				condiciones.add(criteriaBuilder.equal(root.get("ruta").get("idRuta"), filtro.getIdRuta()));
			}
			if (filtro.getIdsRutasPermitidas() != null) {
				condiciones.add(filtro.getIdsRutasPermitidas().isEmpty() ? criteriaBuilder.disjunction()
						: root.get("ruta").get("idRuta").in(filtro.getIdsRutasPermitidas()));
			}
			if (idsNitInvalido != null) {
				condiciones.add(idsNitInvalido.isEmpty() ? criteriaBuilder.disjunction()
						: root.get("idCliente").in(idsNitInvalido));
			}

			return condiciones.isEmpty() ? criteriaBuilder.conjunction()
					: criteriaBuilder.and(condiciones.toArray(new Predicate[0]));
		};
	}

	private String escaparLike(String valor) {
		return valor.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private ClienteDto mapClienteToDto(Cliente cliente) {
		return new ClienteDto(
				cliente.getIdCliente(),
				cliente.getNombre(),
				cliente.getNit(),
				cliente.getDireccion(),
				cliente.getFechaRegistro(),
				cliente.getTelefono(),
				cliente.getCodigoCliente(),
				cliente.getNombreNegocio(),
				cliente.getTipoCliente(),
				cliente.getCategoriaDescuento(),
				cliente.getRuta() == null ? null : cliente.getRuta().getIdRuta(),
				cliente.getRuta() == null ? null : cliente.getRuta().getNombre(),
				cliente.getContacto(),
				cliente.getTelefonoContacto(),
				cliente.getReferenciaUbicacion(),
				cliente.getLimiteCredito(),
				cliente.getDiasCredito());
	}

	@Transactional(readOnly = true)
	@Override
	public Page<Cliente> findAll(Pageable pageable) {

		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		Page <Cliente> clientes = null;
		log.debug("Enter {}", __method);

		try {
			log.info("Listando Clientes Registrados");
			clientes = clienteRepository.findAll(pageable);

			if(clientes != null && clientes.hasContent()) {
				log.info("Listado de clientes obtenidos: ", clientes.getTotalElements());
				return clientes;
			} else {
				log.warn("No existen clientes registrados");
				throw new NoContentException("No existe ningún cliente registrado en la base de datos");
			}
		} catch (Exception e) {
			log.error("Un error ha ocurrido => {}", e);
			throw new RuntimeException("Error => ", e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Cliente findById(Integer idcliente) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		Optional<Cliente> cliente = null;
		log.debug("Enter {}", __method);

		try {
			log.info("Listando Clientes Registrados por Nombre");
			cliente = clienteRepository.findById(idcliente);

			if(cliente.isPresent()) {
				log.info("Listado de clientes con nombre {} obtenidos: {}");
				return cliente.get();
			} else {
				log.warn("No existen clientes registrados con el ID: {}", idcliente);
				throw new NoContentException("No existe ningún cliente registrado en la base de datos con el ID: " + idcliente);
			}
		} catch (Exception e) {
			log.error("Un error ha ocurrido => {}", e);
			throw new RuntimeException("Error => ", e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public List<Cliente> findByName(String nombre) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		List<Cliente> clientes = new ArrayList<>();
		log.debug("Enter {}", __method);

		try {
			log.info("Listando Clientes Registrados por Nombre");
			clientes = clienteRepository.findByNombre(nombre);

			if(!clientes.isEmpty()) {
				log.info("Listado de clientes con nombre {} obtenidos: {}", nombre, clientes.size());
				return clientes;
			} else {
				log.warn("No existen clientes registrados con el nombre: {}", nombre);
				throw new NoContentException("No existe ningún cliente registrado en la base de datos con el nombre: " + nombre);
			}
		} catch (Exception e) {
			log.error("Un error ha ocurrido => {}", e);
			throw new RuntimeException("Error => ", e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Cliente findByNit(String nit) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		Optional<Cliente> cliente = null;
		log.debug("Enter {}", __method);

		try {
			log.info("Listando Clientes Registrados por Nombre");
			cliente = clienteRepository.findByNit(nit);

			if(cliente.isPresent()) {
				log.info("Cliente con nit: {}", nit);
				return cliente.get();
			} else {
				log.error("Cliente con NIT: {}  no se encuentra registrado", nit);
				throw new NotFoundException("Cliente con NIT: " + nit +  " no se encuentra registrado");
			}
		} catch(DataAccessException e) {
			log.error("Ha ocurrido un error a nivel de base de datos: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException("Error al acceder a la base de datos", e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Cliente findByCodigo(String codigoCliente) {
		String codigo = codigoCliente == null ? "" : codigoCliente.trim();
		return clienteRepository.findByCodigoCliente(codigo)
				.orElseThrow(() -> new NotFoundException("Cliente con código: " + codigo + " no se encuentra registrado"));
	}

	@Transactional(readOnly = true)
	@Override
	public Integer totalClientes() {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		try {
			log.info("Consultado cantidad de clientes");
			return clienteRepository.getCantClientes() == null ? 0 : clienteRepository.getCantClientes();
		} catch(Exception e) {
			log.error("Error Inesperado => {}", e);
			throw new RuntimeException("Error => ", e);
		} finally {
			log.debug("{} Exit", __method);
		}
	}

	@Transactional
	@Override
	public Cliente save(Cliente cliente) {
		try {
			if (cliente.getIdCliente() == null) {
				log.info("Registrando nuevo cliente");
				normalizar(cliente);
				// El código lo asigna el servidor: un UUID que nunca se repite ni cambia.
				cliente.setCodigoCliente(nuevoCodigo());
				cliente.setRuta(resolverRuta(cliente, null));
				if (cliente.getFechaRegistro() == null) {
					cliente.setFechaRegistro(LocalDateTime.now());
				}
				return clienteRepository.save(cliente);
			}

			log.info("Actualizando registro de cliente con ID: {}", cliente.getIdCliente());
			Cliente existente = clienteRepository.findById(cliente.getIdCliente()).orElseThrow(() -> new NotFoundException(
					"Cliente con ID: " + cliente.getIdCliente() + " no se encuentra registrado"));
			normalizar(cliente);
			Ruta ruta = resolverRuta(cliente, existente.getRuta());

			// Solo se copian los campos editables: la fecha de registro nunca la manda el cliente.
			existente.setNit(cliente.getNit());
			existente.setNombre(cliente.getNombre());
			existente.setDireccion(cliente.getDireccion());
			existente.setTelefono(cliente.getTelefono());
			// El código no se edita: lo que mande la petición se ignora. Un cliente anterior que no lo tenga lo recibe ahora.
			if (existente.getCodigoCliente() == null) {
				existente.setCodigoCliente(nuevoCodigo());
			}
			existente.setNombreNegocio(cliente.getNombreNegocio());
			existente.setTipoCliente(cliente.getTipoCliente());
			existente.setCategoriaDescuento(cliente.getCategoriaDescuento());
			existente.setRuta(ruta);
			existente.setContacto(cliente.getContacto());
			existente.setTelefonoContacto(cliente.getTelefonoContacto());
			existente.setReferenciaUbicacion(cliente.getReferenciaUbicacion());
			existente.setLimiteCredito(cliente.getLimiteCredito());
			existente.setDiasCredito(cliente.getDiasCredito());
			return clienteRepository.save(existente);
		} catch (DataAccessException e) {
			log.error("Error de base de datos: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(e.getMessage(), e);
		}
	}

	/**
	 * Deja el cliente consistente antes de validarlo: tipo por defecto, texto vacío como nulo y, para un
	 * minorista, sin ruta ni categoría (esos datos solo tienen sentido en un mayorista).
	 */
	static void normalizar(Cliente cliente) {
		if (cliente.getTipoCliente() == null) {
			cliente.setTipoCliente(TipoClienteEnum.MINORISTA);
		}
		cliente.setCodigoCliente(textoONulo(cliente.getCodigoCliente()));
		cliente.setNombreNegocio(textoONulo(cliente.getNombreNegocio()));
		cliente.setContacto(textoONulo(cliente.getContacto()));
		cliente.setTelefonoContacto(textoONulo(cliente.getTelefonoContacto()));
		cliente.setReferenciaUbicacion(textoONulo(cliente.getReferenciaUbicacion()));

		if (cliente.getLimiteCredito() != null && cliente.getLimiteCredito().signum() < 0) {
			throw new ReglaNegocioException("El límite de crédito no puede ser negativo.");
		}
		if (cliente.getDiasCredito() != null && cliente.getDiasCredito() < 0) {
			throw new ReglaNegocioException("Los días de crédito no pueden ser negativos.");
		}

		if (cliente.getTipoCliente() == TipoClienteEnum.MINORISTA) {
			cliente.setRuta(null);
			cliente.setCategoriaDescuento(null);
		}
	}

	private static String textoONulo(String texto) {
		if (texto == null || texto.trim().isEmpty()) {
			return null;
		}
		return texto.trim();
	}

	private String nuevoCodigo() {
		String codigo;
		do {
			codigo = UUID.randomUUID().toString();
		} while (clienteRepository.findByCodigoCliente(codigo).isPresent());
		return codigo;
	}

	/**
	 * Un mayorista debe tener una ruta existente y activa. Si el cliente ya estaba en esa ruta se acepta aunque
	 * hoy esté inactiva, para poder seguir editando sus demás datos.
	 */
	private Ruta resolverRuta(Cliente cliente, Ruta rutaActual) {
		if (cliente.getTipoCliente() != TipoClienteEnum.MAYORISTA) {
			return null;
		}
		if (cliente.getRuta() == null || cliente.getRuta().getIdRuta() == null) {
			throw new ReglaNegocioException("Un cliente mayorista debe tener una ruta.");
		}
		Ruta ruta = rutaRepository.findById(cliente.getRuta().getIdRuta())
				.orElseThrow(() -> new ReglaNegocioException("La ruta seleccionada no existe."));
		boolean mismaRuta = rutaActual != null && rutaActual.getIdRuta().equals(ruta.getIdRuta());
		if (!mismaRuta && (ruta.getEstado() == null || !"ACTIVO".equals(ruta.getEstado().getEstado()))) {
			throw new ReglaNegocioException("La ruta " + ruta.getNombre() + " está inactiva.");
		}
		return ruta;
	}

	@Transactional
	@Override
	public void delete(Cliente cliente) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		try {
			if(cliente != null) {
				log.info("Eliminar cliente con ID: {}", cliente.getIdCliente());
				clienteRepository.deleteById(cliente.getIdCliente());
			} else {
				log.error("Cliente para eliminar no registrado");
				throw new NotFoundException("Cliente para eliminar no registrado");
			}
		} catch (DataAccessException e) {
			log.error("Ha ocurrido un error desconocido", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(e.getMessage(), e);
		} finally {
			log.debug("{} Exit", __method);
		}
	}

}
