package xyz.pangosoft.dtodo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import xyz.pangosoft.dtodo.model.ComisionMeta;

public interface IComisionMetaRepository extends JpaRepository<ComisionMeta, Integer> {

    /** Las metas del año, de la más baja a la más alta. */
    List<ComisionMeta> findByAnioOrderByMontoMinimo(Integer anio);

    void deleteByAnio(Integer anio);
}
