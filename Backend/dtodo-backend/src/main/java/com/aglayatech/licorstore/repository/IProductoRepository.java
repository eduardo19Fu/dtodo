package com.aglayatech.licorstore.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aglayatech.licorstore.model.Estado;
import com.aglayatech.licorstore.model.Producto;

public interface IProductoRepository extends JpaRepository<Producto, Integer> {
	
	// Buscar listado de productos por estado
	List<Producto> findByEstado(Estado estado);

	@Query(value = "{call sp_consultar_productos(:idestado)}", nativeQuery = true)
	List<Producto> listarPorEstadoSP(Integer idestado);

	// Filtra los productos por nombre y devuelve un listado con las coincidencias
	// select * from Producto where nombre = /*valor ingresado por usuario*/
	List<Producto> findByNombreContaining(String nombre);

	@Query(value = "Select get_cant_productos()", nativeQuery = true)
	Integer getCantProductos();
	
	@Query("select p from Producto p where p.codProducto = :codigo")
	Optional<Producto> findByCodigo(@Param("codigo") String codigo);
	
	@Query(value = "select p from Producto p where p.fechaVencimiento <= :fecha")
	List<Producto> findCaducados(@Param("fecha") Date fecha);
	
	@Query(value = "SELECT p.id_producto, p.cod_producto, p.nombre,tp.tipo_producto,mar.marca, p.fecha_vencimiento, p.precio_venta, e.estado\r\n"
			+ "FROM productos AS p\r\n"
			+ "INNER JOIN tipos_producto AS tp ON p.id_tipo_producto = tp.id_tipo_producto\r\n"
			+ "INNER JOIN marcas_producto AS mar ON mar.id_marca_producto = p.id_marca_producto\r\n"
			+ "INNER JOIN estados AS e ON e.id_estado = p.id_estado\r\n"
			+ "WHERE fecha_vencimiento >= curdate()", nativeQuery = true)
	List<Object[]> findExpired();

}
