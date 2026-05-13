package xyz.pangosoft.dtodo.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.model.Role;
import xyz.pangosoft.dtodo.model.Usuario;

public interface IUsuarioService {
	
	public List<Usuario> findAll();
	
	public Page<Usuario> findAll(Pageable pageable);
	
	public Usuario findById(Integer idusaurio);

	public Integer totalUsuarios();
	
	public Usuario save(Usuario usuario);
	
	public Usuario findByUsuario(String usuario);
	
	public void delete(Integer id);
	
	public List<Usuario> cajeros();
	
	// método encargado de recolectar los roles de la BD
	public List<Role> findRoles();

}
