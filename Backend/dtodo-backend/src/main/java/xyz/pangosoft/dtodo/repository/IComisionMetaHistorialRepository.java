package xyz.pangosoft.dtodo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import xyz.pangosoft.dtodo.model.ComisionMetaHistorial;

public interface IComisionMetaHistorialRepository extends JpaRepository<ComisionMetaHistorial, Long> {

    /** Los cambios de las metas de un año, del más reciente al más antiguo. */
    List<ComisionMetaHistorial> findByAnioOrderByFechaDescIdHistorialDesc(Integer anio);
}
