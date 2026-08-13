package xyz.pangosoft.dtodo.repository;

import xyz.pangosoft.dtodo.dto.DespachoNotaDto;
import xyz.pangosoft.dtodo.model.DespachoNota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IDespachoNotaRepository extends JpaRepository<DespachoNota, Long> {

    List<DespachoNota> findByNotaCreditoIdNotaCredito(Long idNotaCredito);

    List<DespachoNota> findByIdEvento(String idEvento);

    @Query("SELECT new xyz.pangosoft.dtodo.dto.DespachoNotaDto("
            + "d.idDespacho, d.idEvento, d.fechaDespacho, d.codProducto, "
            + "d.producto.idProducto, d.producto.nombre, d.cantidad, d.totalDespacho, "
            + "d.usuario.usuario) "
            + "FROM DespachoNota d "
            + "WHERE d.notaCredito.idNotaCredito = ?1 "
            + "ORDER BY d.fechaDespacho DESC, d.idDespacho DESC")
    List<DespachoNotaDto> findByNotaCreditoAsDto(Long idNotaCredito);

    @Query("SELECT new xyz.pangosoft.dtodo.dto.DespachoNotaDto("
            + "d.idDespacho, d.idEvento, d.fechaDespacho, d.codProducto, "
            + "d.producto.idProducto, d.producto.nombre, d.cantidad, d.totalDespacho, "
            + "d.usuario.usuario) "
            + "FROM DespachoNota d "
            + "WHERE d.idEvento = ?1 "
            + "ORDER BY d.idDespacho")
    List<DespachoNotaDto> findByIdEventoAsDto(String idEvento);

    @Query("SELECT COALESCE(SUM(d.cantidad), 0) FROM DespachoNota d WHERE d.notaCredito.idNotaCredito = ?1 AND d.producto.idProducto = ?2")
    int totalDespachado(Long idNotaCredito, Integer idProducto);
}
