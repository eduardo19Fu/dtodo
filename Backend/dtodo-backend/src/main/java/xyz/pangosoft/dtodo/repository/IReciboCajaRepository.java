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
            + "where r.cliente.idCliente = :idCliente and r.anulado = false "
            + "and not exists (select 1 from ReciboCajaPedido rp where rp.recibo = r)")
    List<Object[]> abonosSencillosDelCliente(@Param("idCliente") Integer idCliente);

    /** Lo que los recibos vigentes de varios pedidos aplicaron a cada pedido del cliente: pedido y monto. */
    @Query("select rp.proforma.idProforma, rp.monto from ReciboCajaPedido rp "
            + "where rp.recibo.cliente.idCliente = :idCliente and rp.recibo.anulado = false")
    List<Object[]> partidasDelCliente(@Param("idCliente") Integer idCliente);

    /** Los abonos vigentes del cliente: los de un recibo sencillo y las partidas de los recibos de varios pedidos. */
    default List<Object[]> abonosDelCliente(Integer idCliente) {
        List<Object[]> filas = new java.util.ArrayList<>(abonosSencillosDelCliente(idCliente));
        for (Object[] partida : partidasDelCliente(idCliente)) {
            filas.add(new Object[] { partida[0], null, partida[1] });
        }
        return filas;
    }

    /** Los recibos vigentes de todos los clientes: cliente, pedido, pedido de origen de la factura y monto. */
    @Query("select r.cliente.idCliente, pr.idProforma, f.idProformaOrigen, r.monto from ReciboCaja r "
            + "left join r.proforma pr left join r.factura f where r.anulado = false "
            + "and not exists (select 1 from ReciboCajaPedido rp where rp.recibo = r)")
    List<Object[]> abonosSencillosDeTodos();

    @Query("select rp.recibo.cliente.idCliente, rp.proforma.idProforma, rp.monto from ReciboCajaPedido rp where rp.recibo.anulado = false")
    List<Object[]> partidasDeTodos();

    /** Los recibos vigentes de todos los clientes: cliente, pedido, pedido de origen de la factura y monto (con las partidas). */
    default List<Object[]> abonosDeTodos() {
        List<Object[]> filas = new java.util.ArrayList<>(abonosSencillosDeTodos());
        for (Object[] partida : partidasDeTodos()) {
            filas.add(new Object[] { partida[0], partida[1], null, partida[2] });
        }
        return filas;
    }

    /** Todos los recibos ligados directamente a un pedido, también los anulados, del más antiguo al más reciente. */
    List<ReciboCaja> findByProforma_IdProformaOrderByFechaRegistroAscIdReciboAsc(Long idProforma);

    /** Los recibos vigentes de un cliente, del más antiguo al más reciente. */
    List<ReciboCaja> findByCliente_IdClienteAndAnuladoFalseOrderByFechaPagoAscIdReciboAsc(Integer idCliente);

    /** Lo cobrado, con recibos vigentes, directamente a un pedido (ligados a él o a una factura suya). */
    @Query("select coalesce(sum(r.monto), 0) from ReciboCaja r left join r.proforma pr left join r.factura f "
            + "where r.anulado = false and (pr.idProforma = :idProforma or (pr is null and f.idProformaOrigen = :idProforma)) "
            + "and not exists (select 1 from ReciboCajaPedido rp where rp.recibo = r)")
    BigDecimal cobradoSencilloDelPedido(@Param("idProforma") Long idProforma);

    @Query("select coalesce(sum(rp.monto), 0) from ReciboCajaPedido rp where rp.proforma.idProforma = :idProforma and rp.recibo.anulado = false")
    BigDecimal cobradoPorPartidasDelPedido(@Param("idProforma") Long idProforma);

    /** Los recibos de varios pedidos que aplicaron algo a este pedido (vigentes o no). */
    @Query("select distinct rp.recibo from ReciboCajaPedido rp where rp.proforma.idProforma = :idProforma")
    List<ReciboCaja> recibosPorPartidas(@Param("idProforma") Long idProforma);

    /** Todos los recibos del pedido: los ligados a él y los de varios pedidos que le aplicaron algo, en orden de emisión. */
    default List<ReciboCaja> recibosDelPedido(Long idProforma) {
        List<ReciboCaja> todos = new java.util.ArrayList<>(findByProforma_IdProformaOrderByFechaRegistroAscIdReciboAsc(idProforma));
        todos.addAll(recibosPorPartidas(idProforma));
        todos.sort(java.util.Comparator.comparing(ReciboCaja::getFechaRegistro, java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())));
        return todos;
    }

    /** Lo cobrado a un pedido con recibos vigentes: los ligados a él (o a su factura) y lo que le aplicaron los recibos de varios pedidos. */
    default BigDecimal cobradoDelPedido(Long idProforma) {
        BigDecimal sencillo = cobradoSencilloDelPedido(idProforma);
        BigDecimal partidas = cobradoPorPartidasDelPedido(idProforma);
        return (sencillo == null ? BigDecimal.ZERO : sencillo).add(partidas == null ? BigDecimal.ZERO : partidas);
    }

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
