package xyz.pangosoft.dtodo.dto;

import java.util.Collection;

import lombok.Builder;
import lombok.Data;
import xyz.pangosoft.dtodo.model.enums.TipoClienteEnum;

/** Criterios del listado de clientes. Todos son opcionales. */
@Data
@Builder
public class ClienteFiltro {

    /** Texto libre: se busca cada palabra en nombre, NIT, teléfono, dirección, código, negocio y contacto. */
    private String texto;
    private TipoClienteEnum tipoCliente;
    private Integer idRuta;

    /**
     * Rutas a las que se limita el listado (el vendedor solo ve las suyas). {@code null} no restringe; una
     * colección vacía no devuelve ningún cliente.
     */
    private Collection<Integer> idsRutasPermitidas;

    /** Solo clientes cuyo NIT está escrito pero no es válido (por ejemplo, un teléfono). */
    private boolean soloNitInvalido;
}
