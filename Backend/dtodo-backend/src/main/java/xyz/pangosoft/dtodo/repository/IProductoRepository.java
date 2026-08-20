package xyz.pangosoft.dtodo.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import xyz.pangosoft.dtodo.dto.ProductoDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.Producto;

public interface IProductoRepository extends JpaRepository<Producto, Integer> {
	
	// Buscar listado de productos por estado
	List<Producto> findByEstado(Estado estado);

	@Query(value = "{call sp_consultar_productos(:idestado)}", nativeQuery = true)
	List<Producto> listarPorEstadoSP(Integer idestado);

	@Query("select new xyz.pangosoft.dtodo.dto.ProductoDto(" +
			"p.idProducto, p.codProducto, p.nombre, p.precioCompra, p.precioVenta, " +
			"p.porcentajeGanancia, p.descripcion, p.fechaVencimiento, p.fechaIngreso, " +
			"p.fechaRegistro, p.stock, m.marca, t.tipoProducto, e.estado) " +
			"from Producto p join p.marcaProducto m join p.tipoProducto t join p.estado e " +
			"where (:idestado = 0 or e.idEstado = :idestado) order by p.nombre")
	List<ProductoDto> listarPorEstadoSPDto(@Param("idestado") Integer idestado);

	// Filtra los productos por nombre y devuelve un listado con las coincidencias
	// select * from Producto where nombre = /*valor ingresado por usuario*/
	List<Producto> findByNombreContaining(String nombre);

	@Query(value = "Select get_cant_productos()", nativeQuery = true)
	Integer getCantProductos();
	
	@Query("select p from Producto p where p.codProducto = :codigo")
	Optional<Producto> findByCodigo(@Param("codigo") String codigo);
	
	@Query(value = "select p from Producto p where p.fechaVencimiento <= :fecha")
	List<Producto> findCaducados(@Param("fecha") Date fecha);

	@Query(value = "SELECT prod.id_producto, " +
				"prod.cod_producto, " +
				"prod.nombre, " +
				"prod.precio_compra, " +
				"prod.precio_venta, " +
				"prod.porcentaje_ganancia, " +
				"prod.descripcion, " +
				"prod.fecha_vencimiento, " +
				"prod.fecha_ingreso, " +
				"prod.fecha_registro, " +
				"prod.stock, " +
				"prod.imagen, " +
				"prod.id_estado, " +
				"m.marca, " +
				"tp.tipo_producto, " +
				"e.estado " +
				"FROM productos AS prod " +
				"INNER JOIN estados AS e ON e.id_estado = prod.id_estado " +
				"LEFT JOIN marcas_producto AS m ON m.id_marca_producto = prod.id_marca_producto " +
				"LEFT JOIN tipos_producto AS tp ON tp.id_tipo_producto = prod.id_tipo_producto " +
				"ORDER BY " +
				"CASE WHEN :direccion = 'asc' AND :orden = 'codigo' THEN prod.cod_producto END ASC, " +
				"CASE WHEN :direccion = 'desc' AND :orden = 'codigo' THEN prod.cod_producto END DESC, " +
				"CASE WHEN :direccion = 'asc' AND :orden = 'nombre' THEN prod.nombre END ASC, " +
				"CASE WHEN :direccion = 'desc' AND :orden = 'nombre' THEN prod.nombre END DESC, " +
				"CASE WHEN :direccion = 'asc' AND :orden = 'precioCompra' THEN prod.precio_compra END ASC, " +
				"CASE WHEN :direccion = 'desc' AND :orden = 'precioCompra' THEN prod.precio_compra END DESC, " +
				"CASE WHEN :direccion = 'asc' AND :orden = 'precioVenta' THEN prod.precio_venta END ASC, " +
				"CASE WHEN :direccion = 'desc' AND :orden = 'precioVenta' THEN prod.precio_venta END DESC, " +
				"CASE WHEN :direccion = 'asc' AND :orden = 'stock' THEN prod.stock END ASC, " +
				"CASE WHEN :direccion = 'desc' AND :orden = 'stock' THEN prod.stock END DESC, " +
				"CASE WHEN :direccion = 'asc' AND :orden = 'tipo' THEN tp.tipo_producto END ASC, " +
				"CASE WHEN :direccion = 'desc' AND :orden = 'tipo' THEN tp.tipo_producto END DESC, " +
				"CASE WHEN :direccion = 'asc' AND :orden = 'marca' THEN m.marca END ASC, " +
				"CASE WHEN :direccion = 'desc' AND :orden = 'marca' THEN m.marca END DESC, " +
				"CASE WHEN :direccion = 'asc' AND :orden = 'estado' THEN e.estado END ASC, " +
				"CASE WHEN :direccion = 'desc' AND :orden = 'estado' THEN e.estado END DESC, " +
				"prod.id_producto ASC",
			countQuery = "SELECT COUNT(*) FROM productos " +
					"WHERE :orden = :orden AND :direccion = :direccion",
			nativeQuery = true)
	Page<Object[]> findAllProductosDto(@Param("orden") String orden,
			@Param("direccion") String direccion, Pageable pageable);

