package xyz.pangosoft.dtodo.service;

import java.util.Map;

import xyz.pangosoft.dtodo.model.Factura;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;

/**
 * Inventario de la factura de un pedido mayorista. La entrega ya descuenta lo que se llevó al cliente, así que la
 * factura descuenta solo lo que el pedido todavía no había descontado; y al anularla devuelve solo lo que ella
 * descontó, dejando descontado lo que ya se entregó. Una factura que no viene de un pedido mayorista (venta directa
 * o proforma minorista) no pasa por aquí: se comporta como siempre y estos métodos responden {@code null} o
 * {@code false}.
 */
public interface IInventarioFacturaPedidoService {

    /**
     * Lo que la factura debe descontar por producto (id de producto → unidades), sin modificar nada. Es {@code null}
     * si la factura no viene de un pedido mayorista. Un producto con cero unidades ya estaba descontado.
     */
    Map<Integer, Integer> planearVenta(Factura factura);

    /** Falla (400, como la facturación) si la sucursal no tiene existencias para lo que hay que descontar. */
    void validarExistencias(Map<Integer, Integer> plan, Sucursal sucursal, Factura factura);

    /**
     * Descuenta lo planeado: un movimiento de venta por producto, lo anota en las líneas de la factura y suma lo
     * descontado a las líneas del pedido. La factura ya debe estar guardada (con su id).
     */
    void descontarVenta(Factura factura, Usuario usuario, Map<Integer, Integer> plan);

    /**
     * Anula el descuento de una factura de pedido mayorista: devuelve lo que ella descontó, menos lo que ya se
     * entregó físicamente (que sigue descontado). {@code false} si la factura no viene de un pedido mayorista, y
     * entonces quien llama aplica la devolución de siempre.
     */
    boolean devolverVenta(Factura factura, Usuario usuario);
}
