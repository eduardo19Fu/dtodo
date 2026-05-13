package xyz.pangosoft.dtodo.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.model.TipoProducto;

public interface ITipoProductoService {
	
	public List<TipoProducto> findAll();
	
	public Page<TipoProducto> findAll(Pageable pageable);
	
	public List<TipoProducto> findByTipo(String tipo);
	
	public TipoProducto findById(Integer id);
	
	public TipoProducto save(TipoProducto tipo);
	
	public void delete(TipoProducto tipo);

}
