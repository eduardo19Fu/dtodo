package xyz.pangosoft.dtodo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import xyz.pangosoft.dtodo.model.ProformaEntrega;

public interface IProformaEntregaRepository extends JpaRepository<ProformaEntrega, Long> {

    /** Entregas de un pedido, de la más antigua a la más reciente (incluye las anuladas). */
    List<ProformaEntrega> findByIdProformaOrderByFechaRegistroAscIdEntregaAsc(Long idProforma);
}
