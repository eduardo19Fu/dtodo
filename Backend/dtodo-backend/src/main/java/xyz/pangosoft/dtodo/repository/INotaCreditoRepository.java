package xyz.pangosoft.dtodo.repository;

import xyz.pangosoft.dtodo.dto.NotaCreditoDto;
import xyz.pangosoft.dtodo.model.NotaCredito;
import xyz.pangosoft.dtodo.model.enums.EstadoNotaCreditoEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    @Query("SELECT new xyz.pangosoft.dtodo.dto.NotaCreditoDto("
            + "nc.idNotaCredito, nc.total, nc.usuario.usuario, nc.cliente.nombre, nc.cliente.nit, nc.correlativoFacturaSat, "
            + "nc.serieFacturaSat, nc.tipoDocumentoOrigen, nc.noProforma, "
            + "nc.fechaCreacion, nc.fechaEntregaEstimada, nc.estado) "
            + "FROM NotaCredito nc "
            + "ORDER BY nc.idNotaCredito DESC")
    List<NotaCreditoDto> findAllAsDto();

    @Query("SELECT new xyz.pangosoft.dtodo.dto.NotaCreditoDto("
            + "nc.idNotaCredito, nc.total, nc.usuario.usuario, nc.cliente.nombre, nc.cliente.nit, nc.correlativoFacturaSat, "
            + "nc.serieFacturaSat, nc.tipoDocumentoOrigen, nc.noProforma, "
            + "nc.fechaCreacion, nc.fechaEntregaEstimada, nc.estado) "
            + "FROM NotaCredito nc "
            + "WHERE nc.estado = ?1 "
            + "ORDER BY nc.idNotaCredito DESC")
    List<NotaCreditoDto> findByEstadoAsDto(EstadoNotaCreditoEnum estado);

    @Query("SELECT new xyz.pangosoft.dtodo.dto.NotaCreditoDto(" +
            "nc.idNotaCredito, nc.total, nc.usuario.usuario, nc.cliente.nombre, nc.cliente.nit, " +
            "nc.correlativoFacturaSat, nc.serieFacturaSat, nc.tipoDocumentoOrigen, nc.noProforma, " +
            "nc.fechaCreacion, nc.fechaEntregaEstimada, nc.estado) " +
            "FROM NotaCredito nc ORDER BY nc.fechaCreacion DESC")
    List<NotaCreditoDto> findUltimasAsDto(Pageable pageable);

    @Query("SELECT new xyz.pangosoft.dtodo.dto.NotaCreditoDto(" +
            "nc.idNotaCredito, nc.total, nc.usuario.usuario, nc.cliente.nombre, nc.cliente.nit, " +
            "nc.correlativoFacturaSat, nc.serieFacturaSat, nc.tipoDocumentoOrigen, nc.noProforma, " +
            "nc.fechaCreacion, nc.fechaEntregaEstimada, nc.estado) " +
            "FROM NotaCredito nc WHERE nc.fechaCreacion >= :fechaIni AND nc.fechaCreacion < :fechaFin " +
            "AND (:filtro = '' OR lower(nc.cliente.nombre) LIKE lower(concat('%', :filtro, '%')) " +
            "OR lower(nc.cliente.nit) LIKE lower(concat('%', :filtro, '%')) " +
            "OR lower(nc.usuario.usuario) LIKE lower(concat('%', :filtro, '%')) " +
            "OR lower(coalesce(nc.serieFacturaSat, '')) LIKE lower(concat('%', :filtro, '%')) " +
            "OR lower(coalesce(nc.correlativoFacturaSat, '')) LIKE lower(concat('%', :filtro, '%')) " +
            "OR lower(coalesce(nc.noProforma, '')) LIKE lower(concat('%', :filtro, '%')) " +
            "OR lower(str(nc.idNotaCredito)) LIKE lower(concat('%', :filtro, '%')) " +
            "OR lower(str(nc.tipoDocumentoOrigen)) LIKE lower(concat('%', :filtro, '%')) " +
            "OR lower(str(nc.estado)) LIKE lower(concat('%', :filtro, '%'))) " +
            "ORDER BY nc.fechaCreacion DESC")
    Page<NotaCreditoDto> findByFechasAsDto(@Param("fechaIni") LocalDateTime fechaIni,
                                                @Param("fechaFin") LocalDateTime fechaFin,
                                                @Param("filtro") String filtro,
                                                Pageable pageable);
}
