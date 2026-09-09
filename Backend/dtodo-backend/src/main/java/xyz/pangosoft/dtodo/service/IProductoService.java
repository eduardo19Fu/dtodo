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
	
	public List<Producto> findAll(Integer idSucursal);

	public List<ProductoDto> findAllDto(Integer idSucursal);

	public Page<ProductoDtoMejorado> findAllDtoMejorado(String orden, String direccion, Integer idSucursal, Pageable pageable);

	public Page<ProductoDtoMejorado> searchProductoDtoMejorado(
			String filtro, String orden, String direccion, Integer idSucursal, Pageable pageable);

	public List<ProductoDto> findAllByEstado(Estado estado, Integer idSucursal);
	
	public Page<Producto> findAll(Pageable pageable);
	
	public Producto findById(Integer idproducto);

	public Integer totalProductos(Integer idSucursal);
	
	public Producto save(Producto producto);

	// Actualiza un producto y propaga los campos compartidos (nombre, precios, marca, tipo, etc.)
	// a todas las demás filas de Producto que tengan el mismo codProducto, sin tocar el stock,
	// la fecha de registro ni el estado de cada una (esos campos son propios de cada sucursal).
	public Producto actualizarYSincronizar(Producto producto);

	public void delete(Producto producto);
	
	// Busqueda de Productos desde el frontend
	public List<Producto> findByName(String name);
	
	public Producto findByCodigo(String codigo, Integer idSucursal);
	
	// Listado de productos caducados
	public List<Producto> findCaducados();

	public Producto upload(MultipartFile file, Integer idProducto);

	public Resource cargar(String nombreImagen);

	public byte[] inventarioPDF(String fechaIni, String fechaFin);

	public byte[] productosExcel(Integer idSucursal);
}
