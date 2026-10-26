package xyz.pangosoft.dtodo.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.dto.ClienteDto;
import xyz.pangosoft.dtodo.dto.RutaDto;
import xyz.pangosoft.dtodo.model.Ruta;

public interface IRutaService {

    Page<RutaDto> findListado(String filtro, Pageable pageable);

    /** Rutas activas, para los selectores. */
    List<RutaDto> findActivas();

    /** Rutas activas que atiende un vendedor. */
    List<RutaDto> findActivasDeVendedor(Integer idUsuario);

    Ruta findById(Integer idRuta);

    RutaDto findDtoById(Integer idRuta);

    /** Crea la ruta (si no trae id) o actualiza sus campos editables. */
    RutaDto save(Ruta ruta);

    /** Cartera de la ruta: sus clientes ordenados por nombre. */
    List<ClienteDto> findClientes(Integer idRuta);
}