	@Query(value = "SELECT prod.id_producto, " +
				"prod.cod_producto, " +
				"prod.nombre, " +
				"prod.precio_compra, " +
				"prod.precio_venta, " +
				"prod.porcentaje_ganancia, " +
				"prod.descripcion, " +
				"prod.fecha_vencimiento, " +
				"prod.fecha_ingreso, " +
				"prod.fecha_registro, " +
				"prod.stock, " +
				"prod.imagen, " +
				"prod.id_estado, " +
				"m.marca, " +
				"tp.tipo_producto, " +
				"e.estado " +
				"FROM productos AS prod " +
				"INNER JOIN estados AS e ON e.id_estado = prod.id_estado " +
				"LEFT JOIN marcas_producto AS m ON m.id_marca_producto = prod.id_marca_producto " +
				"LEFT JOIN tipos_producto AS tp ON tp.id_tipo_producto = prod.id_tipo_producto " +
				"WHERE prod.nombre LIkE %:filtro% " +
				"OR m.marca LIKE %:filtro% " +
				"OR tp.tipo_producto LIKE %:filtro% " +
				"OR e.estado LIKE %:filtro% " +
				"OR prod.cod_producto LIKE %:filtro% " +
				"ORDER BY " +
				"CASE WHEN :direccion = 'asc' AND :orden = 'codigo' THEN prod.cod_producto END ASC, " +
				"CASE WHEN :direccion = 'desc' AND :orden = 'codigo' THEN prod.cod_producto END DESC, " +
				"CASE WHEN :direccion = 'asc' AND :orden = 'nombre' THEN prod.nombre END ASC, " +
				"CASE WHEN :direccion = 'desc' AND :orden = 'nombre' THEN prod.nombre END DESC, " +
				"CASE WHEN :direccion = 'asc' AND :orden = 'precioCompra' THEN prod.precio_compra END ASC, " +
				"CASE WHEN :direccion = 'desc' AND :orden = 'precioCompra' THEN prod.precio_compra END DESC, " +
				"CASE WHEN :direccion = 'asc' AND :orden = 'precioVenta' THEN prod.precio_venta END ASC, " +
				"CASE WHEN :direccion = 'desc' AND :orden = 'precioVenta' THEN prod.precio_venta END DESC, " +
				"CASE WHEN :direccion = 'asc' AND :orden = 'stock' THEN prod.stock END ASC, " +
				"CASE WHEN :direccion = 'desc' AND :orden = 'stock' THEN prod.stock END DESC, " +
				"CASE WHEN :direccion = 'asc' AND :orden = 'tipo' THEN tp.tipo_producto END ASC, " +
				"CASE WHEN :direccion = 'desc' AND :orden = 'tipo' THEN tp.tipo_producto END DESC, " +
				"CASE WHEN :direccion = 'asc' AND :orden = 'marca' THEN m.marca END ASC, " +
				"CASE WHEN :direccion = 'desc' AND :orden = 'marca' THEN m.marca END DESC, " +
				"CASE WHEN :direccion = 'asc' AND :orden = 'estado' THEN e.estado END ASC, " +
				"CASE WHEN :direccion = 'desc' AND :orden = 'estado' THEN e.estado END DESC, " +
				"prod.id_producto ASC",
			countQuery = "SELECT COUNT(*) " +
				"FROM productos AS prod " +
				"INNER JOIN estados AS e ON e.id_estado = prod.id_estado " +
				"LEFT JOIN marcas_producto AS m ON m.id_marca_producto = prod.id_marca_producto " +
				"LEFT JOIN tipos_producto AS tp ON tp.id_tipo_producto = prod.id_tipo_producto " +
				"WHERE prod.nombre LIkE %:filtro% " +
				"OR m.marca LIKE %:filtro% " +
				"OR tp.tipo_producto LIKE %:filtro% " +
				"OR prod.cod_producto LIKE %:filtro% " +
				"OR (e.estado LIKE %:filtro% AND :orden = :orden AND :direccion = :direccion)",
			nativeQuery = true)
	Page<Object[]> searchProductosDto(@Param("filtro") String filtro,
			@Param("orden") String orden, @Param("direccion") String direccion, Pageable pageable);

}
