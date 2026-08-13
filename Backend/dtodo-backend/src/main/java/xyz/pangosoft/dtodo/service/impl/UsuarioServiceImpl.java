package xyz.pangosoft.dtodo.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import xyz.pangosoft.dtodo.error.exceptions.NoContentException;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Role;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.repository.IUsuarioRepository;
import xyz.pangosoft.dtodo.service.IUsuarioService;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioServiceImpl implements UserDetailsService, IUsuarioService {

	private final IUsuarioRepository repoUsuario;

	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		Usuario usuario = repoUsuario.findByUsuario(username);

		if (usuario == null) {
			log.error("Error: no existe el usuario '{}' en el sistema.", username);
			throw new UsernameNotFoundException("Error: no existe el usuario en el sistema.");
		}

		List<GrantedAuthority> authorities = usuario.getRoles()
				.stream()
				.map(role -> new SimpleGrantedAuthority(role.getRole()))
				.peek(authority -> log.info("Role: {}", authority.getAuthority()))
				.collect(Collectors.toList());

		return new User(usuario.getUsuario(), usuario.getPassword(), usuario.isEnabled(), true, true, true, authorities);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Usuario> findAll() {
		try {
			log.info("Listando usuarios registrados");
			List<Usuario> usuarios = repoUsuario.findAll(Sort.by(Direction.ASC, "idUsuario"));

			if (usuarios.isEmpty()) {
				log.warn("No existen usuarios registrados");
				throw new NoContentException("No existe ningún usuario registrado en la base de datos");
			}
			return usuarios;
		} catch (DataAccessException e) {
			log.error("Error de base de datos al listar usuarios: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(e.getMessage(), e);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Usuario> findAll(Pageable pageable) {
		try {
			log.info("Listando usuarios paginados");
			return repoUsuario.findAll(pageable);
		} catch (DataAccessException e) {
			log.error("Error de base de datos al paginar usuarios: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(e.getMessage(), e);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Usuario findById(Integer idUsuario) {
		try {
			log.info("Buscando usuario con ID: {}", idUsuario);
			return repoUsuario.findById(idUsuario)
					.orElseThrow(() -> {
						log.warn("El usuario con ID {} no se encuentra registrado", idUsuario);
						return new NotFoundException("El usuario con ID " + idUsuario + " no se encuentra registrado en la base de datos");
					});
		} catch (DataAccessException e) {
			log.error("Error de base de datos al buscar usuario: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(e.getMessage(), e);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Usuario findByUsuario(String usuario) {
		return repoUsuario.findByUsuario(usuario);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean matchesPassword(String usuario, String rawPassword) {
		if (usuario == null || rawPassword == null || rawPassword.isEmpty()) {
			return false;
		}

		Usuario usuarioActual = repoUsuario.findByUsuario(usuario);

		if (usuarioActual == null) {
			log.warn("Verificación de contraseña para un usuario inexistente: '{}'", usuario);
			return false;
		}

		return passwordEncoder.matches(rawPassword, usuarioActual.getPassword());
	}

	@Override
	@Transactional(readOnly = true)
	public Integer totalUsuarios() {
		try {
			log.info("Consultando cantidad de usuarios");
			return repoUsuario.getCantidadUsuarios() == null ? 0 : repoUsuario.getCantidadUsuarios();
		} catch (DataAccessException e) {
			log.error("Error de base de datos al contar usuarios: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(e.getMessage(), e);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<Usuario> cajeros() {
		try {
			log.info("Listando usuarios con rol de cajero");
			return repoUsuario.findByRole();
		} catch (DataAccessException e) {
			log.error("Error de base de datos al listar cajeros: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(e.getMessage(), e);
		}
	}

	@Override
	@Transactional
	public Usuario save(Usuario usuario) {
		try {
			log.info("Registrando nuevo usuario: {}", usuario.getUsuario());
			usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
			return repoUsuario.save(usuario);
		} catch (DataAccessException e) {
			log.error("Error de base de datos al registrar usuario: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(e.getMessage(), e);
		}
	}

	@Override
	@Transactional
	public Usuario update(Usuario usuario, Integer idUsuario) {
		try {
			log.info("Actualizando usuario con ID: {}", idUsuario);

			Usuario usuarioActual = repoUsuario.findById(idUsuario)
					.orElseThrow(() -> {
						log.warn("El usuario a actualizar con ID {} no existe", idUsuario);
						return new NotFoundException("El usuario a actualizar no existe en la base de datos");
					});

			usuarioActual.setPrimerNombre(usuario.getPrimerNombre());
			usuarioActual.setSegundoNombre(usuario.getSegundoNombre());
			usuarioActual.setApellido(usuario.getApellido());
			usuarioActual.setUsuario(usuario.getUsuario());
			usuarioActual.setRoles(usuario.getRoles());

			// Solo se recodifica la contraseña si cambió respecto a la almacenada
			if (!usuarioActual.getPassword().equals(usuario.getPassword())) {
				usuarioActual.setPassword(passwordEncoder.encode(usuario.getPassword()));
			}

			return repoUsuario.save(usuarioActual);
		} catch (DataAccessException e) {
			log.error("Error de base de datos al actualizar usuario: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(e.getMessage(), e);
		}
	}

	@Override
	@Transactional
	public void delete(Integer idUsuario) {
		try {
			log.info("Eliminando usuario con ID: {}", idUsuario);
			if (!repoUsuario.existsById(idUsuario)) {
				log.warn("El usuario a eliminar con ID {} no existe", idUsuario);
				throw new NotFoundException("El usuario a eliminar no existe en la base de datos");
			}
			repoUsuario.deleteById(idUsuario);
		} catch (DataAccessException e) {
			log.error("Error de base de datos al eliminar usuario: {}", e.getMessage());
			throw new xyz.pangosoft.dtodo.error.exceptions.DataAccessException(e.getMessage(), e);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<Role> findRoles() {
		return repoUsuario.findRoles();
	}

}
