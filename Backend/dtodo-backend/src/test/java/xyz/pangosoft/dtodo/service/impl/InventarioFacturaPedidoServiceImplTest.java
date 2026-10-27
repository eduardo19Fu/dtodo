package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import xyz.pangosoft.dtodo.model.DetalleFactura;
import xyz.pangosoft.dtodo.model.DetalleProforma;
import xyz.pangosoft.dtodo.model.Factura;
import xyz.pangosoft.dtodo.model.MovimientoProducto;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum;
import xyz.pangosoft.dtodo.model.enums.TipoMovimientoEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.service.IMovimientoProductoService;

/** La factura de un pedido mayorista descuenta solo lo no descontado y devuelve solo lo que descontó. */
class InventarioFacturaPedidoServiceImplTest {

    private final IProformaRepository proformaRepository = mock(IProformaRepository.class);
    private final IMovimientoProductoService movimientoService = mock(IMovimientoProductoService.class);
    private final InventarioFacturaPedidoServiceImpl service =
            new InventarioFacturaPedidoServiceImpl(proformaRepository, movimientoService);

    private final Sucursal central = Sucursal.builder().idSucursal(1).nombre("Central").build();
    private final Usuario usuario = Usuario.builder().idUsuario(9).build();
    private final Producto cuadernos = Producto.builder().idProducto(1).nombre("CUADERNO").build();
    private final Producto reglas = Producto.builder().idProducto(2).nombre("REGLA").build();
    private Proforma pedido;
    private DetalleProforma lineaCuadernos;
    private DetalleProforma lineaReglas;

    @BeforeEach
    void escenario() {
        pedido = new Proforma();
        pedido.setIdProforma(7L);
        pedido.setTipoProforma(TipoProformaEnum.MAYORISTA);
        pedido.setEstadoPedido(EstadoPedidoEnum.EN_RUTA);
        lineaCuadernos = linea(cuadernos, 10);
        lineaReglas = linea(reglas, 5);
        pedido.getItemsProforma().add(lineaCuadernos);
        pedido.getItemsProforma().add(lineaReglas);
        when(proformaRepository.findById(7L)).thenReturn(Optional.of(pedido));
    }

    private DetalleProforma linea(Producto producto, int cantidad) {
        DetalleProforma linea = new DetalleProforma();
        linea.setProducto(producto);
        linea.setCantidad(cantidad);
        return linea;
    }

    private Factura factura(int cuadernosFacturados, int reglasFacturadas) {
        Factura factura = new Factura();
        factura.setIdFactura(30L);
        factura.setIdProformaOrigen(7L);
        factura.setSucursal(central);
        factura.getItemsFactura().add(item(cuadernos, cuadernosFacturados));
        factura.getItemsFactura().add(item(reglas, reglasFacturadas));
        return factura;
    }

    private DetalleFactura item(Producto producto, int cantidad) {
        DetalleFactura item = new DetalleFactura();
        item.setProducto(producto);
        item.setCantidad(cantidad);
        return item;
    }

    private List<MovimientoProducto> movimientos() {
        ArgumentCaptor<MovimientoProducto> captor = ArgumentCaptor.forClass(MovimientoProducto.class);
        verify(movimientoService, org.mockito.Mockito.atLeast(0)).save(captor.capture());
        return captor.getAllValues();
    }

    // ------------------------------------------------------------------------------------------ planearVenta

    @Test
    void unaVentaDirectaOUnaProformaMinoristaNoPasaPorElControl() {
        Factura directa = factura(1, 1);
        directa.setIdProformaOrigen(null);
        assertNull(service.planearVenta(directa));

        pedido.setTipoProforma(TipoProformaEnum.MINORISTA);
        assertNull(service.planearVenta(factura(1, 1)));

        pedido.setTipoProforma(TipoProformaEnum.MAYORISTA);
        pedido.setEstadoPedido(null);
        assertNull(service.planearVenta(factura(1, 1)));

        when(proformaRepository.findById(99L)).thenReturn(Optional.empty());
        Factura huerfana = factura(1, 1);
        huerfana.setIdProformaOrigen(99L);
        assertNull(service.planearVenta(huerfana));
        assertNull(service.planearVenta(null));
    }

    @Test
    void sinNadaDescontadoLaFacturaDescuentaTodo() {
        Map<Integer, Integer> plan = service.planearVenta(factura(10, 5));

        assertEquals(10, plan.get(1));
        assertEquals(5, plan.get(2));
    }

