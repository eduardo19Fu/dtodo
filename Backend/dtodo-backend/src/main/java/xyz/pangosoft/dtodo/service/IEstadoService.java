package xyz.pangosoft.dtodo.service;

import java.util.List;

import xyz.pangosoft.dtodo.model.Estado;

public interface IEstadoService {
	
	public List<Estado> findAll();
	
	public Estado findById(Integer idestado);
	
	public Estado findByEstado(String estado);
	
	public Estado save(Estado estado);
	
	public void delete(Estado estado);

}
