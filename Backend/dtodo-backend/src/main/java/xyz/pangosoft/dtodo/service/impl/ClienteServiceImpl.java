package xyz.pangosoft.dtodo.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import xyz.pangosoft.dtodo.repository.IClienteRepository;
import xyz.pangosoft.dtodo.service.IClienteService;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClienteServiceImpl implements IClienteService {

	private final IClienteRepository clienteRepository;

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
		try {
			log.info("Listando clientes paginados con filtro");
			List<String> terminos = obtenerTerminosBusqueda(filtro);
			Page<Cliente> clientes = clienteRepository.findAll(
					crearEspecificacionBusqueda(terminos), pageable);
			return clientes.map(this::mapClienteToDto);
		} catch (DataAccessException e) {
			log.error("Error al consultar el listado paginado de clientes", e);
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(
					"Ha ocurrido un error al consultar los clientes", e);
		}
	}

	static List<String> obtenerTerminosBusqueda(String filtro) {
		if (filtro == null || filtro.trim().isEmpty()) {
			return new ArrayList<>();
		}

		return Arrays.stream(filtro.trim().toLowerCase(Locale.ROOT).split("\\s+"))
				.distinct()
				.collect(Collectors.toList());
	}

	private Specification<Cliente> crearEspecificacionBusqueda(List<String> terminos) {
		return (root, query, criteriaBuilder) -> {
			if (terminos.isEmpty()) {
				return criteriaBuilder.conjunction();
			}

			List<Predicate> coincidencias = new ArrayList<>();
			for (String termino : terminos) {
				String patron = "%" + escaparLike(termino) + "%";
				coincidencias.add(criteriaBuilder.or(
						criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("nombre"), "")), patron, '\\'),
						criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("nit"), "")), patron, '\\'),
						criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("telefono"), "")), patron, '\\'),
						criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("direccion"), "")), patron, '\\')));
			}

			return criteriaBuilder.and(coincidencias.toArray(new Predicate[0]));
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
				cliente.getTelefono());
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
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		Cliente newCliente = null;
		log.debug("Enter {}", __method);

		if (cliente.getFechaRegistro() == null) {
			cliente.setFechaRegistro(LocalDateTime.now());
		}

		try {
			log.info("Registrando nuevo cliente");

			if(cliente.getIdCliente() != null) {
				log.info("Actuliazando registro de cliente con ID: {}", cliente.getIdCliente());
				Cliente clienteActualizado = new Cliente();
				clienteActualizado.setIdCliente(cliente.getIdCliente());
				clienteActualizado.setNit(cliente.getNit());
				clienteActualizado.setNombre(cliente.getNombre());
				clienteActualizado.setDireccion(cliente.getDireccion());
				clienteActualizado.setTelefono(cliente.getTelefono());

				newCliente = clienteRepository.save(clienteActualizado);
			}

			newCliente = clienteRepository.save(cliente);
		} catch (DataAccessException e) {
			log.error("Error de base de datos: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(e.getMessage(), e);
		}
		return newCliente;
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
