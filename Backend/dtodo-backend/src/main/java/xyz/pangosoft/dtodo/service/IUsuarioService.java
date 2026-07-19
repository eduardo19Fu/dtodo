package xyz.pangosoft.dtodo.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.model.Role;
import xyz.pangosoft.dtodo.model.Usuario;

public interface IUsuarioService {

	List<Usuario> findAll();

	Page<Usuario> findAll(Pageable pageable);

	Usuario findById(Integer idUsuario);

	Usuario findByUsuario(String usuario);

	Integer totalUsuarios();

	List<Usuario> cajeros();

	Usuario save(Usuario usuario);

	Usuario update(Usuario usuario, Integer idUsuario);

	void delete(Integer idUsuario);

	// Método encargado de recolectar los roles de la BD
	List<Role> findRoles();

}
