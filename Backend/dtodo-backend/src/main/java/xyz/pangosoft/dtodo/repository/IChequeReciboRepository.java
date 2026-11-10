package xyz.pangosoft.dtodo.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.pangosoft.dtodo.model.ChequeRecibo;
import xyz.pangosoft.dtodo.model.enums.EstadoChequeEnum;

public interface IChequeReciboRepository extends JpaRepository<ChequeRecibo, Long> {

    Optional<ChequeRecibo> findByRecibo_IdRecibo(Long idRecibo);

    /** Todos los cheques del filtro (para los reportes), del pago más reciente al más antiguo. */
    @Query("select c from ChequeRecibo c join fetch c.recibo r where (:idSucursal is null or r.sucursal.idSucursal = :idSucursal) "
            + "and (:idCliente is null or r.cliente.idCliente = :idCliente) "
            + "and (:estado is null or c.estado = :estado) "
            + "and r.fechaPago >= :desde and r.fechaPago <= :hasta "
            + "order by r.fechaPago desc, c.idCheque desc")
    List<ChequeRecibo> buscar(@Param("idSucursal") Integer idSucursal, @Param("idCliente") Integer idCliente,
            @Param("estado") EstadoChequeEnum estado, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /** La misma búsqueda, paginada, para la pantalla de control. */
    @Query(value = "select c from ChequeRecibo c join c.recibo r where (:idSucursal is null or r.sucursal.idSucursal = :idSucursal) "
            + "and (:idCliente is null or r.cliente.idCliente = :idCliente) "
            + "and (:estado is null or c.estado = :estado) "
            + "and r.fechaPago >= :desde and r.fechaPago <= :hasta order by r.fechaPago desc, c.idCheque desc",
            countQuery = "select count(c) from ChequeRecibo c join c.recibo r where (:idSucursal is null or r.sucursal.idSucursal = :idSucursal) "
                    + "and (:idCliente is null or r.cliente.idCliente = :idCliente) "
                    + "and (:estado is null or c.estado = :estado) "
                    + "and r.fechaPago >= :desde and r.fechaPago <= :hasta")
    Page<ChequeRecibo> pagina(@Param("idSucursal") Integer idSucursal, @Param("idCliente") Integer idCliente,
            @Param("estado") EstadoChequeEnum estado, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
            Pageable pageable);

    /** ¿Ya se recibió un cheque vigente (no rechazado) con este banco y número? Evita registrar el mismo cheque dos veces. */
    @Query("select count(c) > 0 from ChequeRecibo c where lower(c.banco) = lower(:banco) and c.numeroCheque = :numero "
            + "and c.estado <> xyz.pangosoft.dtodo.model.enums.EstadoChequeEnum.RECHAZADO")
    boolean existeVigente(@Param("banco") String banco, @Param("numero") String numero);
}
