package xyz.pangosoft.dtodo.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.model.Role;

public interface IRoleService {

	List<Role> findAll();

	Page<Role> findAll(Pageable pageable);

	// Devuelve el rol cuyo nombre coincide con el parámetro otorgado
	Role findByName(String role);

	Role findById(Integer idRole);

	Role save(Role role);

}
