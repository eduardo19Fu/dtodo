package com.aglayatech.licorstore.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aglayatech.licorstore.model.Correlativo;
import com.aglayatech.licorstore.model.Estado;
import com.aglayatech.licorstore.model.Usuario;

public interface ICorrelativoRepository extends JpaRepository<Correlativo, Long> {
	
	// Buscar el correlativo del usuario logueado en el sistema
	public Optional<Correlativo> findByUsuarioAndEstado(Usuario usuario, Estado estado);

}
