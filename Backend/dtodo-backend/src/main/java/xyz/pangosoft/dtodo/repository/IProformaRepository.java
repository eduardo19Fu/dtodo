package xyz.pangosoft.dtodo.repository;

import xyz.pangosoft.dtodo.dto.AutorizacionProformaDto;
import xyz.pangosoft.dtodo.dto.PedidoDto;
import xyz.pangosoft.dtodo.dto.ProformaFechaDto;
import xyz.pangosoft.dtodo.dto.ProformaDto;
import xyz.pangosoft.dtodo.dto.DetalleDocumentoDto;
import xyz.pangosoft.dtodo.dto.DocumentoOrigenNotaDto;
import xyz.pangosoft.dtodo.dto.UsuarioDto;
import xyz.pangosoft.dtodo.dto.ReporteSelectorDto;
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

    /** Pedidos mayoristas vigentes (no anulados) de un cliente, del más antiguo al más reciente. */
    @Query("select p from Proforma p where p.cliente.idCliente = :idCliente "
            + "and p.tipoProforma = xyz.pangosoft.dtodo.model.enums.TipoProformaEnum.MAYORISTA "
            + "and p.estadoPedido is not null and p.estado.estado <> 'ANULADO' order by p.idProforma")
    List<Proforma> pedidosVigentesDelCliente(@Param("idCliente") Integer idCliente);

    /**
     * Factura que se muestra en el listado: la vigente (no anulada) y, si solo hubo anuladas, la última. Una
     * proforma puede tener varias facturas (anulada y reemitida); sin esto el listado repetiría la proforma.
     */
    String FACTURA_ACTUAL = "coalesce(" +
            "(select max(fv.idFactura) from Factura fv where fv.idProformaOrigen = p.idProforma and fv.estado.estado <> 'ANULADO'), " +
            "(select max(fa.idFactura) from Factura fa where fa.idProformaOrigen = p.idProforma))";

    String SELECT_AUTORIZACION = "select new xyz.pangosoft.dtodo.dto.AutorizacionProformaDto(" +
            "p.idProforma, p.noProforma, p.fechaEmision, p.total, c.nombre, rt.nombre, " +
            "trim(concat(coalesce(us.primerNombre, ''), ' ', coalesce(us.apellido, ''))), p.fechaVencimiento, " +
            "(select max(i.descuento) from Proforma p2 join p2.itemsProforma i where p2.idProforma = p.idProforma), ";

    /**
     * Pedidos mayoristas para la lista de pedidos. Sin importes. Más antiguos primero dentro de cada ruta.
     * {@code idRuta} e {@code idUsuario} son opcionales (nulos = todos).
     */
    @Query("select new xyz.pangosoft.dtodo.dto.PedidoDto(p.idProforma, p.noProforma, p.fechaEmision, p.fechaEntregaEstimada, " +
            "c.nombre, c.nombreNegocio, rt.idRuta, rt.nombre, " +
            "trim(concat(coalesce(u.primerNombre, ''), ' ', coalesce(u.apellido, ''))), p.estadoPedido, " +
            "(select coalesce(sum(i.cantidad), 0) from Proforma p2 join p2.itemsProforma i where p2.idProforma = p.idProforma), " +
            "p.estadoAutorizacion, rp.idUsuario, " +
            "trim(concat(coalesce(rp.primerNombre, ''), ' ', coalesce(rp.apellido, '')))) " +
            "from Proforma p join p.cliente c join p.usuario u left join p.ruta rt left join p.repartidor rp " +
            "where p.tipoProforma = :tipo and p.estadoPedido in :estados " +
            "and (:idRuta is null or rt.idRuta = :idRuta) and (:idUsuario is null or u.idUsuario = :idUsuario) " +
            "and (:idRepartidor is null or rp.idUsuario = :idRepartidor) " +
            "and p.fechaEmision >= :desde and p.fechaEmision < :hasta " +
            "order by rt.nombre, p.fechaEntregaEstimada, p.fechaEmision")
    List<PedidoDto> findPedidos(@Param("tipo") xyz.pangosoft.dtodo.model.enums.TipoProformaEnum tipo,
                                @Param("estados") List<xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum> estados,
                                @Param("idRuta") Integer idRuta, @Param("idUsuario") Integer idUsuario,
                                @Param("idRepartidor") Integer idRepartidor,
                                @Param("desde") Date desde, @Param("hasta") Date hasta, Pageable pageable);

    /** Proformas que esperan que un administrador apruebe o rechace su descuento, las más antiguas primero. */
    @Query(SELECT_AUTORIZACION + "'DESCUENTO') from Proforma p join p.cliente c join p.usuario us left join p.ruta rt " +
            "where p.estadoAutorizacion = :estado order by p.fechaEmision")
    List<AutorizacionProformaDto> findPorAutorizar(
            @Param("estado") xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum estado);

    /** Proformas mayoristas vencidas que todavía no se facturan ni tienen el vencimiento autorizado. */
    @Query(SELECT_AUTORIZACION + "'VENCIDA') from Proforma p join p.cliente c join p.usuario us left join p.ruta rt " +
            "where p.tipoProforma = :tipo and p.fechaVencimiento < :hoy and p.vencimientoAutorizado = false " +
            "and not exists (select 1 from Factura f where f.idProformaOrigen = p.idProforma and f.estado.estado <> 'ANULADO') " +
            "order by p.fechaVencimiento")
    List<AutorizacionProformaDto> findVencidasSinAutorizar(
            @Param("tipo") xyz.pangosoft.dtodo.model.enums.TipoProformaEnum tipo,
            @Param("hoy") java.time.LocalDate hoy);

    @Query("select distinct new xyz.pangosoft.dtodo.dto.ReporteSelectorDto(" +
            "u.idUsuario, trim(concat(concat(coalesce(u.primerNombre, ''), ' '), " +
            "concat(coalesce(u.segundoNombre, ''), concat(' ', coalesce(u.apellido, ''))))), " +
            "concat('Usuario: ', u.usuario)) from Proforma p join p.usuario u " +
            "where (:idSucursal is null or p.sucursal.idSucursal = :idSucursal) order by u.usuario")
    List<ReporteSelectorDto> findOpcionesUsuarioReporte(@Param("idSucursal") Integer idSucursal);

    @Query("select count(p) from Proforma p where (:idUsuario is null or p.usuario.idUsuario = :idUsuario)")
    Long countByUsuario(@Param("idUsuario") Integer idUsuario);

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
            "u.usuario, concat(coalesce(u.primerNombre, ''), ' ', coalesce(u.apellido, '')), c.nombre, c.nit, " +
            "f.idFactura, f.serie, f.noFactura, fe.estado, " +
            "concat(coalesce(fu.primerNombre, ''), ' ', coalesce(fu.apellido, '')), p.tipoProforma, rt.nombre, p.estadoAutorizacion) " +
            "from Proforma p join p.estado e join p.usuario u join p.cliente c " +
            "left join p.ruta rt " +
            "left join Factura f on f.idFactura = " + FACTURA_ACTUAL + " " +
            "left join f.estado fe left join f.usuario fu " +
            "where (:idUsuario is null or u.idUsuario = :idUsuario) " +
            "order by p.fechaEmision desc")
    List<ProformaDto> findUltimasListadoDto(@Param("idUsuario") Integer idUsuario, Pageable pageable);

    @Query("select new xyz.pangosoft.dtodo.dto.ProformaDto(" +
            "p.idProforma, p.noProforma, p.fechaEmision, p.total, e.idEstado, e.estado, " +
            "u.usuario, concat(coalesce(u.primerNombre, ''), ' ', coalesce(u.apellido, '')), c.nombre, c.nit, " +
            "f.idFactura, f.serie, f.noFactura, fe.estado, " +
            "concat(coalesce(fu.primerNombre, ''), ' ', coalesce(fu.apellido, '')), p.tipoProforma, rt.nombre, p.estadoAutorizacion) " +
            "from Proforma p join p.estado e join p.usuario u join p.cliente c " +
            "left join p.ruta rt " +
            "left join Factura f on f.idFactura = " + FACTURA_ACTUAL + " " +
            "left join f.estado fe left join f.usuario fu " +
            "where p.fechaEmision >= :fechaIni and p.fechaEmision < :fechaFin " +
            "and (:idUsuario is null or u.idUsuario = :idUsuario)")
    Page<ProformaDto> findAllListadoDto(@Param("fechaIni") Date fechaIni,
                                                @Param("fechaFin") Date fechaFin,
                                                @Param("idUsuario") Integer idUsuario,
                                                Pageable pageable);

    @Query("select new xyz.pangosoft.dtodo.dto.ProformaDto(" +
            "p.idProforma, p.noProforma, p.fechaEmision, p.total, e.idEstado, e.estado, " +
            "u.usuario, concat(coalesce(u.primerNombre, ''), ' ', coalesce(u.apellido, '')), c.nombre, c.nit, " +
            "f.idFactura, f.serie, f.noFactura, fe.estado, " +
            "concat(coalesce(fu.primerNombre, ''), ' ', coalesce(fu.apellido, '')), p.tipoProforma, rt.nombre, p.estadoAutorizacion) " +
            "from Proforma p join p.estado e join p.usuario u join p.cliente c " +
            "left join p.ruta rt " +
            "left join Factura f on f.idFactura = " + FACTURA_ACTUAL + " " +
            "left join f.estado fe left join f.usuario fu " +
            "where p.fechaEmision >= :fechaIni and p.fechaEmision < :fechaFin " +
            "and (:idUsuario is null or u.idUsuario = :idUsuario) and (" +
            "lower(c.nombre) like lower(concat('%', :filtro, '%')) or " +
            "lower(c.nit) like lower(concat('%', :filtro, '%')) or " +
            "lower(u.usuario) like lower(concat('%', :filtro, '%')) or " +
            "lower(concat(coalesce(u.primerNombre, ''), ' ', coalesce(u.apellido, ''))) like lower(concat('%', :filtro, '%')) or " +
            "lower(p.noProforma) like lower(concat('%', :filtro, '%')))")
    Page<ProformaDto> searchListadoDto(@Param("fechaIni") Date fechaIni,
                                               @Param("fechaFin") Date fechaFin,
                                               @Param("idUsuario") Integer idUsuario,
                                               @Param("filtro") String filtro,
                                               Pageable pageable);

    @Query(value = "SELECT d.id_detalle AS idDetalle, p.id_producto AS idProducto, " +
            "p.cod_producto AS codigoProducto, p.nombre AS producto, d.cantidad AS cantidad, " +
            "d.sub_total AS subTotal, d.descuento AS descuento, d.sub_total_descuento AS subTotalDescuento, " +
            "d.observacion AS observacion " +
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
