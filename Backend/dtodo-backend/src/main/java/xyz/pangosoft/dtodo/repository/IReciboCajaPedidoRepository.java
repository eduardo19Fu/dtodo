package xyz.pangosoft.dtodo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.pangosoft.dtodo.model.ReciboCajaPedido;

public interface IReciboCajaPedidoRepository extends JpaRepository<ReciboCajaPedido, Long> {

    List<ReciboCajaPedido> findByRecibo_IdReciboOrderByIdPartida(Long idRecibo);

    /** Los recibos vigentes que pagaron (en parte) un pedido junto con otros, del más antiguo al más reciente. */
    @Query("select rp from ReciboCajaPedido rp join fetch rp.recibo r where rp.proforma.idProforma = :idProforma and r.anulado = false "
            + "order by r.fechaRegistro, r.idRecibo")
    List<ReciboCajaPedido> vigentesDelPedido(@Param("idProforma") Long idProforma);
}
