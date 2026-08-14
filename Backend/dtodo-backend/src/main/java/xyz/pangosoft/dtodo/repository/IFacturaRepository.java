package xyz.pangosoft.dtodo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import xyz.pangosoft.dtodo.dto.FacturaDto;
import xyz.pangosoft.dtodo.dto.DetalleDocumentoDto;
import xyz.pangosoft.dtodo.model.Factura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface IFacturaRepository extends JpaRepository<Factura, Long> {

    @Query(value = "Select get_cant_ventas()", nativeQuery = true)
    Integer getCantidadVentas();

    List<Factura> findByFechaBetween(Date iniDate, Date endDate);

    Optional<Factura> findFacturaByNoFactura(Long noFactura);

    Optional<Factura> findFacturaByCorrelativoSatAndSerieSat(String correlativoSat, String serieSat);

    @Query(value = "{call sp_get_facturas(:date1, :date2);}", nativeQuery = true)
    List<Factura> findAllFacturas(@Param("date1") Date date1, @Param("date2") Date date2);

    @Query("select new xyz.pangosoft.dtodo.dto.FacturaDto(" +
            "f.idFactura, f.noFactura, f.serie, f.fecha, f.total, e.idEstado, e.estado, " +
            "u.usuario, concat(coalesce(u.primerNombre, ''), ' ', coalesce(u.apellido, '')), " +
            "c.nombre, c.nit, f.certificacionSat) " +
            "from Factura f join f.estado e join f.usuario u join f.cliente c " +
            "order by f.fecha desc")
    List<FacturaDto> findUltimasListadoDto(Pageable pageable);

    @Query("select new xyz.pangosoft.dtodo.dto.FacturaDto(" +
            "f.idFactura, f.noFactura, f.serie, f.fecha, f.total, e.idEstado, e.estado, " +
            "u.usuario, concat(coalesce(u.primerNombre, ''), ' ', coalesce(u.apellido, '')), " +
            "c.nombre, c.nit, f.certificacionSat) " +
            "from Factura f join f.estado e join f.usuario u join f.cliente c " +
            "where f.fecha >= :fechaIni and f.fecha < :fechaFin " +
            "order by f.fecha desc")
    Page<FacturaDto> findAllListadoDto(@Param("fechaIni") Date fechaIni,
                                               @Param("fechaFin") Date fechaFin,
                                               Pageable pageable);

    @Query("select new xyz.pangosoft.dtodo.dto.FacturaDto(" +
            "f.idFactura, f.noFactura, f.serie, f.fecha, f.total, e.idEstado, e.estado, " +
            "u.usuario, concat(coalesce(u.primerNombre, ''), ' ', coalesce(u.apellido, '')), " +
            "c.nombre, c.nit, f.certificacionSat) " +
            "from Factura f join f.estado e join f.usuario u join f.cliente c " +
            "where f.fecha >= :fechaIni and f.fecha < :fechaFin and (" +
            "lower(c.nombre) like lower(concat('%', :filtro, '%')) or " +
            "lower(c.nit) like lower(concat('%', :filtro, '%')) or " +
            "lower(u.usuario) like lower(concat('%', :filtro, '%')) or " +
            "lower(concat(coalesce(u.primerNombre, ''), ' ', coalesce(u.apellido, ''))) like lower(concat('%', :filtro, '%')) or " +
            "lower(coalesce(f.serie, '')) like lower(concat('%', :filtro, '%')) or " +
            "str(f.noFactura) like concat('%', :filtro, '%')) " +
            "order by f.fecha desc")
    Page<FacturaDto> searchListadoDto(@Param("fechaIni") Date fechaIni,
                                              @Param("fechaFin") Date fechaFin,
                                              @Param("filtro") String filtro,
                                              Pageable pageable);

    @Query(value = "SELECT d.id_detalle AS idDetalle, p.nombre AS producto, d.cantidad AS cantidad, " +
            "d.sub_total AS subTotal, d.descuento AS descuento, d.sub_total_descuento AS subTotalDescuento " +
            "FROM facturas_detalle d INNER JOIN productos p ON p.id_producto = d.id_producto " +
            "WHERE d.id_factura = :idFactura ORDER BY d.id_detalle",
            countQuery = "SELECT COUNT(*) FROM facturas_detalle WHERE id_factura = :idFactura",
            nativeQuery = true)
    Page<DetalleDocumentoDto> findDetalleDto(@Param("idFactura") Long idFactura, Pageable pageable);
}
