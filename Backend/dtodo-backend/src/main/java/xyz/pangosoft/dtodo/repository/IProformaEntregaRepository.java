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
     * entregadas, importe con descuento de la línea y unidades de la línea. Solo pedidos del tipo indicado; las entregas
     * anuladas no cuentan. Quien llama calcula el valor de lo entregado como unidades × importe / unidades de la línea.
     */
    @Query("select p.usuario.idUsuario, ed.cantidad, l.subTotalDescuento, l.cantidad " +
            "from ProformaEntrega e, ProformaEntregaDetalle ed, DetalleProforma l, Proforma p " +
            "where ed.entrega = e and l.idDetalle = ed.idDetalle and p.idProforma = e.idProforma " +
            "and e.anulada = false and p.tipoProforma = :tipo and p.estado.estado <> 'ANULADO' " +
            "and e.fechaEntrega >= :desde and e.fechaEntrega < :hasta")
    List<Object[]> entregadoDelPeriodo(@Param("tipo") TipoProformaEnum tipo, @Param("desde") LocalDate desde,
                                       @Param("hasta") LocalDate hasta);

    /** Entregas de un pedido, de la más antigua a la más reciente (incluye las anuladas). */
    List<ProformaEntrega> findByIdProformaOrderByFechaRegistroAscIdEntregaAsc(Long idProforma);
}
