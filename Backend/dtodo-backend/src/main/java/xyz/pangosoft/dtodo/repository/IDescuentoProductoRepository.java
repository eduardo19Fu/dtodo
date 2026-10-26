package xyz.pangosoft.dtodo.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.pangosoft.dtodo.model.DescuentoProducto;
import xyz.pangosoft.dtodo.model.Producto;

public interface IDescuentoProductoRepository extends JpaRepository<DescuentoProducto, Integer> {

    List<DescuentoProducto> findByProducto_IdProductoOrderByTipo(Integer idProducto);

    List<DescuentoProducto> findByProducto_IdProductoIn(Collection<Integer> idsProducto);

    /**
     * Productos para la pantalla de descuentos, con búsqueda por nombre o código. Con {@code soloConDescuentos}
     * quedan solo los que ya tienen un descuento especial o un precio mayorista.
     */
    @Query(value = "select p from Producto p where " +
            "(:filtro = '' or lower(p.nombre) like lower(concat('%', :filtro, '%')) " +
            "or lower(coalesce(p.codProducto, '')) like lower(concat('%', :filtro, '%'))) " +
            "and (:soloConDescuentos = false " +
            "or exists (select 1 from DescuentoProducto d where d.producto = p) " +
            "or exists (select 1 from PrecioMayorista m where m.idProducto = p.idProducto))",
            countQuery = "select count(p) from Producto p where " +
                    "(:filtro = '' or lower(p.nombre) like lower(concat('%', :filtro, '%')) " +
                    "or lower(coalesce(p.codProducto, '')) like lower(concat('%', :filtro, '%'))) " +
                    "and (:soloConDescuentos = false " +
                    "or exists (select 1 from DescuentoProducto d where d.producto = p) " +
                    "or exists (select 1 from PrecioMayorista m where m.idProducto = p.idProducto))")
    Page<Producto> buscarProductos(@Param("filtro") String filtro,
                                   @Param("soloConDescuentos") boolean soloConDescuentos, Pageable pageable);
}
