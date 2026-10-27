package xyz.pangosoft.dtodo.service;

import java.util.List;

import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.Factura;
import xyz.pangosoft.dtodo.model.Proforma;

/**
 * Alcance del vendedor: solo ve y trabaja con los clientes de sus rutas y con sus propias proformas. Todo el
 * resto de los roles conserva su alcance: cada método devuelve lo recibido, o no hace nada, cuando la sesión no
 * es de un vendedor acotado (ver {@code Utils.esVendedorAcotado()}).
 *
 * <p>Se aplica en el servidor porque el backend no puede confiar en los ids que envía el navegador: el usuario
 * sale siempre del token. Las reglas de negocio responden 422 (401 y 403 los reserva el interceptor del
 * frontend) y lo ajeno se informa como inexistente para no revelar qué hay fuera del alcance.</p>
 */
public interface IAmbitoVendedorService {

    /** {@code true} si la sesión es de un vendedor acotado a sus rutas. */
    boolean esVendedor();

    /** El usuario de la sesión es un repartidor acotado (ver {@code Utils.esRepartidorAcotado()}). */
    boolean esRepartidor();

    /** El id del repartidor de la sesión, para limitar listados a sus pedidos; {@code null} si no es un repartidor acotado. */
    Integer idRepartidorForzado();

    /**
     * Un repartidor solo ve los pedidos que se le asignaron y que siguen por entregar; de otro modo responde como si no
     * existieran (404). No hace nada para los demás roles.
     */
    void exigirPedidoAsignado(Proforma proforma);

    /** Id del usuario de la sesión, tomado del token. */
    Integer idUsuarioActual();

    /**
     * Ids de las rutas que atiende el vendedor de la sesión; {@code null} (sin restricción) si no es un vendedor
     * acotado. Una lista vacía significa que no tiene rutas y, por tanto, no ve ningún cliente.
     */
    List<Integer> idsRutasPermitidas();

    /** Un vendedor solo ve a los clientes de sus rutas; los demás roles ven a todos. */
    boolean clienteEnAmbito(Cliente cliente);

    /** Lectura de un cliente: lo devuelve o, si es de otra ruta, responde como si no existiera (404). */
    Cliente limitarLectura(Cliente cliente);

    /** 422 si el cliente no es de una ruta del vendedor. */
    void exigirClienteEnAmbito(Cliente cliente);

    /**
     * Alta de un cliente por un vendedor: queda como mayorista en una de sus rutas, sin categoría ni crédito (los
     * fija el administrador), y su NIT no puede estar ya registrado con otro cliente.
     */
    Cliente prepararAlta(Cliente cliente);

    /**
     * Edición de un cliente por un vendedor: carga el cliente (404 si no es de sus rutas) y le aplica solo los
     * campos que puede cambiar: dirección, contacto, teléfono de contacto y referencia de ubicación.
     */
    Cliente prepararEdicion(Cliente solicitud);

    /** Un vendedor usa siempre su propio id; los demás roles, el solicitado. */
    Integer forzarUsuario(Integer idUsuarioSolicitado);

    /** 404 si la proforma no es del vendedor. */
    void exigirProformaPropia(Proforma proforma);

    /** 422 si la proforma ya tiene factura. */
    void exigirProformaSinFactura(Proforma proforma);

    /** El vendedor queda como autor de la proforma y esta solo puede ir a un cliente de sus rutas. */
    void prepararProforma(Proforma proforma);

    /**
     * Factura emitida por un vendedor: debe provenir de una proforma suya, queda a su nombre (con su
     * correlativo) y solo puede ir a un cliente de sus rutas.
     */
    void prepararFactura(Factura factura);

    /** 404 si el id no es el del propio vendedor (por ejemplo, para consultar su usuario o su correlativo). */
    void exigirUsuarioPropio(Integer idUsuario);
}
