package xyz.pangosoft.dtodo.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.pangosoft.dtodo.model.ProformaEntrega;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;

public interface IProformaEntregaRepository extends JpaRepository<ProformaEntrega, Long> {

    /**
     * Lo entregado en un período (por la fecha real de entrega), una fila por línea entregada: vendedor del pedido, unidades
     * entregadas, importe con descuento de la línea, unidades de la línea y fecha de entrega. Solo pedidos del tipo indicado; las entregas
     * anuladas no cuentan. Quien llama calcula el valor de lo entregado como unidades × importe / unidades de la línea.
     */
    @Query("select p.usuario.idUsuario, ed.cantidad, l.subTotalDescuento, l.cantidad, e.fechaEntrega " +
            "from ProformaEntrega e, ProformaEntregaDetalle ed, DetalleProforma l, Proforma p " +
            "where ed.entrega = e and l.idDetalle = ed.idDetalle and p.idProforma = e.idProforma " +
            "and e.anulada = false and p.tipoProforma = :tipo and p.estado.estado <> 'ANULADO' " +
            "and e.fechaEntrega >= :desde and e.fechaEntrega < :hasta")
    List<Object[]> entregadoDelPeriodo(@Param("tipo") TipoProformaEnum tipo, @Param("desde") LocalDate desde,
                                       @Param("hasta") LocalDate hasta);

    /**
     * Lo entregado en los pedidos mayoristas vigentes de un cliente, una fila por línea entregada: pedido, unidades
     * entregadas, importe con descuento de la línea, unidades de la línea y fecha de entrega. Las entregas anuladas no cuentan.
     */
    @Query("select p.idProforma, ed.cantidad, l.subTotalDescuento, l.cantidad, e.fechaEntrega " +
            "from ProformaEntrega e, ProformaEntregaDetalle ed, DetalleProforma l, Proforma p " +
            "where ed.entrega = e and l.idDetalle = ed.idDetalle and p.idProforma = e.idProforma " +
            "and e.anulada = false and p.tipoProforma = xyz.pangosoft.dtodo.model.enums.TipoProformaEnum.MAYORISTA " +
            "and p.estado.estado <> 'ANULADO' and p.cliente.idCliente = :idCliente")
    List<Object[]> entregadoDelCliente(@Param("idCliente") Integer idCliente);

    /** Lo mismo para un solo pedido (para saber si ya está cobrado lo que se va a entregar). */
    @Query("select p.idProforma, ed.cantidad, l.subTotalDescuento, l.cantidad, e.fechaEntrega " +
            "from ProformaEntrega e, ProformaEntregaDetalle ed, DetalleProforma l, Proforma p " +
            "where ed.entrega = e and l.idDetalle = ed.idDetalle and p.idProforma = e.idProforma " +
            "and e.anulada = false and p.idProforma = :idProforma")
    List<Object[]> entregadoDelPedido(@Param("idProforma") Long idProforma);

    /** Entregas de un pedido, de la más antigua a la más reciente (incluye las anuladas). */
    List<ProformaEntrega> findByIdProformaOrderByFechaRegistroAscIdEntregaAsc(Long idProforma);
}
