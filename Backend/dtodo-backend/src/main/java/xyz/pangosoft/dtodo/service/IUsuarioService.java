package xyz.pangosoft.dtodo.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.model.Role;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.dto.UsuarioDto;

public interface IUsuarioService {

	List<Usuario> findAll();

	Page<Usuario> findAll(Pageable pageable);

	Page<UsuarioDto> findListado(String filtro, Pageable pageable);

	Usuario findById(Integer idUsuario);

	Usuario findByUsuario(String usuario);

	/**
	 * Verifica la contraseña en claro de un usuario contra el hash almacenado.
	 * Se usa para reautenticar operaciones sensibles (p. ej. el despacho de una
	 * Nota de Crédito) sin emitir un token nuevo.
	 *
	 * @param usuario     nombre de usuario, normalmente el del token en sesión
	 * @param rawPassword contraseña en claro a validar
	 * @return {@code true} solo si el usuario existe y la contraseña coincide
	 */
	boolean matchesPassword(String usuario, String rawPassword);

	Integer totalUsuarios();

	List<Usuario> cajeros();

	Usuario save(Usuario usuario);

	Usuario update(Usuario usuario, Integer idUsuario);

	void delete(Integer idUsuario);

	// Método encargado de recolectar los roles de la BD
	List<Role> findRoles();

}
