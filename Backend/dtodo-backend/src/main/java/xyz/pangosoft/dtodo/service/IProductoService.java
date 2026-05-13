package xyz.pangosoft.dtodo.service;

import java.util.List;

import xyz.pangosoft.dtodo.dto.ProductoDto;
import xyz.pangosoft.dtodo.dto.ProductoDtoMejorado;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.Producto;

import org.springframework.web.multipart.MultipartFile;

public interface IProductoService {
	
	public List<Producto> findAll();

	public List<ProductoDto> findAllDto();

	public Page<ProductoDtoMejorado> findAllDtoMejorado(Pageable pageable);

	public Page<ProductoDtoMejorado> searchProductoDtoMejorado(String filtro, Pageable pageable);
	
	public List<ProductoDto> findAllByEstado(Estado estado);
	
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
