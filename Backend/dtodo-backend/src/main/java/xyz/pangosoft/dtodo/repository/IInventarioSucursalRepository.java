package xyz.pangosoft.dtodo.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.pangosoft.dtodo.dto.InventarioSucursalDto;
import xyz.pangosoft.dtodo.model.InventarioSucursal;

public interface IInventarioSucursalRepository extends JpaRepository<InventarioSucursal, Long> {

	Optional<InventarioSucursal> findBySucursal_IdSucursalAndProducto_IdProducto(Integer idSucursal, Integer idProducto);

	boolean existsBySucursal_IdSucursal(Integer idSucursal);

	long countBySucursal_IdSucursal(Integer idSucursal);

	@Query(value = "SELECT new xyz.pangosoft.dtodo.dto.InventarioSucursalDto(" +
			"i.idInventarioSucursal, p.idProducto, p.codProducto, p.nombre, " +
			"i.stock, i.stockMinimo, i.fechaActualizacion) " +
			"FROM InventarioSucursal i JOIN i.producto p " +
			"WHERE i.sucursal.idSucursal = :idSucursal " +
			"AND (:filtro = '' OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(p.codProducto) LIKE LOWER(CONCAT('%', :filtro, '%')))",
			countQuery = "SELECT COUNT(i) FROM InventarioSucursal i JOIN i.producto p " +
					"WHERE i.sucursal.idSucursal = :idSucursal " +
					"AND (:filtro = '' OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(p.codProducto) LIKE LOWER(CONCAT('%', :filtro, '%')))")
	Page<InventarioSucursalDto> findListado(
			@Param("idSucursal") Integer idSucursal, @Param("filtro") String filtro, Pageable pageable);

	@Modifying
	@Query(value = "INSERT INTO inventario_sucursal (id_sucursal, id_producto, stock, stock_minimo, fecha_actualizacion) " +
			"SELECT :idSucursalDestino, id_producto, stock, stock_minimo, NOW() " +
			"FROM inventario_sucursal WHERE id_sucursal = :idSucursalOrigen",
			nativeQuery = true)
	void clonarInventario(@Param("idSucursalOrigen") Integer idSucursalOrigen,
			@Param("idSucursalDestino") Integer idSucursalDestino);

}
