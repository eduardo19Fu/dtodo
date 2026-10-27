package xyz.pangosoft.dtodo.repository;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.pangosoft.dtodo.model.ReciboCaja;

public interface IReciboCajaRepository extends JpaRepository<ReciboCaja, Long> {

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
