package xyz.pangosoft.dtodo.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.pangosoft.dtodo.model.DetalleFactura;
import xyz.pangosoft.dtodo.model.DetalleProforma;
import xyz.pangosoft.dtodo.model.Factura;
import xyz.pangosoft.dtodo.model.MovimientoProducto;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.TipoMovimientoEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.service.IInventarioFacturaPedidoService;
import xyz.pangosoft.dtodo.service.IMovimientoProductoService;

/**
 * Por cada producto, el pedido lleva la cuenta de lo que ya salió del inventario por su causa
 * ({@code cantidadDescontada}) y de lo que se entregó físicamente ({@code cantidadEntregada}); siempre
 * {@code descontada >= entregada}. La factura sube lo descontado hasta lo facturado; al anularla se devuelve lo que
 * ella descontó, pero lo entregado vuelve a quedar cubierto.
 */
@Service
@RequiredArgsConstructor
public class InventarioFacturaPedidoServiceImpl implements IInventarioFacturaPedidoService {

    static final String ORIGEN = "FACTURA";

    private final IProformaRepository proformaRepository;
    private final IMovimientoProductoService movimientoService;

    @Transactional(readOnly = true)
    @Override
    public Map<Integer, Integer> planearVenta(Factura factura) {
        Proforma pedido = pedidoDe(factura);
        if (pedido == null) {
            return null;
        }
        Map<Integer, Integer> facturado = sumarPorProducto(factura.getItemsFactura());
        Map<Integer, Integer> plan = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> producto : facturado.entrySet()) {
            int descontada = sumaDeLineas(pedido, producto.getKey(), true);
            // Un producto que no está en el pedido se descuenta completo, como una venta cualquiera.
            plan.put(producto.getKey(), Math.max(0, producto.getValue() - descontada));
        }
        return plan;
    }

    @Override
    public void validarExistencias(Map<Integer, Integer> plan, Sucursal sucursal, Factura factura) {
        List<DetalleFactura> aDescontar = new ArrayList<>();
        Map<Integer, Producto> productos = new LinkedHashMap<>();
        for (DetalleFactura item : factura.getItemsFactura()) {
            productos.putIfAbsent(item.getProducto().getIdProducto(), item.getProducto());
        }
        for (Map.Entry<Integer, Integer> entrada : plan.entrySet()) {
            if (entrada.getValue() > 0) {
                DetalleFactura item = new DetalleFactura();
                item.setProducto(productos.get(entrada.getKey()));
                item.setCantidad(entrada.getValue());
                aDescontar.add(item);
            }
        }
        if (!aDescontar.isEmpty()) {
            movimientoService.validarStockDisponible(aDescontar, sucursal);
        }
    }

    @Transactional
    @Override
    public void descontarVenta(Factura factura, Usuario usuario, Map<Integer, Integer> plan) {
        Proforma pedido = pedidoDe(factura);
        Map<Integer, Boolean> anotado = new LinkedHashMap<>();
        for (DetalleFactura item : factura.getItemsFactura()) {
            Integer idProducto = item.getProducto().getIdProducto();
            int descontar = plan.getOrDefault(idProducto, 0);
            // Lo descontado de un producto se anota en su primera línea de la factura; las repetidas quedan en cero.
            item.setCantidadDescontada(anotado.putIfAbsent(idProducto, Boolean.TRUE) == null ? descontar : 0);
            if (anotado.get(idProducto) && item.getCantidadDescontada() > 0) {
                movimientoService.save(movimiento(TipoMovimientoEnum.VENTA, item.getProducto(), item.getCantidadDescontada(),
                        factura, usuario));
            }
        }
        if (pedido != null) {
            for (Map.Entry<Integer, Integer> producto : plan.entrySet()) {
                if (producto.getValue() > 0) {
                    ajustarDescontada(pedido, producto.getKey(), producto.getValue());
                }
            }
            proformaRepository.save(pedido);
        }
    }

    @Transactional
    @Override
    public boolean devolverVenta(Factura factura, Usuario usuario) {
        Proforma pedido = pedidoDe(factura);
        if (pedido == null) {
            return false;
        }
        Map<Integer, Producto> productos = new LinkedHashMap<>();
        Map<Integer, Integer> descontado = new LinkedHashMap<>();
        Map<Integer, Integer> reduccion = new LinkedHashMap<>();
        for (DetalleFactura item : factura.getItemsFactura()) {
            Integer idProducto = item.getProducto().getIdProducto();
            productos.putIfAbsent(idProducto, item.getProducto());
            // Una factura anterior a este control no anotó lo descontado: descontó todo y no sumó nada al pedido.
            descontado.merge(idProducto, item.getCantidadDescontada() == null ? item.getCantidad() : item.getCantidadDescontada(),
                    Integer::sum);
            reduccion.merge(idProducto, item.getCantidadDescontada() == null ? 0 : item.getCantidadDescontada(), Integer::sum);
        }
        for (Map.Entry<Integer, Integer> producto : descontado.entrySet()) {
            Integer idProducto = producto.getKey();
            int descontadaAntes = sumaDeLineas(pedido, idProducto, true);
            int entregada = sumaDeLineas(pedido, idProducto, false);
            int descontadaSinLaFactura = Math.max(0, descontadaAntes - reduccion.get(idProducto));
            // Lo que ya se entregó sigue fuera del inventario aunque la factura que lo cubría se anule.
            int vuelveACubrirse = Math.max(0, entregada - descontadaSinLaFactura);
            int devolver = Math.max(0, producto.getValue() - vuelveACubrirse);
            fijarDescontada(pedido, idProducto, descontadaSinLaFactura + vuelveACubrirse);
            if (devolver > 0) {
                movimientoService.save(movimiento(TipoMovimientoEnum.ANULACION_FACTURA, productos.get(idProducto), devolver,
                        factura, usuario));
            }
        }
        proformaRepository.save(pedido);
        return true;
    }

    // ------------------------------------------------------------------------------------------ apoyo

    /** El pedido mayorista del que viene la factura; {@code null} si es una venta directa o de una minorista. */
    private Proforma pedidoDe(Factura factura) {
        if (factura == null || factura.getIdProformaOrigen() == null) {
            return null;
        }
        Proforma proforma = proformaRepository.findById(factura.getIdProformaOrigen()).orElse(null);
        return proforma != null && proforma.getTipoProforma() == TipoProformaEnum.MAYORISTA && proforma.getEstadoPedido() != null
                ? proforma : null;
    }

    private static Map<Integer, Integer> sumarPorProducto(List<DetalleFactura> items) {
        Map<Integer, Integer> suma = new LinkedHashMap<>();
        for (DetalleFactura item : items) {
            suma.merge(item.getProducto().getIdProducto(), item.getCantidad(), Integer::sum);
        }
        return suma;
    }

    /** Suma, entre las líneas del pedido de ese producto, lo descontado ({@code true}) o lo entregado ({@code false}). */
    private static int sumaDeLineas(Proforma pedido, Integer idProducto, boolean descontada) {
        int total = 0;
        for (DetalleProforma linea : pedido.getItemsProforma()) {
            if (linea.getProducto() != null && idProducto.equals(linea.getProducto().getIdProducto())) {
                total += valor(descontada ? linea.getCantidadDescontada() : linea.getCantidadEntregada());
            }
        }
        return total;
    }

    private static void ajustarDescontada(Proforma pedido, Integer idProducto, int adicional) {
        fijarDescontada(pedido, idProducto, sumaDeLineas(pedido, idProducto, true) + adicional);
    }

    /**
     * Reparte lo descontado de un producto entre sus líneas del pedido: primero cubre lo entregado de cada una y lo
     * que sobra se reparte, en orden, hasta completar cada línea.
     */
    private static void fijarDescontada(Proforma pedido, Integer idProducto, int total) {
        List<DetalleProforma> lineas = new ArrayList<>();
        for (DetalleProforma linea : pedido.getItemsProforma()) {
            if (linea.getProducto() != null && idProducto.equals(linea.getProducto().getIdProducto())) {
                lineas.add(linea);
            }
        }
        if (lineas.isEmpty()) {
            return;
        }
        int restante = total;
        for (DetalleProforma linea : lineas) {
            int cubierto = Math.min(restante, valor(linea.getCantidadEntregada()));
            linea.setCantidadDescontada(cubierto);
            restante -= cubierto;
        }
        for (DetalleProforma linea : lineas) {
            int hueco = Math.max(0, linea.getCantidad() - linea.getCantidadDescontada());
            int extra = Math.min(restante, hueco);
            linea.setCantidadDescontada(linea.getCantidadDescontada() + extra);
            restante -= extra;
        }
        if (restante > 0) {
            // Se facturó más de lo que dice el pedido: el exceso queda en la primera línea para no perderlo.
            DetalleProforma primera = lineas.get(0);
            primera.setCantidadDescontada(primera.getCantidadDescontada() + restante);
        }
    }

    private MovimientoProducto movimiento(TipoMovimientoEnum tipo, Producto producto, int cantidad, Factura factura, Usuario usuario) {
        return MovimientoProducto.builder()
                .tipoMovimiento(tipo)
                .tipoDocumentoOrigen(ORIGEN)
                .idDocumentoOrigen(factura.getIdFactura())
                .usuario(usuario)
                .producto(producto)
                .sucursal(factura.getSucursal())
                .cantidad(cantidad)
                .build();
    }

    private static int valor(Integer numero) {
        return numero == null ? 0 : numero;
    }
}
