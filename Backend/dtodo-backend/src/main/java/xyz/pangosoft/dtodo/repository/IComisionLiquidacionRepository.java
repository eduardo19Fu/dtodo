package xyz.pangosoft.dtodo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import xyz.pangosoft.dtodo.model.ComisionLiquidacion;

public interface IComisionLiquidacionRepository extends JpaRepository<ComisionLiquidacion, Long> {

    List<ComisionLiquidacion> findByAnio(Integer anio);

    boolean existsByAnio(Integer anio);

    void deleteByAnio(Integer anio);
}