    @Test
    void loQueLaEntregaYaDescontoNoSeDescuentaOtraVez() {
        lineaCuadernos.setCantidadEntregada(10);
        lineaCuadernos.setCantidadDescontada(10);
        lineaReglas.setCantidadEntregada(2);
        lineaReglas.setCantidadDescontada(2);

        Map<Integer, Integer> plan = service.planearVenta(factura(10, 5));

        assertEquals(0, plan.get(1));
        assertEquals(3, plan.get(2));
    }

    @Test
    void unProductoQueNoEstaEnElPedidoSeDescuentaCompleto() {
        Factura factura = factura(10, 5);
        Producto extra = Producto.builder().idProducto(3).nombre("LAPIZ").build();
        factura.getItemsFactura().add(item(extra, 4));

        assertEquals(4, service.planearVenta(factura).get(3));
    }

    @Test
    void siSeFacturaMenosDeLoDescontadoNoSeDescuentaNada() {
        lineaCuadernos.setCantidadDescontada(10);

        assertEquals(0, service.planearVenta(factura(6, 5)).get(1));
    }

    // ------------------------------------------------------------------------------------------ descontarVenta

    @Test
    void descontarVentaMueveSoloLoPlaneadoLoAnotaEnLaFacturaYLoSumaAlPedido() {
        lineaCuadernos.setCantidadEntregada(4);
        lineaCuadernos.setCantidadDescontada(4);
        Factura factura = factura(10, 5);
        Map<Integer, Integer> plan = service.planearVenta(factura);

        service.descontarVenta(factura, usuario, plan);

        List<MovimientoProducto> movimientos = movimientos();
        assertEquals(2, movimientos.size());
        assertEquals(6, movimientos.get(0).getCantidad());
        assertEquals(TipoMovimientoEnum.VENTA, movimientos.get(0).getTipoMovimiento());
        assertEquals("FACTURA", movimientos.get(0).getTipoDocumentoOrigen());
        assertEquals(Long.valueOf(30L), movimientos.get(0).getIdDocumentoOrigen());
        assertEquals(5, movimientos.get(1).getCantidad());
        assertEquals(6, factura.getItemsFactura().get(0).getCantidadDescontada());
        assertEquals(5, factura.getItemsFactura().get(1).getCantidadDescontada());
        assertEquals(10, lineaCuadernos.getCantidadDescontada());
        assertEquals(5, lineaReglas.getCantidadDescontada());
        verify(proformaRepository).save(pedido);
    }

    @Test
    void sinNadaQueDescontarNoHayMovimientosPeroLaFacturaQuedaAnotadaEnCero() {
        lineaCuadernos.setCantidadEntregada(10);
        lineaCuadernos.setCantidadDescontada(10);
        lineaReglas.setCantidadEntregada(5);
        lineaReglas.setCantidadDescontada(5);
        Factura factura = factura(10, 5);

        service.descontarVenta(factura, usuario, service.planearVenta(factura));

        verify(movimientoService, never()).save(any());
        assertEquals(0, factura.getItemsFactura().get(0).getCantidadDescontada());
    }

