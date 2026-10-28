package xyz.pangosoft.dtodo.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.pangosoft.dtodo.model.DevolucionCliente;

public interface IDevolucionClienteRepository extends JpaRepository<DevolucionCliente, Long> {

    /** Lo devuelto por pedido de un cliente: pedido y valor. */
    @Query("select d.idProforma, sum(l.valor) from DevolucionClienteDetalle l join l.devolucion d "
            + "where d.cliente.idCliente = :idCliente group by d.idProforma")
    List<Object[]> devueltoDelCliente(@Param("idCliente") Integer idCliente);

    /** Lo devuelto por pedido de todos los clientes: cliente, pedido y valor. */
    @Query("select d.cliente.idCliente, d.idProforma, sum(l.valor) from DevolucionClienteDetalle l join l.devolucion d "
            + "group by d.cliente.idCliente, d.idProforma")
    List<Object[]> devueltoDeTodos();

    /** El valor devuelto de un pedido. */
    @Query("select coalesce(sum(l.valor), 0) from DevolucionClienteDetalle l join l.devolucion d where d.idProforma = :idProforma")
    BigDecimal devueltoDelPedido(@Param("idProforma") Long idProforma);

    /** Lo devuelto en un período por vendedor del pedido: vendedor y valor (para las ventas netas). */
    @Query("select p.usuario.idUsuario, sum(l.valor) from DevolucionClienteDetalle l join l.devolucion d, Proforma p "
            + "where p.idProforma = d.idProforma and d.fecha >= :desde and d.fecha < :hasta group by p.usuario.idUsuario")
    List<Object[]> devueltoDelPeriodoPorVendedor(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    List<DevolucionCliente> findByCliente_IdClienteOrderByFechaAscIdDevolucionAsc(Integer idCliente);

    List<DevolucionCliente> findByIdProformaOrderByFechaRegistroAscIdDevolucionAsc(Long idProforma);

    /** Devoluciones filtradas por cliente (opcional) y por fecha, de la más reciente a la más antigua. */
    @Query("select d from DevolucionCliente d where (:idCliente is null or d.cliente.idCliente = :idCliente) "
            + "and d.fecha >= :desde and d.fecha <= :hasta order by d.fecha desc, d.idDevolucion desc")
    Page<DevolucionCliente> buscar(@Param("idCliente") Integer idCliente, @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta, Pageable pageable);
}
