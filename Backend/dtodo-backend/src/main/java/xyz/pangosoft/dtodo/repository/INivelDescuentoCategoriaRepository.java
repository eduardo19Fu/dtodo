package xyz.pangosoft.dtodo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import xyz.pangosoft.dtodo.model.NivelDescuentoCategoria;
import xyz.pangosoft.dtodo.model.enums.CategoriaClienteEnum;

public interface INivelDescuentoCategoriaRepository extends JpaRepository<NivelDescuentoCategoria, Integer> {

    /** Todos los niveles de una categoría, activos o no, para poder reactivar uno que se había quitado. */
    List<NivelDescuentoCategoria> findByCategoriaOrderByPorcentajeAsc(CategoriaClienteEnum categoria);

    List<NivelDescuentoCategoria> findByCategoriaAndActivoTrueOrderByPorcentajeAsc(CategoriaClienteEnum categoria);
}
