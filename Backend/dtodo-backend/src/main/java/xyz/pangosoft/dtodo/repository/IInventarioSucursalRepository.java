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

import xyz.pangosoft.dtodo.dto.InventarioSucursalDto;
import xyz.pangosoft.dtodo.model.InventarioSucursal;
import xyz.pangosoft.dtodo.util.TerminosBusqueda;

public interface IInventarioSucursalRepository extends JpaRepository<InventarioSucursal, Long> {

	Optional<InventarioSucursal> findBySucursal_IdSucursalAndProducto_IdProducto(Integer idSucursal, Integer idProducto);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT i FROM InventarioSucursal i WHERE i.sucursal.idSucursal = :idSucursal " +
			"AND i.producto.idProducto = :idProducto")
	Optional<InventarioSucursal> findByProductoParaActualizar(
			@Param("idSucursal") Integer idSucursal, @Param("idProducto") Integer idProducto);

	boolean existsBySucursal_IdSucursal(Integer idSucursal);

	long countBySucursal_IdSucursal(Integer idSucursal);

	@Query(value = "SELECT new xyz.pangosoft.dtodo.dto.InventarioSucursalDto(" +
			"i.idInventarioSucursal, p.idProducto, p.codProducto, p.nombre, " +
			"i.stock, i.stockMinimo, i.fechaActualizacion) " +
			"FROM InventarioSucursal i JOIN i.producto p " +
			"WHERE i.sucursal.idSucursal = :idSucursal " +
			"AND (:t1 = '' OR CONCAT(LOWER(p.nombre), ' ', LOWER(COALESCE(p.codProducto, ''))) LIKE CONCAT('%', :t1, '%')) " +
			"AND (:t2 = '' OR CONCAT(LOWER(p.nombre), ' ', LOWER(COALESCE(p.codProducto, ''))) LIKE CONCAT('%', :t2, '%')) " +
			"AND (:t3 = '' OR CONCAT(LOWER(p.nombre), ' ', LOWER(COALESCE(p.codProducto, ''))) LIKE CONCAT('%', :t3, '%')) " +
			"AND (:t4 = '' OR CONCAT(LOWER(p.nombre), ' ', LOWER(COALESCE(p.codProducto, ''))) LIKE CONCAT('%', :t4, '%')) " +
			"AND (:t5 = '' OR CONCAT(LOWER(p.nombre), ' ', LOWER(COALESCE(p.codProducto, ''))) LIKE CONCAT('%', :t5, '%')) " +
			"AND (:t6 = '' OR CONCAT(LOWER(p.nombre), ' ', LOWER(COALESCE(p.codProducto, ''))) LIKE CONCAT('%', :t6, '%')) ",
			countQuery = "SELECT COUNT(i) FROM InventarioSucursal i JOIN i.producto p " +
					"WHERE i.sucursal.idSucursal = :idSucursal " +
					"AND (:t1 = '' OR CONCAT(LOWER(p.nombre), ' ', LOWER(COALESCE(p.codProducto, ''))) LIKE CONCAT('%', :t1, '%')) " +
					"AND (:t2 = '' OR CONCAT(LOWER(p.nombre), ' ', LOWER(COALESCE(p.codProducto, ''))) LIKE CONCAT('%', :t2, '%')) " +
					"AND (:t3 = '' OR CONCAT(LOWER(p.nombre), ' ', LOWER(COALESCE(p.codProducto, ''))) LIKE CONCAT('%', :t3, '%')) " +
					"AND (:t4 = '' OR CONCAT(LOWER(p.nombre), ' ', LOWER(COALESCE(p.codProducto, ''))) LIKE CONCAT('%', :t4, '%')) " +
					"AND (:t5 = '' OR CONCAT(LOWER(p.nombre), ' ', LOWER(COALESCE(p.codProducto, ''))) LIKE CONCAT('%', :t5, '%')) " +
					"AND (:t6 = '' OR CONCAT(LOWER(p.nombre), ' ', LOWER(COALESCE(p.codProducto, ''))) LIKE CONCAT('%', :t6, '%')) ")
	Page<InventarioSucursalDto> buscarPorTerminos(
			@Param("idSucursal") Integer idSucursal,
			@Param("t1") String t1, @Param("t2") String t2, @Param("t3") String t3, @Param("t4") String t4, @Param("t5") String t5, @Param("t6") String t6, Pageable pageable);

	/**
	 * Existencias de la sucursal filtradas por nombre o código: el texto se divide en palabras y el producto debe
	 * contenerlas todas, en cualquier orden.
	 */
	default Page<InventarioSucursalDto> findListado(Integer idSucursal, String filtro, Pageable pageable) {
		String[] t = TerminosBusqueda.dividir(filtro);
		return buscarPorTerminos(idSucursal, t[0], t[1], t[2], t[3], t[4], t[5], pageable);
	}

	@Modifying
	@Query(value = "INSERT INTO inventario_sucursal (id_sucursal, id_producto, stock, stock_minimo, fecha_actualizacion) " +
			"SELECT :idSucursalDestino, id_producto, stock, stock_minimo, NOW() " +
			"FROM inventario_sucursal WHERE id_sucursal = :idSucursalOrigen",
			nativeQuery = true)
	void clonarInventario(@Param("idSucursalOrigen") Integer idSucursalOrigen,
			@Param("idSucursalDestino") Integer idSucursalDestino);

}
