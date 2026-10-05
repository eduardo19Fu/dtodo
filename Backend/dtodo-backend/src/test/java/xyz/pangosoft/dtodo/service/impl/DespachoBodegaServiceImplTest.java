package xyz.pangosoft.dtodo.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;

import xyz.pangosoft.dtodo.dto.DespachoBodegaRequest;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Bodega;
import xyz.pangosoft.dtodo.model.DespachoBodega;
import xyz.pangosoft.dtodo.model.DespachoBodegaDetalle;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.MovimientoBodega;
import xyz.pangosoft.dtodo.model.MovimientoProducto;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.EstadoDespachoBodegaEnum;
import xyz.pangosoft.dtodo.model.enums.TipoMovimientoBodegaEnum;
import xyz.pangosoft.dtodo.model.enums.TipoMovimientoEnum;
import xyz.pangosoft.dtodo.repository.IDespachoBodegaRepository;
import xyz.pangosoft.dtodo.service.IBodegaService;
import xyz.pangosoft.dtodo.service.IInventarioBodegaService;
import xyz.pangosoft.dtodo.service.IMovimientoProductoService;
import xyz.pangosoft.dtodo.service.IProductoService;
import xyz.pangosoft.dtodo.service.ISucursalService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DespachoBodegaServiceImplTest {

    private final IDespachoBodegaRepository despachoRepo = mock(IDespachoBodegaRepository.class);
    private final IBodegaService bodegaService = mock(IBodegaService.class);
    private final IInventarioBodegaService inventarioBodegaService = mock(IInventarioBodegaService.class);
    private final IProductoService productoService = mock(IProductoService.class);
    private final ISucursalService sucursalService = mock(ISucursalService.class);
    private final IUsuarioService usuarioService = mock(IUsuarioService.class);
    private final IMovimientoProductoService movimientoProductoService = mock(IMovimientoProductoService.class);

    private final DespachoBodegaServiceImpl service = new DespachoBodegaServiceImpl(
            despachoRepo, bodegaService, inventarioBodegaService, productoService, sucursalService, usuarioService,
            movimientoProductoService);

    private final Estado activo = Estado.builder().idEstado(1).estado("ACTIVO").build();
    private final Sucursal norte = Sucursal.builder().idSucursal(2).nombre("Norte").estado(activo).build();
    private final Bodega bodega = Bodega.builder().idBodega(1).nombre("Principal").estado(activo).sucursal(norte).build();
    private final Usuario bodeguero = Usuario.builder().idUsuario(9).usuario("bodeguero").build();
    private final Usuario admin = Usuario.builder().idUsuario(1).usuario("admin").build();
    private final Producto cuaderno = Producto.builder().idProducto(10).codProducto("C-10").nombre("Cuaderno")
            .precioCompra(new BigDecimal("12.50")).build();
    private final Producto lapiz = Producto.builder().idProducto(11).codProducto("L-11").nombre("Lápiz")
            .precioCompra(new BigDecimal("1.25")).build();

    private void prepararEntorno() {
        when(bodegaService.findActivaById(1)).thenReturn(bodega);
        when(sucursalService.findById(2)).thenReturn(norte);
        when(usuarioService.findById(9)).thenReturn(bodeguero);
        when(usuarioService.findById(1)).thenReturn(admin);
        when(productoService.findById(10)).thenReturn(cuaderno);
        when(productoService.findById(11)).thenReturn(lapiz);
        when(despachoRepo.save(any(DespachoBodega.class))).thenAnswer(invocation -> {
            DespachoBodega despacho = invocation.getArgument(0);
            if (despacho.getIdDespacho() == null) {
                despacho.setIdDespacho(77L);
            }
            return despacho;
        });
        when(inventarioBodegaService.registrarMovimiento(any(), any(), any(), anyInt(), anyString(), any(), any(), any()))
                .thenReturn(new MovimientoBodega());
    }

    private DespachoBodegaRequest solicitud(DespachoBodegaRequest.Linea... lineas) {
        DespachoBodegaRequest request = new DespachoBodegaRequest();
        request.setIdBodega(1);
        request.setRecibidoPor("  Maria López ");
        request.setItems(new ArrayList<>(List.of(lineas)));
        return request;
    }

    private DespachoBodegaRequest.Linea linea(int idProducto, int cantidad) {
        return new DespachoBodegaRequest.Linea(idProducto, cantidad);
    }

    // ---------- crear ----------

    @Test
    void creaUnDespachoPendienteConTotalesExistenciaYMovimientosDeBodega() {
        prepararEntorno();
        when(inventarioBodegaService.obtenerStockParaActualizar(bodega, cuaderno)).thenReturn(50);
        when(inventarioBodegaService.obtenerStockParaActualizar(bodega, lapiz)).thenReturn(200);

        DespachoBodega despacho = service.crear(solicitud(linea(10, 4), linea(11, 20)), 9);

        assertEquals(EstadoDespachoBodegaEnum.PENDIENTE, despacho.getEstado());
        assertEquals("Maria López", despacho.getRecibidoPor());
        assertEquals(bodeguero, despacho.getUsuarioDespacha());
        assertNull(despacho.getUsuarioResuelve());
        assertNull(despacho.getFechaResolucion());
        assertEquals(norte, despacho.getSucursalDestino());
        // 4 x 12.50 + 20 x 1.25 = 50.00 + 25.00
        assertEquals(new BigDecimal("75.00"), despacho.getTotal());

        DespachoBodegaDetalle primero = despacho.getItems().get(0);
        assertEquals(cuaderno, primero.getProducto());
        assertEquals(new BigDecimal("12.50"), primero.getPrecioUnitario());
        assertEquals(new BigDecimal("50.00"), primero.getSubTotal());
        assertEquals(50, primero.getExistenciaBodega());

        verify(inventarioBodegaService).registrarMovimiento(eq(bodega), eq(cuaderno),
                eq(TipoMovimientoBodegaEnum.DESPACHO), eq(4), anyString(), eq(bodeguero), eq("DESPACHO_BODEGA"), eq(77L));
        verify(inventarioBodegaService).registrarMovimiento(eq(bodega), eq(lapiz),
                eq(TipoMovimientoBodegaEnum.DESPACHO), eq(20), anyString(), eq(bodeguero), eq("DESPACHO_BODEGA"), eq(77L));
    }

    @Test
    void sumaLasLineasRepetidasDeUnMismoProductoYLasOrdenaPorProducto() {
        prepararEntorno();
        when(inventarioBodegaService.obtenerStockParaActualizar(any(), any())).thenReturn(100);

        DespachoBodega despacho = service.crear(solicitud(linea(11, 2), linea(10, 3), linea(11, 1)), 9);

        assertEquals(2, despacho.getItems().size());
        assertEquals(10, despacho.getItems().get(0).getProducto().getIdProducto());
        assertEquals(3, despacho.getItems().get(0).getCantidad());
        assertEquals(11, despacho.getItems().get(1).getProducto().getIdProducto());
        assertEquals(3, despacho.getItems().get(1).getCantidad());
    }

    @Test
    void usaLaSucursalAsignadaALaBodegaSiNoSeIndicaDestinoYLaIndicadaSiSeEnvia() {
        prepararEntorno();
        when(inventarioBodegaService.obtenerStockParaActualizar(any(), any())).thenReturn(100);
        Sucursal sur = Sucursal.builder().idSucursal(3).nombre("Sur").estado(activo).build();
        when(sucursalService.findById(3)).thenReturn(sur);

        assertEquals(norte, service.crear(solicitud(linea(10, 1)), 9).getSucursalDestino());

        DespachoBodegaRequest conDestino = solicitud(linea(10, 1));
        conDestino.setIdSucursalDestino(3);
        assertEquals(sur, service.crear(conDestino, 9).getSucursalDestino());
    }

    @Test
    void rechazaUnDespachoSinSucursalDestinoSiLaBodegaNoTieneSucursal() {
        prepararEntorno();
        Bodega sinSucursal = Bodega.builder().idBodega(1).nombre("Suelta").estado(activo).build();
        when(bodegaService.findActivaById(1)).thenReturn(sinSucursal);
        when(inventarioBodegaService.obtenerStockParaActualizar(any(), any())).thenReturn(100);

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.crear(solicitud(linea(10, 1)), 9));

        assertTrue(error.getMessage().contains("sucursal destino"));
        verify(despachoRepo, never()).save(any(DespachoBodega.class));
    }

    @Test
    void rechazaUnaSucursalDestinoInactiva() {
        prepararEntorno();
        Sucursal cerrada = Sucursal.builder().idSucursal(3).nombre("Cerrada")
                .estado(Estado.builder().idEstado(2).estado("INACTIVO").build()).build();
        when(sucursalService.findById(3)).thenReturn(cerrada);
        DespachoBodegaRequest request = solicitud(linea(10, 1));
        request.setIdSucursalDestino(3);

        assertThrows(BadRequestException.class, () -> service.crear(request, 9));

        verify(despachoRepo, never()).save(any(DespachoBodega.class));
    }

    @Test
    void rechazaLaCantidadMayorALaExistenciaDeLaBodegaSinGuardarNiMoverStock() {
        prepararEntorno();
        when(inventarioBodegaService.obtenerStockParaActualizar(bodega, cuaderno)).thenReturn(3);

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.crear(solicitud(linea(10, 4)), 9));

        assertTrue(error.getMessage().contains("Disponible: 3, solicitado: 4"));
        verify(despachoRepo, never()).save(any(DespachoBodega.class));
        verify(inventarioBodegaService, never()).registrarMovimiento(
                any(), any(), any(), anyInt(), anyString(), any(), any(), any());
    }

    @Test
    void validaLaSolicitudAntesDeConsultarNada() {
        DespachoBodegaRequest sinBodega = solicitud(linea(10, 1));
        sinBodega.setIdBodega(null);
        assertThrows(BadRequestException.class, () -> service.crear(sinBodega, 9));

        DespachoBodegaRequest sinReceptor = solicitud(linea(10, 1));
        sinReceptor.setRecibidoPor("  ");
        assertThrows(BadRequestException.class, () -> service.crear(sinReceptor, 9));

        DespachoBodegaRequest receptorLargo = solicitud(linea(10, 1));
        receptorLargo.setRecibidoPor("x".repeat(151));
        assertThrows(BadRequestException.class, () -> service.crear(receptorLargo, 9));

        DespachoBodegaRequest observacionesLargas = solicitud(linea(10, 1));
        observacionesLargas.setObservaciones("x".repeat(501));
        assertThrows(BadRequestException.class, () -> service.crear(observacionesLargas, 9));

        assertThrows(BadRequestException.class, () -> service.crear(solicitud(), 9));
        assertThrows(BadRequestException.class, () -> service.crear(null, 9));
    }

    @Test
    void rechazaLineasConProductoOCantidadInvalidos() {
        prepararEntorno();
        assertThrows(BadRequestException.class, () -> service.crear(solicitud(linea(10, 0)), 9));
        assertThrows(BadRequestException.class, () -> service.crear(solicitud(linea(10, -2)), 9));
        assertThrows(BadRequestException.class,
                () -> service.crear(solicitud(new DespachoBodegaRequest.Linea(null, 2)), 9));
        assertThrows(BadRequestException.class,
                () -> service.crear(solicitud(new DespachoBodegaRequest.Linea(10, null)), 9));
    }

    @Test
    void unProductoSinPrecioDeCompraSeValorizaEnCero() {
        prepararEntorno();
        Producto sinPrecio = Producto.builder().idProducto(12).nombre("Regalo").build();
        when(productoService.findById(12)).thenReturn(sinPrecio);
        when(inventarioBodegaService.obtenerStockParaActualizar(any(), any())).thenReturn(10);

        DespachoBodega despacho = service.crear(solicitud(linea(12, 3)), 9);

        assertEquals(new BigDecimal("0.00"), despacho.getTotal());
    }

    // ---------- aprobar ----------

    private DespachoBodega despachoPendiente() {
        DespachoBodega despacho = DespachoBodega.builder().idDespacho(77L).estado(EstadoDespachoBodegaEnum.PENDIENTE)
                .bodega(bodega).sucursalDestino(norte).usuarioDespacha(bodeguero).total(new BigDecimal("75.00"))
                .build();
        despacho.getItems().add(DespachoBodegaDetalle.builder().producto(cuaderno).cantidad(4).build());
        despacho.getItems().add(DespachoBodegaDetalle.builder().producto(lapiz).cantidad(20).build());
        return despacho;
    }

    @Test
    void aprobarIngresaLasExistenciasALaSucursalDestinoYMarcaElDespachoComoRealizado() {
        prepararEntorno();
        DespachoBodega despacho = despachoPendiente();
        when(despachoRepo.findParaResolver(77L)).thenReturn(Optional.of(despacho));

        DespachoBodega resultado = service.aprobar(77L, 1);

        assertEquals(EstadoDespachoBodegaEnum.REALIZADO, resultado.getEstado());
        assertEquals(admin, resultado.getUsuarioResuelve());
        assertNotNull(resultado.getFechaResolucion());

        ArgumentCaptor<MovimientoProducto> movimientos = ArgumentCaptor.forClass(MovimientoProducto.class);
        verify(movimientoProductoService, times(2)).save(movimientos.capture());
        for (MovimientoProducto movimiento : movimientos.getAllValues()) {
            assertEquals(TipoMovimientoEnum.ENTRADA, movimiento.getTipoMovimiento());
            assertEquals("DESPACHO_BODEGA", movimiento.getTipoDocumentoOrigen());
            assertEquals(77L, movimiento.getIdDocumentoOrigen());
            assertEquals(norte, movimiento.getSucursal());
            assertEquals(admin, movimiento.getUsuario());
        }
        assertEquals(4, movimientos.getAllValues().get(0).getCantidad());
        assertEquals(20, movimientos.getAllValues().get(1).getCantidad());
    }

    @Test
    void aprobarUnDespachoYaResueltoOInexistenteFalla() {
        prepararEntorno();
        DespachoBodega realizado = despachoPendiente();
        realizado.setEstado(EstadoDespachoBodegaEnum.REALIZADO);
        DespachoBodega cancelado = despachoPendiente();
        cancelado.setIdDespacho(78L);
        cancelado.setEstado(EstadoDespachoBodegaEnum.CANCELADO);
        when(despachoRepo.findParaResolver(77L)).thenReturn(Optional.of(realizado));
        when(despachoRepo.findParaResolver(78L)).thenReturn(Optional.of(cancelado));
        when(despachoRepo.findParaResolver(404L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> service.aprobar(77L, 1));
        assertThrows(BadRequestException.class, () -> service.aprobar(78L, 1));
        assertThrows(NotFoundException.class, () -> service.aprobar(404L, 1));

        verify(movimientoProductoService, never()).save(any(MovimientoProducto.class));
    }

    @Test
    void aprobarRechazaUnDestinoQueSeDesactivoDespuesDeRegistrarElDespacho() {
        prepararEntorno();
        DespachoBodega despacho = despachoPendiente();
        despacho.setSucursalDestino(Sucursal.builder().idSucursal(2).nombre("Norte")
                .estado(Estado.builder().idEstado(2).estado("INACTIVO").build()).build());
        when(despachoRepo.findParaResolver(77L)).thenReturn(Optional.of(despacho));

        assertThrows(BadRequestException.class, () -> service.aprobar(77L, 1));

        assertEquals(EstadoDespachoBodegaEnum.PENDIENTE, despacho.getEstado());
        verify(movimientoProductoService, never()).save(any(MovimientoProducto.class));
    }

    // ---------- cancelar ----------

    @Test
    void cancelarRegresaLasExistenciasReservadasALaBodegaYGuardaElMotivo() {
        prepararEntorno();
        DespachoBodega despacho = despachoPendiente();
        when(despachoRepo.findParaResolver(77L)).thenReturn(Optional.of(despacho));

        DespachoBodega resultado = service.cancelar(77L, "  Error en cantidades ", 9, false);

        assertEquals(EstadoDespachoBodegaEnum.CANCELADO, resultado.getEstado());
        assertEquals("Error en cantidades", resultado.getMotivoCancelacion());
        assertEquals(bodeguero, resultado.getUsuarioResuelve());
        assertNotNull(resultado.getFechaResolucion());

        verify(inventarioBodegaService).registrarMovimiento(eq(bodega), eq(cuaderno),
                eq(TipoMovimientoBodegaEnum.ANULACION_DESPACHO), eq(4), anyString(), eq(bodeguero),
                eq("DESPACHO_BODEGA"), eq(77L));
        verify(inventarioBodegaService).registrarMovimiento(eq(bodega), eq(lapiz),
                eq(TipoMovimientoBodegaEnum.ANULACION_DESPACHO), eq(20), anyString(), eq(bodeguero),
                eq("DESPACHO_BODEGA"), eq(77L));
        verify(movimientoProductoService, never()).save(any(MovimientoProducto.class));
    }

    @Test
    void cancelarExigeMotivo() {
        prepararEntorno();
        when(despachoRepo.findParaResolver(77L)).thenReturn(Optional.of(despachoPendiente()));

        assertThrows(BadRequestException.class, () -> service.cancelar(77L, " ", 9, true));
        assertThrows(BadRequestException.class, () -> service.cancelar(77L, null, 9, true));
        assertThrows(BadRequestException.class, () -> service.cancelar(77L, "x".repeat(301), 9, true));

        verify(inventarioBodegaService, never()).registrarMovimiento(
                any(), any(), any(), anyInt(), anyString(), any(), any(), anyLong());
    }

    @Test
    void unDespachoYaCanceladoNoSePuedeCancelarNiSiquieraUnAdministrador() {
        prepararEntorno();
        DespachoBodega cancelado = despachoPendiente();
        cancelado.setEstado(EstadoDespachoBodegaEnum.CANCELADO);
        when(despachoRepo.findParaResolver(77L)).thenReturn(Optional.of(cancelado));

        assertThrows(BadRequestException.class, () -> service.cancelar(77L, "Motivo", 9, true));
        assertThrows(BadRequestException.class, () -> service.cancelar(77L, "Motivo", 9, false));

        verify(movimientoProductoService, never()).save(any(MovimientoProducto.class));
    }

    @Test
    void soloUnAdministradorPuedeRevertirUnDespachoAprobado() {
        prepararEntorno();
        DespachoBodega realizado = despachoPendiente();
        realizado.setEstado(EstadoDespachoBodegaEnum.REALIZADO);
        when(despachoRepo.findParaResolver(77L)).thenReturn(Optional.of(realizado));

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.cancelar(77L, "Motivo válido", 9, false));

        assertTrue(error.getMessage().contains("administrador"));
        assertEquals(EstadoDespachoBodegaEnum.REALIZADO, realizado.getEstado());
        verify(movimientoProductoService, never()).save(any(MovimientoProducto.class));
        verify(inventarioBodegaService, never()).registrarMovimiento(
                any(), any(), any(), anyInt(), anyString(), any(), any(), anyLong());
    }

    @Test
    void revertirUnDespachoAprobadoRetiraLasUnidadesDeLaSucursalYLasRegresaALaBodega() {
        prepararEntorno();
        DespachoBodega realizado = despachoPendiente();
        realizado.setEstado(EstadoDespachoBodegaEnum.REALIZADO);
        realizado.setUsuarioResuelve(bodeguero);
        when(despachoRepo.findParaResolver(77L)).thenReturn(Optional.of(realizado));

        DespachoBodega resultado = service.cancelar(77L, "  Enviado por error ", 1, true);

        assertEquals(EstadoDespachoBodegaEnum.CANCELADO, resultado.getEstado());
        assertEquals("Enviado por error", resultado.getMotivoCancelacion());
        assertEquals(admin, resultado.getUsuarioResuelve());

        ArgumentCaptor<MovimientoProducto> salidas = ArgumentCaptor.forClass(MovimientoProducto.class);
        verify(movimientoProductoService, times(2)).save(salidas.capture());
        for (MovimientoProducto salida : salidas.getAllValues()) {
            assertEquals(TipoMovimientoEnum.SALIDA, salida.getTipoMovimiento());
            assertEquals("DESPACHO_BODEGA", salida.getTipoDocumentoOrigen());
            assertEquals(77L, salida.getIdDocumentoOrigen());
            assertEquals(norte, salida.getSucursal());
            assertEquals(admin, salida.getUsuario());
        }
        verify(inventarioBodegaService).registrarMovimiento(eq(bodega), eq(cuaderno),
                eq(TipoMovimientoBodegaEnum.ANULACION_DESPACHO), eq(4), eq("Reversión del despacho #77"), eq(admin),
                eq("DESPACHO_BODEGA"), eq(77L));
        verify(inventarioBodegaService).registrarMovimiento(eq(bodega), eq(lapiz),
                eq(TipoMovimientoBodegaEnum.ANULACION_DESPACHO), eq(20), eq("Reversión del despacho #77"), eq(admin),
                eq("DESPACHO_BODEGA"), eq(77L));
    }

    @Test
    void siLaSucursalYaNoTieneLasUnidadesLaReversionSeRechazaSinTocarLaBodega() {
        prepararEntorno();
        DespachoBodega realizado = despachoPendiente();
        realizado.setEstado(EstadoDespachoBodegaEnum.REALIZADO);
        when(despachoRepo.findParaResolver(77L)).thenReturn(Optional.of(realizado));
        when(movimientoProductoService.save(any(MovimientoProducto.class)))
                .thenThrow(new BadRequestException("Stock insuficiente para Cuaderno. Disponible: 1, solicitado: 4.", null));

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.cancelar(77L, "Motivo", 1, true));

        assertTrue(error.getMessage().contains("No se puede revertir el despacho #77"));
        assertTrue(error.getMessage().contains("Norte"));
        assertEquals(EstadoDespachoBodegaEnum.REALIZADO, realizado.getEstado());
        verify(inventarioBodegaService, never()).registrarMovimiento(
                any(), any(), any(), anyInt(), anyString(), any(), any(), anyLong());
    }

    @Test
    void cancelarUnDespachoPendienteNoToscaElInventarioDeLaSucursalAunSiEsAdministrador() {
        prepararEntorno();
        when(despachoRepo.findParaResolver(77L)).thenReturn(Optional.of(despachoPendiente()));

        service.cancelar(77L, "Motivo", 1, true);

        verify(movimientoProductoService, never()).save(any(MovimientoProducto.class));
    }

    // ---------- consultas ----------

    @Test
    void elListadoRechazaUnEstadoDesconocidoYAceptaMinusculas() {
        assertThrows(BadRequestException.class,
                () -> service.findListado("", null, "EN_CAMINO", PageRequest.of(0, 5)));

        service.findListado("  texto ", 1, "pendiente", PageRequest.of(0, 5));

        verify(despachoRepo).findListado(eq("texto"), eq(1), eq(EstadoDespachoBodegaEnum.PENDIENTE), any());
    }

    @Test
    void elListadoSinEstadoNoFiltraPorEstado() {
        service.findListado(null, null, "", PageRequest.of(0, 5));

        verify(despachoRepo).findListado(eq(""), eq(null), eq(null), any());
    }

    @Test
    void findByIdInexistenteLanzaNotFound() {
        when(despachoRepo.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.findById(404L));
    }

    @Test
    void findByIdDevuelveElDespachoConSuDetalle() {
        DespachoBodega despacho = despachoPendiente();
        when(despachoRepo.findById(77L)).thenReturn(Optional.of(despacho));

        assertEquals(2, service.findById(77L).getItems().size());
    }
}