    @Test
    void siUnProductoVieneRepetidoEnLaFacturaSeAnotaEnLaPrimeraLinea() {
        Factura factura = factura(6, 5);
        factura.getItemsFactura().add(item(cuadernos, 4));

        service.descontarVenta(factura, usuario, service.planearVenta(factura));

        assertEquals(10, factura.getItemsFactura().get(0).getCantidadDescontada() + factura.getItemsFactura().get(2).getCantidadDescontada());
        assertEquals(0, factura.getItemsFactura().get(2).getCantidadDescontada());
        verify(movimientoService, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void validarExistenciasSoloExigeLoQueSeVaADescontar() {
        Factura factura = factura(10, 5);
        Map<Integer, Integer> plan = new LinkedHashMap<>();
        plan.put(1, 0);
        plan.put(2, 3);

        service.validarExistencias(plan, central, factura);

        verify(movimientoService).validarStockDisponible(argThat(items -> items.size() == 1
                && items.get(0).getProducto().getIdProducto() == 2 && items.get(0).getCantidad() == 3), org.mockito.ArgumentMatchers.eq(central));
    }

    @Test
    void validarExistenciasSinNadaQueDescontarNoConsultaElInventario() {
        Map<Integer, Integer> plan = new LinkedHashMap<>();
        plan.put(1, 0);
        plan.put(2, 0);

        service.validarExistencias(plan, central, factura(10, 5));

        verify(movimientoService, never()).validarStockDisponible(any(), any());
    }

    // ------------------------------------------------------------------------------------------ devolverVenta

    private Factura facturaYaDescontada(int cuadernosDescontados, int reglasDescontadas) {
        Factura factura = factura(10, 5);
        factura.getItemsFactura().get(0).setCantidadDescontada(cuadernosDescontados);
        factura.getItemsFactura().get(1).setCantidadDescontada(reglasDescontadas);
        lineaCuadernos.setCantidadDescontada(cuadernosDescontados);
        lineaReglas.setCantidadDescontada(reglasDescontadas);
        return factura;
    }

    @Test
    void sinEntregasAnularLaFacturaDevuelveTodoLoQueDescontoYDejaElPedidoSinDescontar() {
        Factura factura = facturaYaDescontada(10, 5);

        assertTrue(service.devolverVenta(factura, usuario));

        List<MovimientoProducto> movimientos = movimientos();
        assertEquals(2, movimientos.size());
        assertEquals(TipoMovimientoEnum.ANULACION_FACTURA, movimientos.get(0).getTipoMovimiento());
        assertEquals(10, movimientos.get(0).getCantidad());
        assertEquals(5, movimientos.get(1).getCantidad());
        assertEquals(0, lineaCuadernos.getCantidadDescontada());
        assertEquals(0, lineaReglas.getCantidadDescontada());
    }

    @Test
    void loQueSeEntregoDespuesDeFacturarSigueDescontadoAlAnularLaFactura() {
        Factura factura = facturaYaDescontada(10, 5);
        lineaCuadernos.setCantidadEntregada(10);
        lineaReglas.setCantidadEntregada(2);

        service.devolverVenta(factura, usuario);

        List<MovimientoProducto> movimientos = movimientos();
        assertEquals(1, movimientos.size());
        assertEquals(2, movimientos.get(0).getProducto().getIdProducto());
        assertEquals(3, movimientos.get(0).getCantidad());
        assertEquals(10, lineaCuadernos.getCantidadDescontada());
        assertEquals(2, lineaReglas.getCantidadDescontada());
    }

    @Test
    void siLaEntregaYaHabiaDescontadoLaFacturaSoloDevuelveLoSuyo() {
        // Entregó 4 (descontó 4) y la factura descontó los 6 restantes.
        Factura factura = facturaYaDescontada(6, 5);
        lineaCuadernos.setCantidadEntregada(4);
        lineaCuadernos.setCantidadDescontada(10);

        service.devolverVenta(factura, usuario);

        List<MovimientoProducto> movimientos = movimientos();
        assertEquals(6, movimientos.get(0).getCantidad());
        assertEquals(4, lineaCuadernos.getCantidadDescontada());
    }

    @Test
    void unaFacturaQueNoDescontoNadaNoDevuelveNada() {
        Factura factura = facturaYaDescontada(0, 0);
        lineaCuadernos.setCantidadEntregada(10);
        lineaCuadernos.setCantidadDescontada(10);
        lineaReglas.setCantidadEntregada(5);
        lineaReglas.setCantidadDescontada(5);

        service.devolverVenta(factura, usuario);

        verify(movimientoService, never()).save(any());
        assertEquals(10, lineaCuadernos.getCantidadDescontada());
    }

    @Test
    void unaFacturaAnteriorAlControlDevuelveLaCantidadCompletaSinTocarLoDescontadoDelPedido() {
        Factura factura = factura(10, 5);
        lineaCuadernos.setCantidadEntregada(10);
        lineaCuadernos.setCantidadDescontada(10);

        service.devolverVenta(factura, usuario);

        List<MovimientoProducto> movimientos = movimientos();
        assertEquals(2, movimientos.size());
        assertEquals(10, movimientos.get(0).getCantidad());
        assertEquals(10, lineaCuadernos.getCantidadDescontada());
    }

    @Test
    void devolverUnaVentaQueNoEsDeUnPedidoMayoristaNoHaceNada() {
        pedido.setTipoProforma(TipoProformaEnum.MINORISTA);

        assertFalse(service.devolverVenta(factura(10, 5), usuario));

        verify(movimientoService, never()).save(any());
        verify(proformaRepository, never()).save(any());
    }

    @Test
    void lasLineasRepetidasDeUnProductoSeCubrenPrimeroConLoEntregado() {
        DetalleProforma otra = linea(cuadernos, 6);
        pedido.getItemsProforma().add(otra);
        lineaCuadernos.setCantidadEntregada(3);
        lineaCuadernos.setCantidadDescontada(3);
        otra.setCantidadEntregada(2);
        otra.setCantidadDescontada(2);
        Factura factura = factura(16, 5);

        Map<Integer, Integer> plan = service.planearVenta(factura);
        assertEquals(11, plan.get(1));
        service.descontarVenta(factura, usuario, plan);

        assertEquals(10, lineaCuadernos.getCantidadDescontada());
        assertEquals(6, otra.getCantidadDescontada());
        assertEquals(16, lineaCuadernos.getCantidadDescontada() + otra.getCantidadDescontada());
    }
}
