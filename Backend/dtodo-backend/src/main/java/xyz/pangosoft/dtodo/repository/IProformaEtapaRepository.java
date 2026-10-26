package xyz.pangosoft.dtodo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.pangosoft.dtodo.dto.PedidoEtapaDto;
import xyz.pangosoft.dtodo.model.ProformaEtapa;

public interface IProformaEtapaRepository extends JpaRepository<ProformaEtapa, Long> {

    /** Etapas del pedido, de la más antigua a la más reciente. */
    @Query("select new xyz.pangosoft.dtodo.dto.PedidoEtapaDto(e.idEtapa, e.etapa, " +
            "trim(concat(coalesce(u.primerNombre, ''), ' ', coalesce(u.apellido, ''))), e.responsable, e.motivo, e.fecha) " +
            "from ProformaEtapa e join e.usuario u where e.idProforma = :idProforma " +
            "order by e.fecha, e.idEtapa")
    List<PedidoEtapaDto> findHistorial(@Param("idProforma") Long idProforma);
}
