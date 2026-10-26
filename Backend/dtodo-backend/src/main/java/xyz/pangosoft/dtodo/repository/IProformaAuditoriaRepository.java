package xyz.pangosoft.dtodo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.pangosoft.dtodo.dto.ProformaAuditoriaDto;
import xyz.pangosoft.dtodo.model.ProformaAuditoria;

public interface IProformaAuditoriaRepository extends JpaRepository<ProformaAuditoria, Long> {

    /** Historial de una proforma, del más reciente al más antiguo. */
    @Query("select new xyz.pangosoft.dtodo.dto.ProformaAuditoriaDto(a.idAuditoria, a.accion, " +
            "trim(concat(coalesce(u.primerNombre, ''), ' ', coalesce(u.apellido, ''))), a.motivo, a.detalle, a.fecha) " +
            "from ProformaAuditoria a join a.usuario u where a.idProforma = :idProforma " +
            "order by a.fecha desc, a.idAuditoria desc")
    List<ProformaAuditoriaDto> findHistorial(@Param("idProforma") Long idProforma);
}
