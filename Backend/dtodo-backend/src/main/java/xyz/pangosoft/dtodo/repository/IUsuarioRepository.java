package xyz.pangosoft.dtodo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.pangosoft.dtodo.dto.UsuarioDto;
import xyz.pangosoft.dtodo.model.Role;
import xyz.pangosoft.dtodo.model.Usuario;

public interface IUsuarioRepository extends JpaRepository<Usuario, Integer> {
	
	public Usuario findByUsuario(String usuario);
	
	@Query("select u from Usuario u where u.usuario = ?1")
	public Usuario findByUsuario2(String usuario);
	
	
	@Query("from Role r")
	public List<Role> findRoles();
	
	@Query(value = "select u.*\r\n"
					+ "from usuarios as u\r\n"
					+ "inner join usuarios_roles as ur on ur.usuario_id = u.id_usuario\r\n"
					+ "inner join roles as r on r.id_role = ur.role_id\r\n"
					+ "where r.role = 'ROLE_COBRADOR' AND enabled = 1;",
			nativeQuery = true)
	public List<Usuario> findByRole();

	@Query(value = "Select get_cant_usuarios()", nativeQuery = true)
	Integer getCantidadUsuarios();

	@Query(value = "SELECT new xyz.pangosoft.dtodo.dto.UsuarioDto(" +
			"u.idUsuario, u.usuario, u.primerNombre, u.segundoNombre, " +
			"u.apellido, u.enabled, u.fechaRegistro) FROM Usuario u " +
			"WHERE (:filtro = '' " +
			"OR LOWER(COALESCE(u.usuario, '')) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(COALESCE(u.primerNombre, '')) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(COALESCE(u.segundoNombre, '')) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(COALESCE(u.apellido, '')) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(CONCAT(COALESCE(u.primerNombre, ''), ' ', " +
			"COALESCE(u.segundoNombre, ''), ' ', COALESCE(u.apellido, ''))) " +
			"LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR STR(u.idUsuario) LIKE CONCAT('%', :filtro, '%'))",
			countQuery = "SELECT COUNT(u) FROM Usuario u WHERE (:filtro = '' " +
					"OR LOWER(COALESCE(u.usuario, '')) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(COALESCE(u.primerNombre, '')) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(COALESCE(u.segundoNombre, '')) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(COALESCE(u.apellido, '')) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(CONCAT(COALESCE(u.primerNombre, ''), ' ', " +
					"COALESCE(u.segundoNombre, ''), ' ', COALESCE(u.apellido, ''))) " +
					"LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR STR(u.idUsuario) LIKE CONCAT('%', :filtro, '%'))")
	Page<UsuarioDto> findListado(@Param("filtro") String filtro, Pageable pageable);

}
