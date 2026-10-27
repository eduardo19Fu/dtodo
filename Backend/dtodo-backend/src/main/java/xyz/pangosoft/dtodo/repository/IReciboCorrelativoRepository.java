package xyz.pangosoft.dtodo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import xyz.pangosoft.dtodo.model.ReciboCorrelativo;

public interface IReciboCorrelativoRepository extends JpaRepository<ReciboCorrelativo, Integer> {

    /** El correlativo de la sucursal con la fila bloqueada hasta el fin de la transacción, para no repetir números. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ReciboCorrelativo c where c.idSucursal = :idSucursal")
    Optional<ReciboCorrelativo> bloquear(@Param("idSucursal") Integer idSucursal);
}
