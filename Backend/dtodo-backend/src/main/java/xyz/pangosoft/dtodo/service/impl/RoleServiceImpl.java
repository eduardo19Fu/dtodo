package xyz.pangosoft.dtodo.service.impl;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import xyz.pangosoft.dtodo.error.exceptions.NoContentException;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Role;
import xyz.pangosoft.dtodo.repository.IRoleRepository;
import xyz.pangosoft.dtodo.service.IRoleService;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl implements IRoleService {

	private final IRoleRepository repoRole;

	@Override
	@Transactional(readOnly = true)
	public List<Role> findAll() {
		try {
			log.info("Listando roles registrados");
			List<Role> roles = repoRole.findAll(Sort.by(Direction.ASC, "idRole"));

			if (roles.isEmpty()) {
				log.warn("No existen roles registrados");
				throw new NoContentException("No existe ningún rol registrado en la base de datos");
			}
			return roles;
		} catch (DataAccessException e) {
			log.error("Error de base de datos al listar roles: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(e.getMessage(), e);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Role> findAll(Pageable pageable) {
		try {
			log.info("Listando roles paginados");
			return repoRole.findAll(pageable);
		} catch (DataAccessException e) {
			log.error("Error de base de datos al paginar roles: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(e.getMessage(), e);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Role findById(Integer idRole) {
		try {
			log.info("Buscando rol con ID: {}", idRole);
			return repoRole.findById(idRole)
					.orElseThrow(() -> {
						log.warn("El rol con ID {} no se encuentra registrado", idRole);
						return new NotFoundException("El rol con ID " + idRole + " no se encuentra registrado en la base de datos");
					});
		} catch (DataAccessException e) {
			log.error("Error de base de datos al buscar rol: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(e.getMessage(), e);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Role findByName(String role) {
		try {
			log.info("Buscando rol con nombre: {}", role);
			Role foundRole = repoRole.getRoles(role);

			if (foundRole == null) {
				log.warn("El rol '{}' no se encuentra registrado", role);
				throw new NotFoundException("El rol '" + role + "' no se encuentra registrado en la base de datos");
			}
			return foundRole;
		} catch (DataAccessException e) {
			log.error("Error de base de datos al buscar rol por nombre: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(e.getMessage(), e);
		}
	}

	@Override
	@Transactional
	public Role save(Role role) {
		try {
			log.info("Registrando rol: {}", role.getRole());
			return repoRole.save(role);
		} catch (DataAccessException e) {
			log.error("Error de base de datos al registrar rol: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(e.getMessage(), e);
		}
	}

}
