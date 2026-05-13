package xyz.pangosoft.dtodo.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import xyz.pangosoft.dtodo.model.Role;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.repository.IUsuarioRepository;
import xyz.pangosoft.dtodo.service.IUsuarioService;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioServiceImpl implements UserDetailsService, IUsuarioService {

	private final IUsuarioRepository repoUsuario;

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		
		Usuario usuario = repoUsuario.findByUsuario(username);
		
		if(usuario == null) {
			log.error("Error: no existe el usuario en el sistema.");
			throw new UsernameNotFoundException("Error: no existe el usuario en el sistema.");
		}
		
		List<GrantedAuthority> authorities = usuario.getRoles()
				.stream()
				.map(role -> new SimpleGrantedAuthority(role.getRole()))
				.peek(authority -> log.info("Role: " + authority.getAuthority()))
				.collect(Collectors.toList());
		
		return new User(usuario.getUsuario(), usuario.getPassword(), usuario.isEnabled(), true, true, true, authorities);
	}

	@Override
	public Usuario findByUsuario(String usuario) {
		return repoUsuario.findByUsuario(usuario);
	}

	@Override
	public List<Usuario> findAll() {
		return repoUsuario.findAll(Sort.by(Direction.ASC, "idUsuario"));
	}

	@Override
	public Page<Usuario> findAll(Pageable pageable) {
		return repoUsuario.findAll(pageable);
	}

	@Override
	public Usuario findById(Integer idusaurio) {
		return repoUsuario.findById(idusaurio).orElse(null);
	}

	@Override
	public Integer totalUsuarios() {
		return this.repoUsuario.getCantidadUsuarios() == null ? 0 : this.repoUsuario.getCantidadUsuarios();
	}

	@Override
	public Usuario save(Usuario usuario) {
		return repoUsuario.save(usuario);
	}

	@Override
	public List<Role> findRoles() {
		return repoUsuario.findRoles();
	}

	@Override
	public void delete(Integer id) {
		repoUsuario.deleteById(id);
	}

	@Override
	public List<Usuario> cajeros() {
		return repoUsuario.findByRole();
	}

}
