package xyz.pangosoft.dtodo.repository;

import xyz.pangosoft.dtodo.dto.ProformaFechaDto;
import xyz.pangosoft.dtodo.dto.ProformaDto;
import xyz.pangosoft.dtodo.dto.DetalleDocumentoDto;
import xyz.pangosoft.dtodo.dto.DocumentoOrigenNotaDto;
import xyz.pangosoft.dtodo.dto.UsuarioDto;
import xyz.pangosoft.dtodo.model.Proforma;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface IProformaRepository extends JpaRepository<Proforma, Long> {

    List<Proforma> findAllByFechaEmisionBetween(Date date1, Date date2);

    @Query(value = "{call sp_get_proformas(:date1, :date2)}", nativeQuery = true)
    List<Proforma> findAllProformas(@Param("date1") Date date1, @Param("date2") Date date2);

    @Query("select new xyz.pangosoft.dtodo.dto.ProformaFechaDto(" +
            "p.idProforma, p.noProforma, p.fechaEmision, p.total, e.estado, u.usuario, c.nombre) " +
            "from Proforma p join p.estado e join p.usuario u join p.cliente c " +
            "where p.fechaEmision between :date1 and :date2")
    List<ProformaFechaDto> findAllProformasDto(@Param("date1") Date date1, @Param("date2") Date date2);

    Optional<Proforma> findProformaByNoProforma(String noProforma);

    @Query("select new xyz.pangosoft.dtodo.dto.DocumentoOrigenNotaDto(" +
            "p.idProforma, c.idCliente, c.nombre, p.noProforma, '') " +
            "from Proforma p join p.cliente c where p.noProforma = :noProforma")
    Optional<DocumentoOrigenNotaDto> findOrigenNotaDto(@Param("noProforma") String noProforma);

    @Query("select new xyz.pangosoft.dtodo.dto.ProformaDto(" +
            "p.idProforma, p.noProforma, p.fechaEmision, p.total, e.idEstado, e.estado, " +
            "u.usuario, concat(coalesce(u.primerNombre, ''), ' ', coalesce(u.apellido, '')), c.nombre, c.nit) " +
            "from Proforma p join p.estado e join p.usuario u join p.cliente c " +
            "order by p.fechaEmision desc")
    List<ProformaDto> findUltimasListadoDto(Pageable pageable);

    @Query("select new xyz.pangosoft.dtodo.dto.ProformaDto(" +
            "p.idProforma, p.noProforma, p.fechaEmision, p.total, e.idEstado, e.estado, " +
            "u.usuario, concat(coalesce(u.primerNombre, ''), ' ', coalesce(u.apellido, '')), c.nombre, c.nit) " +
            "from Proforma p join p.estado e join p.usuario u join p.cliente c " +
            "where p.fechaEmision >= :fechaIni and p.fechaEmision < :fechaFin")
    Page<ProformaDto> findAllListadoDto(@Param("fechaIni") Date fechaIni,
                                                @Param("fechaFin") Date fechaFin,
                                                Pageable pageable);

    @Query("select new xyz.pangosoft.dtodo.dto.ProformaDto(" +
            "p.idProforma, p.noProforma, p.fechaEmision, p.total, e.idEstado, e.estado, " +
            "u.usuario, concat(coalesce(u.primerNombre, ''), ' ', coalesce(u.apellido, '')), c.nombre, c.nit) " +
            "from Proforma p join p.estado e join p.usuario u join p.cliente c " +
            "where p.fechaEmision >= :fechaIni and p.fechaEmision < :fechaFin and (" +
            "lower(c.nombre) like lower(concat('%', :filtro, '%')) or " +
            "lower(c.nit) like lower(concat('%', :filtro, '%')) or " +
            "lower(u.usuario) like lower(concat('%', :filtro, '%')) or " +
            "lower(concat(coalesce(u.primerNombre, ''), ' ', coalesce(u.apellido, ''))) like lower(concat('%', :filtro, '%')) or " +
            "lower(p.noProforma) like lower(concat('%', :filtro, '%')))")
    Page<ProformaDto> searchListadoDto(@Param("fechaIni") Date fechaIni,
                                               @Param("fechaFin") Date fechaFin,
                                               @Param("filtro") String filtro,
                                               Pageable pageable);

    @Query(value = "SELECT d.id_detalle AS idDetalle, p.id_producto AS idProducto, " +
            "p.cod_producto AS codigoProducto, p.nombre AS producto, d.cantidad AS cantidad, " +
            "d.sub_total AS subTotal, d.descuento AS descuento, d.sub_total_descuento AS subTotalDescuento " +
            "FROM proformas_detalle d INNER JOIN productos p ON p.id_producto = d.id_producto " +
            "WHERE d.id_proforma = :idProforma ORDER BY d.id_detalle",
            countQuery = "SELECT COUNT(*) FROM proformas_detalle WHERE id_proforma = :idProforma",
            nativeQuery = true)
    Page<Object[]> findDetalleDto(@Param("idProforma") Long idProforma, Pageable pageable);

    @Query("select distinct new xyz.pangosoft.dtodo.dto.UsuarioDto(" +
            "u.idUsuario, u.usuario, u.primerNombre, u.segundoNombre, " +
            "u.apellido, u.enabled, u.fechaRegistro) " +
            "from Proforma p join p.usuario u order by u.usuario")
    List<UsuarioDto> findUsuariosConProformas();

}
