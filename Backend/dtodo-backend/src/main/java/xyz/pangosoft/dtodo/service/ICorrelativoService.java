package xyz.pangosoft.dtodo.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.model.Correlativo;
import xyz.pangosoft.dtodo.dto.CorrelativoDto;

public interface ICorrelativoService {
	
	public List<Correlativo> findAll();
	
	public Page<Correlativo> findAll(Pageable pageable);

	public Page<CorrelativoDto> findListado(String filtro, Pageable pageable);
	
	public Correlativo findById(Long idcorrelativo);
	
	public Correlativo findByUsuario(Integer idusuario);
	
	public Correlativo save(Correlativo correlativo);

	public Correlativo update(Correlativo correlativo);
	
	public Correlativo anular(Long id);

}
