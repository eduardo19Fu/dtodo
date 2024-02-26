package com.aglayatech.licorstore.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.aglayatech.licorstore.model.MarcaProducto;

public interface IMarcaProductoService {

	// Devuelve un listado con todas las marcas guardadas en la base de datos
	public List<MarcaProducto> findAll();
	
	// Devuelve un listado paginable PARA el frontend
	public Page<MarcaProducto> findAll(Pageable pageable);

	// Devuelve la marca encontrada por su id en la base de datos
	public MarcaProducto findById(Integer idMarca);

	// Devuelve un listado de todas las marcas, cuyo nombre coincida con el elemento ingresado por el usuario
	public List<MarcaProducto> findByMarca(String marca);
	
	// Registra en la base de datos una marca nueva, ingresada por el usuario y devuelve un json con los valores ingresados
	public MarcaProducto save(MarcaProducto marca);
	
	// Eliminar registro de la base de datos
	public void deleteMarca(Integer idMarca);

}
