package xyz.pangosoft.dtodo.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.dto.ProveedorDto;
import xyz.pangosoft.dtodo.model.Proveedor;

public interface IProveedorService {

	public List<Proveedor> findAll();

	public Page<ProveedorDto> findListado(String filtro, Pageable pageable);

	public Proveedor findById(Integer idProveedor);

	public Proveedor save(Proveedor proveedor);

}
