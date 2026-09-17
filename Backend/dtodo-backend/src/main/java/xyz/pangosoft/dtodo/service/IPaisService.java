package xyz.pangosoft.dtodo.service;

import java.util.List;

import xyz.pangosoft.dtodo.model.Pais;

public interface IPaisService {

	public List<Pais> findAll();

	public Pais findById(Integer idPais);

	public Pais save(Pais pais);

}
