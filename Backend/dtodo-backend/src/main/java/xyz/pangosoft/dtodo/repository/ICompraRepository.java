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
			"c.total, c.costoEnvio, c.estado, p.nombre, s.nombre, u.usuario) " +
			"FROM Compra c LEFT JOIN c.proveedor p LEFT JOIN c.sucursal s LEFT JOIN c.usuario u " +
			"WHERE (:idSucursal IS NULL OR s.idSucursal = :idSucursal) " +
			"AND (:filtro = '' OR LOWER(c.noComprobante) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
			"OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')))",
			countQuery = "SELECT COUNT(c) FROM Compra c LEFT JOIN c.proveedor p LEFT JOIN c.sucursal s " +
					"WHERE (:idSucursal IS NULL OR s.idSucursal = :idSucursal) " +
					"AND (:filtro = '' OR LOWER(c.noComprobante) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
					"OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')))")
	Page<CompraDto> findListado(@Param("filtro") String filtro, @Param("idSucursal") Integer idSucursal, Pageable pageable);

}
