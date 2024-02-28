package com.aglayatech.licorstore.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.aglayatech.licorstore.error.exceptions.NoContentException;
import com.aglayatech.licorstore.error.exceptions.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import com.aglayatech.licorstore.model.Cliente;
import com.aglayatech.licorstore.repository.IClienteRepository;
import com.aglayatech.licorstore.service.IClienteService;

@Service
@Slf4j
public class ClienteServiceImpl implements IClienteService {

	@Autowired
	private IClienteRepository clienteRepository;

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

	@Override
	public List<Cliente> findByName(String nombre) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		List<Cliente> clientes = new ArrayList<>();
		log.debug("Enter {}", __method);

		try {
			log.info("Listando Clientes Registrados por Nombre");
			clientes = clienteRepository.findByNombre(nombre);

			if(clientes != null && clientes.size() > 0) {
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
				log.error("No existen clientes registrados con el nit: {}", nit);
				throw new NotFoundException("No existe ningún cliente registrado en la base de datos con el nit: " + nit);
			}
		} catch (Exception e) {
			log.error("Un error ha ocurrido => {}", e);
			throw new RuntimeException("Error => ", e);
		}
	}

	@Override
	public Integer totalClientes() {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		log.debug("Enter {}", __method);

		try {
			log.info("Consultado cantidad de clientes");
			return this.clienteRepository.getCantClientes() == null ? 0 : this.clienteRepository.getCantClientes();
		} catch(Exception e) {
			log.error("Error Inesperado => {}", e);
			throw new RuntimeException("Error => ", e);
		} finally {
			log.debug("{} Exit", __method);
		}
	}

	@Override
	public Cliente save(Cliente cliente) {
		String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
		Cliente newCliente = null;
		log.debug("Enter {}", __method);

		try {
			log.info("Registrando nuevo cliente");

			if(cliente.getIdCliente() != null) {
				log.info("Actuliazando registro de cliente con ID: ", cliente.getIdCliente());
				newCliente.setIdCliente(cliente.getIdCliente());
				newCliente.setNombre(cliente.getNombre());
				newCliente.setNit(cliente.getNit());
				newCliente.setDireccion(cliente.getDireccion());
				newCliente.setTelefono(cliente.getTelefono());
			}

			newCliente = clienteRepository.save(cliente);
		} catch (DataAccessException e) {
			log.error("Error de base de datos: {}", e.getMessage());
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException(e.getMessage(), e);
		}
		return newCliente;
	}

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
			throw new com.aglayatech.licorstore.error.exceptions.DataAccessException(e.getMessage(), e);
		} finally {
			log.debug("{} Exit", __method);
		}
	}

}
