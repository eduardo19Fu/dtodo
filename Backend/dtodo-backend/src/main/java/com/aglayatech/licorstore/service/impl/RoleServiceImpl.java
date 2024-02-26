package com.aglayatech.licorstore.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import com.aglayatech.licorstore.model.Role;
import com.aglayatech.licorstore.repository.IRoleRepository;
import com.aglayatech.licorstore.service.IRoleService;

@Service
public class RoleServiceImpl implements IRoleService {

	@Autowired
	private IRoleRepository repoRole;
	
	@Override
	public List<Role> findAll() {
		return repoRole.findAll(Sort.by(Direction.ASC, "idRole"));
	}

	@Override
	public Page<Role> findAll(Pageable pageable) {
		return repoRole.findAll(pageable);
	}

	@Override
	public Role findById(Integer id) {
		return repoRole.findById(id).orElse(null);
	}

	@Override
	public Role save(Role role) {
		return repoRole.save(role);
	}

	@Override
	public Role findByName(String role) {
		return repoRole.getRoles(role);
	}

}
