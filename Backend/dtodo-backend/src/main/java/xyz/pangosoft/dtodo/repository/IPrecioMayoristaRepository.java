package xyz.pangosoft.dtodo.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import xyz.pangosoft.dtodo.model.PrecioMayorista;

public interface IPrecioMayoristaRepository extends JpaRepository<PrecioMayorista, Integer> {

    List<PrecioMayorista> findByIdProductoIn(Collection<Integer> idsProducto);
}
