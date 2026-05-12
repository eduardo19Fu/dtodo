package com.aglayatech.licorstore.repository;

import com.aglayatech.licorstore.dto.NotaCreditoListDto;
import com.aglayatech.licorstore.model.NotaCredito;
import com.aglayatech.licorstore.model.enums.EstadoNotaCreditoEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface INotaCreditoRepository extends JpaRepository<NotaCredito, Long> {

    List<NotaCredito> findNotasByEstado(EstadoNotaCreditoEnum estado);

    /**
     * Verifica si ya existe una Nota de Crédito emitida sobre la Factura indicada
     * (independiente de su estado actual: PENDIENTE, ENTREGADO o ANULADO).
     *
     * @param correlativoFacturaSat correlativo SAT de la factura
     * @param serieFacturaSat       serie SAT de la factura
     * @return {@code true} si existe al menos una nota; {@code false} en caso contrario
     */
    boolean existsByCorrelativoFacturaSatAndSerieFacturaSat(String correlativoFacturaSat, String serieFacturaSat);

    /**
     * Verifica si ya existe una Nota de Crédito emitida sobre la Proforma indicada.
     *
     * @param noProforma número único de proforma
     * @return {@code true} si existe al menos una nota; {@code false} en caso contrario
     */
    boolean existsByNoProforma(String noProforma);

    @Query("SELECT new com.aglayatech.licorstore.dto.NotaCreditoListDto("
            + "nc.idNotaCredito, nc.total, nc.usuario.usuario, nc.correlativoFacturaSat, "
            + "nc.serieFacturaSat, nc.tipoDocumentoOrigen, nc.noProforma, "
            + "nc.fechaCreacion, nc.fechaEntregaEstimada, nc.estado) "
            + "FROM NotaCredito nc "
            + "ORDER BY nc.idNotaCredito DESC")
    List<NotaCreditoListDto> findAllAsDto();

    @Query("SELECT new com.aglayatech.licorstore.dto.NotaCreditoListDto("
            + "nc.idNotaCredito, nc.total, nc.usuario.usuario, nc.correlativoFacturaSat, "
            + "nc.serieFacturaSat, nc.tipoDocumentoOrigen, nc.noProforma, "
            + "nc.fechaCreacion, nc.fechaEntregaEstimada, nc.estado) "
            + "FROM NotaCredito nc "
            + "WHERE nc.estado = ?1 "
            + "ORDER BY nc.idNotaCredito DESC")
    List<NotaCreditoListDto> findByEstadoAsDto(EstadoNotaCreditoEnum estado);
}
