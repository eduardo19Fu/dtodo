package xyz.pangosoft.dtodo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.pangosoft.dtodo.dto.CompraDto;
import xyz.pangosoft.dtodo.model.Compra;

public interface ICompraRepository extends JpaRepository<Compra, Long> {

	@Query(value = "SELECT new xyz.pangosoft.dtodo.dto.CompraDto(" +
			"c.idCompra, c.fechaCompra, c.fechaRegistro, c.noComprobante, c.tipoComprobante, " +
			"c.total, c.costoEnvio, c.estado, p.nombre, s.nombre, u.usuario, " +
			"concat(coalesce(u.primerNombre, ''), ' ', coalesce(u.apellido, '')), c.observaciones) " +
			"FROM Compra c LEFT JOIN c.proveedor p LEFT JOIN c.sucursal s LEFT JOIN c.usuario u " +
			"WHERE (:idSucursal IS NULL OR s.idSucursal = :idSucursal) " +
			"AND (:filtro = '' OR LOWER(c.noComprobante) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')))",
			countQuery = "SELECT COUNT(c) FROM Compra c LEFT JOIN c.proveedor p LEFT JOIN c.sucursal s " +
					"WHERE (:idSucursal IS NULL OR s.idSucursal = :idSucursal) " +
					"AND (:filtro = '' OR LOWER(c.noComprobante) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')))")
	Page<CompraDto> findListado(@Param("filtro") String filtro, @Param("idSucursal") Integer idSucursal, Pageable pageable);

	@Query(value = "SELECT d.id_detalle AS idDetalle, p.id_producto AS idProducto, " +
			"p.cod_producto AS codigoProducto, p.nombre AS producto, d.cantidad AS cantidad, " +
			"d.precio_unitario AS precioUnitario, d.sub_total AS subTotal " +
			"FROM compras_detalle d INNER JOIN productos p ON p.id_producto = d.id_producto " +
			"WHERE d.id_compra = :idCompra ORDER BY d.id_detalle",
			countQuery = "SELECT COUNT(*) FROM compras_detalle WHERE id_compra = :idCompra",
			nativeQuery = true)
	Page<Object[]> findDetalleDto(@Param("idCompra") Long idCompra, Pageable pageable);

}
