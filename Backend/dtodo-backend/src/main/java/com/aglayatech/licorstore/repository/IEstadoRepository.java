package com.aglayatech.licorstore.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aglayatech.licorstore.model.Estado;

public interface IEstadoRepository extends JpaRepository<Estado, Integer> {
	
	Optional<Estado> findByEstado(String estado);

}
