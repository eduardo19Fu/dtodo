package xyz.pangosoft.dtodo.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import xyz.pangosoft.dtodo.dto.InventarioBodegaDto;
import xyz.pangosoft.dtodo.model.InventarioBodega;

public interface IInventarioBodegaRepository extends JpaRepository<InventarioBodega, Long> {

	Optional<InventarioBodega> findByBodega_IdBodegaAndProducto_IdProducto(Integer idBodega, Integer idProducto);

	/** Bloquea la fila mientras se modifica el stock para evitar descuentos simultáneos sobre la misma existencia. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT i FROM InventarioBodega i WHERE i.bodega.idBodega = :idBodega " +
			"AND i.producto.idProducto = :idProducto")
	Optional<InventarioBodega> findParaActualizar(
			@Param("idBodega") Integer idBodega, @Param("idProducto") Integer idProducto);

	boolean existsByBodega_IdBodega(Integer idBodega);

	@Query(value = "SELECT new xyz.pangosoft.dtodo.dto.InventarioBodegaDto(" +
			"i.idInventarioBodega, p.idProducto, p.codProducto, p.nombre, " +
			"i.stock, i.stockMinimo, p.precioCompra, i.fechaActualizacion) " +
			"FROM InventarioBodega i JOIN i.producto p " +
			"WHERE i.bodega.idBodega = :idBodega " +
			"AND (:filtro = '' OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(p.codProducto) LIKE LOWER(CONCAT('%', :filtro, '%')))",
			countQuery = "SELECT COUNT(i) FROM InventarioBodega i JOIN i.producto p " +
					"WHERE i.bodega.idBodega = :idBodega " +
					"AND (:filtro = '' OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(p.codProducto) LIKE LOWER(CONCAT('%', :filtro, '%')))")
	Page<InventarioBodegaDto> findListado(
			@Param("idBodega") Integer idBodega, @Param("filtro") String filtro, Pageable pageable);

	@Query("SELECT new xyz.pangosoft.dtodo.dto.InventarioBodegaDto(" +
			"i.idInventarioBodega, p.idProducto, p.codProducto, p.nombre, " +
			"i.stock, i.stockMinimo, p.precioCompra, i.fechaActualizacion) " +
			"FROM InventarioBodega i JOIN i.producto p " +
			"WHERE i.bodega.idBodega = :idBodega AND LOWER(p.codProducto) = LOWER(:codigo)")
	Optional<InventarioBodegaDto> findDtoPorCodigo(
			@Param("idBodega") Integer idBodega, @Param("codigo") String codigo);

	/** Copia a la bodega todos los productos de una sucursal, incluso los que están sin existencias. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = "INSERT INTO inventario_bodega (id_bodega, id_producto, stock, stock_minimo, fecha_actualizacion) " +
			"SELECT :idBodegaDestino, id_producto, stock, stock_minimo, NOW() " +
			"FROM inventario_sucursal WHERE id_sucursal = :idSucursalOrigen",
			nativeQuery = true)
	int clonarDesdeSucursal(@Param("idBodegaDestino") Integer idBodegaDestino,
			@Param("idSucursalOrigen") Integer idSucursalOrigen);

	/** Copia a la bodega todos los productos de otra bodega, incluso los que están sin existencias. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = "INSERT INTO inventario_bodega (id_bodega, id_producto, stock, stock_minimo, fecha_actualizacion) " +
			"SELECT :idBodegaDestino, id_producto, stock, stock_minimo, NOW() " +
			"FROM inventario_bodega WHERE id_bodega = :idBodegaOrigen",
			nativeQuery = true)
	int clonarDesdeBodega(@Param("idBodegaDestino") Integer idBodegaDestino,
			@Param("idBodegaOrigen") Integer idBodegaOrigen);

}
