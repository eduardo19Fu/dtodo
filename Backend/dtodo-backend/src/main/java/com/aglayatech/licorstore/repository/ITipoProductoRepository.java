package com.aglayatech.licorstore.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aglayatech.licorstore.model.TipoProducto;

public interface ITipoProductoRepository extends JpaRepository<TipoProducto, Integer> {
	
	// Consulta para encontrar un listado de tipos de producto que coincida con el dato ingresado por el usuario
	@Query("Select t from TipoProducto t where t.tipoProducto = :tipo")
	List<TipoProducto> findByTipoProducto(@Param("tipo") String tipo);

}
