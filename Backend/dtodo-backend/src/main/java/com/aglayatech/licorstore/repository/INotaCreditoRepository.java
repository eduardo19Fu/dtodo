package com.aglayatech.licorstore.repository;

import com.aglayatech.licorstore.dto.NotaCreditoListDto;
import com.aglayatech.licorstore.model.NotaCredito;
import com.aglayatech.licorstore.model.enums.EstadoNotaCreditoEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface INotaCreditoRepository extends JpaRepository<NotaCredito, Long> {

    public List<NotaCredito> findNotasByEstado(EstadoNotaCreditoEnum estado);

    @Query("SELECT new com.aglayatech.licorstore.dto.NotaCreditoListDto("
            + "nc.idNotaCredito, nc.total, nc.usuario.usuario, nc.correlativoFacturaSat, "
            + "nc.serieFacturaSat, nc.fechaCreacion, nc.fechaEntregaEstimada, nc.estado) "
            + "FROM NotaCredito nc "
            + "ORDER BY nc.idNotaCredito DESC")
    List<NotaCreditoListDto> findAllAsDto();

    @Query("SELECT new com.aglayatech.licorstore.dto.NotaCreditoListDto("
            + "nc.idNotaCredito, nc.total, nc.usuario.usuario, nc.correlativoFacturaSat, "
            + "nc.serieFacturaSat, nc.fechaCreacion, nc.fechaEntregaEstimada, nc.estado) "
            + "FROM NotaCredito nc "
            + "WHERE nc.estado = ?1 "
            + "ORDER BY nc.idNotaCredito DESC")
    List<NotaCreditoListDto> findByEstadoAsDto(EstadoNotaCreditoEnum estado);
}
