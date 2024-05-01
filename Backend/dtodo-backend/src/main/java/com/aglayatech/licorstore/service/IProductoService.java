package com.aglayatech.licorstore.service;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.aglayatech.licorstore.model.Estado;
import com.aglayatech.licorstore.model.Producto;

import net.sf.jasperreports.engine.JRException;
import org.springframework.web.multipart.MultipartFile;

public interface IProductoService {
	
	public List<Producto> findAll();
	
	public List<Producto> findAllByEstado(Estado estado);
	
	public Page<Producto> findAll(Pageable pageable);
	
	public Producto findById(Integer idproducto);

	public Integer totalProductos();
	
	public Producto save(Producto producto);
	
	public void delete(Producto producto);
	
	// Busqueda de Productos desde el frontend
	public List<Producto> findByName(String name);
	
	public Producto findByCodigo(String codigo);
	
	// Listado de productos caducados
	public List<Producto> findCaducados();

	public Producto upload(MultipartFile file, Integer idProducto);

	public Resource cargar(String nombreImagen);

	public byte[] inventarioPDF(String fechaIni, String fechaFin);
}
