package xyz.pangosoft.dtodo.repository;

import xyz.pangosoft.dtodo.model.TipoFactura;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ITipoFacturaRepository extends JpaRepository<TipoFactura, Integer> {
}
