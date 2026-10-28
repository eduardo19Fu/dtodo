package xyz.pangosoft.dtodo.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.pangosoft.dtodo.model.ReciboCaja;

public interface IReciboCajaRepository extends JpaRepository<ReciboCaja, Long> {

    /**
     * Recibos vigentes de un cliente: pedido al que paga, pedido de origen de la factura a la que paga y monto. Si no indican
     * ninguno de los dos, son abonos a la cuenta del cliente.
     */
    @Query("select pr.idProforma, f.idProformaOrigen, r.monto from ReciboCaja r left join r.proforma pr left join r.factura f "
            + "where r.cliente.idCliente = :idCliente and r.anulado = false")
    List<Object[]> abonosDelCliente(@Param("idCliente") Integer idCliente);

    /** Los recibos vigentes de todos los clientes: cliente, pedido, pedido de origen de la factura y monto. */
    @Query("select r.cliente.idCliente, pr.idProforma, f.idProformaOrigen, r.monto from ReciboCaja r "
            + "left join r.proforma pr left join r.factura f where r.anulado = false")
    List<Object[]> abonosDeTodos();

    /** Los recibos vigentes de un cliente, del más antiguo al más reciente. */
    List<ReciboCaja> findByCliente_IdClienteAndAnuladoFalseOrderByFechaPagoAscIdReciboAsc(Integer idCliente);

    /** Lo cobrado, con recibos vigentes, directamente a un pedido (ligados a él o a una factura suya). */
    @Query("select coalesce(sum(r.monto), 0) from ReciboCaja r left join r.proforma pr left join r.factura f "
            + "where r.anulado = false and (pr.idProforma = :idProforma or (pr is null and f.idProformaOrigen = :idProforma))")
    BigDecimal cobradoDelPedido(@Param("idProforma") Long idProforma);

    /** Recibos filtrados por sucursal y cliente (opcionales) y por fecha de pago; los anulados solo si se piden. */
    @Query("select r from ReciboCaja r where (:idSucursal is null or r.sucursal.idSucursal = :idSucursal) "
            + "and (:idCliente is null or r.cliente.idCliente = :idCliente) "
            + "and r.fechaPago >= :desde and r.fechaPago <= :hasta "
            + "and (:incluirAnulados = true or r.anulado = false) "
            + "order by r.fechaPago desc, r.idRecibo desc")
    Page<ReciboCaja> buscar(@Param("idSucursal") Integer idSucursal, @Param("idCliente") Integer idCliente,
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
            @Param("incluirAnulados") boolean incluirAnulados, Pageable pageable);
}
