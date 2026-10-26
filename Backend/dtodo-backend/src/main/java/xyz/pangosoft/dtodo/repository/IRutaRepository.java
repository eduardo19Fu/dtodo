package xyz.pangosoft.dtodo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import xyz.pangosoft.dtodo.dto.RutaDto;
import xyz.pangosoft.dtodo.model.Ruta;

public interface IRutaRepository extends JpaRepository<Ruta, Integer> {

    String SELECT_DTO = "select new xyz.pangosoft.dtodo.dto.RutaDto(" +
            "r.idRuta, r.nombre, r.zona, r.diasVisita, v.idUsuario, " +
            "concat(coalesce(v.primerNombre, ''), ' ', coalesce(v.apellido, '')), " +
            "s.idSucursal, s.nombre, e.idEstado, e.estado, " +
            "(select count(c) from Cliente c where c.ruta = r)) " +
            "from Ruta r left join r.vendedor v join r.sucursal s join r.estado e ";

    @Query(value = SELECT_DTO +
            "where (:filtro = '' " +
            "or lower(r.nombre) like lower(concat('%', :filtro, '%')) " +
            "or lower(coalesce(r.zona, '')) like lower(concat('%', :filtro, '%')) " +
            "or lower(coalesce(r.diasVisita, '')) like lower(concat('%', :filtro, '%')) " +
            "or lower(concat(coalesce(v.primerNombre, ''), ' ', coalesce(v.apellido, ''))) like lower(concat('%', :filtro, '%')) " +
            "or lower(s.nombre) like lower(concat('%', :filtro, '%')))",
            countQuery = "select count(r) from Ruta r left join r.vendedor v join r.sucursal s join r.estado e " +
                    "where (:filtro = '' " +
                    "or lower(r.nombre) like lower(concat('%', :filtro, '%')) " +
                    "or lower(coalesce(r.zona, '')) like lower(concat('%', :filtro, '%')) " +
                    "or lower(coalesce(r.diasVisita, '')) like lower(concat('%', :filtro, '%')) " +
                    "or lower(concat(coalesce(v.primerNombre, ''), ' ', coalesce(v.apellido, ''))) like lower(concat('%', :filtro, '%')) " +
                    "or lower(s.nombre) like lower(concat('%', :filtro, '%')))")
    Page<RutaDto> findListado(@Param("filtro") String filtro, Pageable pageable);

    /** Rutas activas para los selectores (formulario de cliente, filtros). */
    @Query(SELECT_DTO + "where e.estado = 'ACTIVO' order by r.nombre")
    List<RutaDto> findActivasDto();

    /** Ids de todas las rutas que atiende un vendedor, activas o no: su cartera sigue siendo suya. */
    @Query("select r.idRuta from Ruta r where r.vendedor.idUsuario = :idUsuario")
    List<Integer> findIdsByVendedor(@Param("idUsuario") Integer idUsuario);

    /** Rutas activas de un vendedor, para el selector al registrar clientes. */
    @Query(SELECT_DTO + "where v.idUsuario = :idUsuario and e.estado = 'ACTIVO' order by r.nombre")
    List<RutaDto> findActivasDtoByVendedor(@Param("idUsuario") Integer idUsuario);

    @Query(SELECT_DTO + "where r.idRuta = :idRuta")
    Optional<RutaDto> findDtoById(@Param("idRuta") Integer idRuta);

    boolean existsByNombreIgnoreCaseAndSucursal_IdSucursal(String nombre, Integer idSucursal);

    boolean existsByNombreIgnoreCaseAndSucursal_IdSucursalAndIdRutaNot(String nombre, Integer idSucursal, Integer idRuta);
}
