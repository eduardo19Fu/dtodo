package xyz.pangosoft.dtodo.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;

import xyz.pangosoft.dtodo.dto.DevolucionBodegaRequest;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Bodega;
import xyz.pangosoft.dtodo.model.DevolucionBodega;
import xyz.pangosoft.dtodo.model.DevolucionBodegaDetalle;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.InventarioSucursal;
import xyz.pangosoft.dtodo.model.MovimientoBodega;
import xyz.pangosoft.dtodo.model.MovimientoProducto;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.EstadoDespachoBodegaEnum;
import xyz.pangosoft.dtodo.model.enums.TipoMovimientoBodegaEnum;
import xyz.pangosoft.dtodo.model.enums.TipoMovimientoEnum;
import xyz.pangosoft.dtodo.repository.IDevolucionBodegaRepository;
import xyz.pangosoft.dtodo.service.IBodegaService;
import xyz.pangosoft.dtodo.service.IInventarioBodegaService;
import xyz.pangosoft.dtodo.service.IInventarioSucursalService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DevolucionBodegaServiceImplTest {

    private final IDevolucionBodegaRepository devolucionRepo = mock(IDevolucionBodegaRepository.class);
    private final IBodegaService bodegaService = mock(IBodegaService.class);
    private final IInventarioBodegaService inventarioBodegaService = mock(IInventarioBodegaService.class);
    private final IInventarioSucursalService inventarioSucursalService = mock(IInventarioSucursalService.class);
    private final IProductoService productoService = mock(IProductoService.class);
    private final ISucursalService sucursalService = mock(ISucursalService.class);
    private final IUsuarioService usuarioService = mock(IUsuarioService.class);
    private final IMovimientoProductoService movimientoProductoService = mock(IMovimientoProductoService.class);

    private final DevolucionBodegaServiceImpl service = new DevolucionBodegaServiceImpl(
            devolucionRepo, bodegaService, inventarioBodegaService, inventarioSucursalService, productoService,
            sucursalService, usuarioService, movimientoProductoService);

    private final Estado activo = Estado.builder().idEstado(1).estado("ACTIVO").build();
    private final Estado inactivo = Estado.builder().idEstado(2).estado("INACTIVO").build();
    private final Sucursal norte = Sucursal.builder().idSucursal(2).nombre("Norte").estado(activo).build();
    private final Bodega bodega = Bodega.builder().idBodega(1).nombre("Principal").estado(activo).build();
    private final Usuario bodeguero = Usuario.builder().idUsuario(9).usuario("bodeguero").build();
    private final Usuario admin = Usuario.builder().idUsuario(1).usuario("admin").build();
    private final Producto cuaderno = Producto.builder().idProducto(10).codProducto("C-10").nombre("Cuaderno")
            .precioCompra(new BigDecimal("12.50")).build();
    private final Producto lapiz = Producto.builder().idProducto(11).codProducto("L-11").nombre("Lápiz")
            .precioCompra(new BigDecimal("1.25")).build();

    private void prepararEntorno() {
        when(sucursalService.findById(2)).thenReturn(norte);
        when(bodegaService.findActivaById(1)).thenReturn(bodega);
        when(usuarioService.findById(9)).thenReturn(bodeguero);
        when(usuarioService.findById(1)).thenReturn(admin);
        when(productoService.findById(10)).thenReturn(cuaderno);
        when(productoService.findById(11)).thenReturn(lapiz);
        when(devolucionRepo.save(any(DevolucionBodega.class))).thenAnswer(invocation -> {
            DevolucionBodega devolucion = invocation.getArgument(0);
            if (devolucion.getIdDevolucion() == null) {
                devolucion.setIdDevolucion(55L);
            }
            return devolucion;
        });
        when(inventarioBodegaService.registrarMovimiento(any(), any(), any(), anyInt(), anyString(), any(), any(), any()))
                .thenReturn(new MovimientoBodega());
    }

    private void existenciaEnSucursal(Producto producto, int stock) {
        when(inventarioSucursalService.obtenerParaActualizar(norte, producto))
                .thenReturn(InventarioSucursal.builder().sucursal(norte).producto(producto).stock(stock).build());
    }

    private DevolucionBodegaRequest solicitud(DevolucionBodegaRequest.Linea... lineas) {
        DevolucionBodegaRequest request = new DevolucionBodegaRequest();
        request.setIdSucursalOrigen(2);
        request.setIdBodega(1);
        request.setEntregadoPor("  Maria López ");
        request.setMotivo(" Producto con sobrestock ");
        request.setItems(new ArrayList<>(List.of(lineas)));
        return request;
    }

    private DevolucionBodegaRequest.Linea linea(int idProducto, int cantidad) {
        return new DevolucionBodegaRequest.Linea(idProducto, cantidad);
    }

    private DevolucionBodega devolucionPendiente() {
        DevolucionBodega devolucion = DevolucionBodega.builder()
                .idDevolucion(55L)
                .estado(EstadoDespachoBodegaEnum.PENDIENTE)
                .sucursalOrigen(norte)
                .bodega(bodega)
                .usuarioRegistra(bodeguero)
                .build();
        devolucion.getItems().add(DevolucionBodegaDetalle.builder().producto(cuaderno).cantidad(4).build());
        devolucion.getItems().add(DevolucionBodegaDetalle.builder().producto(lapiz).cantidad(20).build());
        return devolucion;
    }

    // ---------- crear ----------

    @Test
    void creaUnaDevolucionPendienteConTotalesYReservaElStockDeLaSucursal() {
        prepararEntorno();
        existenciaEnSucursal(cuaderno, 50);
        existenciaEnSucursal(lapiz, 200);

        DevolucionBodega devolucion = service.crear(solicitud(linea(10, 4), linea(11, 20)), 9);

        assertEquals(EstadoDespachoBodegaEnum.PENDIENTE, devolucion.getEstado());
        assertEquals("Maria López", devolucion.getEntregadoPor());
        assertEquals("Producto con sobrestock", devolucion.getMotivo());
        assertEquals(bodeguero, devolucion.getUsuarioRegistra());
        assertEquals(norte, devolucion.getSucursalOrigen());
        assertEquals(bodega, devolucion.getBodega());
        assertNull(devolucion.getUsuarioResuelve());
        assertNull(devolucion.getFechaResolucion());
        // 4 x 12.50 + 20 x 1.25 = 50.00 + 25.00
        assertEquals(new BigDecimal("75.00"), devolucion.getTotal());

        DevolucionBodegaDetalle primero = devolucion.getItems().get(0);
        assertEquals(cuaderno, primero.getProducto());
        assertEquals(new BigDecimal("12.50"), primero.getPrecioUnitario());
        assertEquals(new BigDecimal("50.00"), primero.getSubTotal());
        assertEquals(50, primero.getExistenciaSucursal());

        ArgumentCaptor<MovimientoProducto> captor = ArgumentCaptor.forClass(MovimientoProducto.class);
        verify(movimientoProductoService, times(2)).save(captor.capture());
        for (MovimientoProducto movimiento : captor.getAllValues()) {
            assertEquals(TipoMovimientoEnum.SALIDA, movimiento.getTipoMovimiento());
            assertEquals("DEVOLUCION_BODEGA", movimiento.getTipoDocumentoOrigen());
            assertEquals(55L, movimiento.getIdDocumentoOrigen());
            assertEquals(norte, movimiento.getSucursal());
            assertEquals(bodeguero, movimiento.getUsuario());
        }
        assertEquals(4, captor.getAllValues().get(0).getCantidad());
        assertEquals(20, captor.getAllValues().get(1).getCantidad());
        // La bodega no recibe nada hasta que un administrador aprueba
        verify(inventarioBodegaService, never()).registrarMovimiento(any(), any(), any(), anyInt(), anyString(), any(),
                any(), any());
    }

    @Test
    void sumaLasLineasRepetidasDeUnMismoProducto() {
        prepararEntorno();
        existenciaEnSucursal(cuaderno, 100);

        DevolucionBodega devolucion = service.crear(solicitud(linea(10, 3), linea(10, 2)), 9);

        assertEquals(1, devolucion.getItems().size());
        assertEquals(5, devolucion.getItems().get(0).getCantidad());
        assertEquals(new BigDecimal("62.50"), devolucion.getTotal());
    }

    @Test
    void rechazaDevolverMasDeLoQueTieneLaSucursal() {
        prepararEntorno();
        existenciaEnSucursal(cuaderno, 3);

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.crear(solicitud(linea(10, 4)), 9));

        assertTrue(error.getMessage().contains("Existencias insuficientes en la sucursal para Cuaderno"));
        verify(devolucionRepo, never()).save(any());
        verify(movimientoProductoService, never()).save(any());
    }

    @Test
    void rechazaUnaSucursalInactiva() {
        prepararEntorno();
        norte.setEstado(inactivo);

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.crear(solicitud(linea(10, 1)), 9));

        assertTrue(error.getMessage().contains("inactiva"));
        verify(devolucionRepo, never()).save(any());
    }

    @Test
    void validaLosDatosObligatoriosDeLaSolicitud() {
        prepararEntorno();

        DevolucionBodegaRequest sinSucursal = solicitud(linea(10, 1));
        sinSucursal.setIdSucursalOrigen(null);
        DevolucionBodegaRequest sinBodega = solicitud(linea(10, 1));
        sinBodega.setIdBodega(null);
        DevolucionBodegaRequest sinEntregador = solicitud(linea(10, 1));
        sinEntregador.setEntregadoPor("  ");
        DevolucionBodegaRequest sinMotivo = solicitud(linea(10, 1));
        sinMotivo.setMotivo(null);
        DevolucionBodegaRequest motivoLargo = solicitud(linea(10, 1));
        motivoLargo.setMotivo("x".repeat(301));
        DevolucionBodegaRequest sinProductos = solicitud();
        DevolucionBodegaRequest cantidadCero = solicitud(linea(10, 0));

        for (DevolucionBodegaRequest invalida : List.of(sinSucursal, sinBodega, sinEntregador, sinMotivo, motivoLargo,
                sinProductos, cantidadCero)) {
            assertThrows(BadRequestException.class, () -> service.crear(invalida, 9));
        }
        verify(devolucionRepo, never()).save(any());
        verify(movimientoProductoService, never()).save(any());
    }

    // ---------- aprobar ----------

    @Test
    void aprobarIngresaLasUnidadesALaBodegaYMarcaLaDevolucionRealizada() {
        prepararEntorno();
        when(devolucionRepo.findParaResolver(55L)).thenReturn(Optional.of(devolucionPendiente()));

        DevolucionBodega aprobada = service.aprobar(55L, 1);

        assertEquals(EstadoDespachoBodegaEnum.REALIZADO, aprobada.getEstado());
        assertEquals(admin, aprobada.getUsuarioResuelve());
        assertNotNull(aprobada.getFechaResolucion());
        verify(inventarioBodegaService).registrarMovimiento(eq(bodega), eq(cuaderno),
                eq(TipoMovimientoBodegaEnum.INGRESO_DEVOLUCION), eq(4), anyString(), eq(admin),
                eq("DEVOLUCION_BODEGA"), eq(55L));
        verify(inventarioBodegaService).registrarMovimiento(eq(bodega), eq(lapiz),
                eq(TipoMovimientoBodegaEnum.INGRESO_DEVOLUCION), eq(20), anyString(), eq(admin),
                eq("DEVOLUCION_BODEGA"), eq(55L));
        // La sucursal ya había descontado el stock al registrar la devolución
        verify(movimientoProductoService, never()).save(any());
    }

    @Test
    void aprobarFallaSiLaBodegaYaNoEstaActiva() {
        prepararEntorno();
        when(devolucionRepo.findParaResolver(55L)).thenReturn(Optional.of(devolucionPendiente()));
        when(bodegaService.findActivaById(1)).thenThrow(new BadRequestException("La bodega está inactiva", null));

        assertThrows(BadRequestException.class, () -> service.aprobar(55L, 1));

        verify(inventarioBodegaService, never()).registrarMovimiento(any(), any(), any(), anyInt(), anyString(), any(),
                any(), any());
        verify(devolucionRepo, never()).save(any());
    }

    @Test
    void noSePuedeAprobarUnaDevolucionYaResuelta() {
        prepararEntorno();
        DevolucionBodega aprobada = devolucionPendiente();
        aprobada.setEstado(EstadoDespachoBodegaEnum.REALIZADO);
        DevolucionBodega rechazada = devolucionPendiente();
        rechazada.setEstado(EstadoDespachoBodegaEnum.CANCELADO);
        when(devolucionRepo.findParaResolver(55L)).thenReturn(Optional.of(aprobada));
        when(devolucionRepo.findParaResolver(56L)).thenReturn(Optional.of(rechazada));

        BadRequestException yaAprobada = assertThrows(BadRequestException.class, () -> service.aprobar(55L, 1));
        BadRequestException yaRechazada = assertThrows(BadRequestException.class, () -> service.aprobar(56L, 1));

        assertTrue(yaAprobada.getMessage().contains("aprobada"));
        assertTrue(yaRechazada.getMessage().contains("rechazada"));
        verify(inventarioBodegaService, never()).registrarMovimiento(any(), any(), any(), anyInt(), anyString(), any(),
                any(), any());
    }

    @Test
    void aprobarUnaDevolucionInexistenteLanzaNotFound() {
        when(devolucionRepo.findParaResolver(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.aprobar(99L, 1));
    }

    // ---------- cancelar ----------

    @Test
    void cancelarRegresaLasUnidadesALaSucursalYGuardaElMotivo() {
        prepararEntorno();
        when(devolucionRepo.findParaResolver(55L)).thenReturn(Optional.of(devolucionPendiente()));

        DevolucionBodega rechazada = service.cancelar(55L, "  Mercadería en mal estado ", 1);

        assertEquals(EstadoDespachoBodegaEnum.CANCELADO, rechazada.getEstado());
        assertEquals("Mercadería en mal estado", rechazada.getMotivoCancelacion());
        assertEquals(admin, rechazada.getUsuarioResuelve());
        assertNotNull(rechazada.getFechaResolucion());

        ArgumentCaptor<MovimientoProducto> captor = ArgumentCaptor.forClass(MovimientoProducto.class);
        verify(movimientoProductoService, times(2)).save(captor.capture());
        for (MovimientoProducto movimiento : captor.getAllValues()) {
            assertEquals(TipoMovimientoEnum.ENTRADA, movimiento.getTipoMovimiento());
            assertEquals("DEVOLUCION_BODEGA", movimiento.getTipoDocumentoOrigen());
            assertEquals(norte, movimiento.getSucursal());
        }
        verify(inventarioBodegaService, never()).registrarMovimiento(any(), any(), any(), anyInt(), anyString(), any(),
                any(), any());
    }

    @Test
    void cancelarExigeUnMotivoNoVacioYNoMayorA300Caracteres() {
        prepararEntorno();
        when(devolucionRepo.findParaResolver(55L)).thenReturn(Optional.of(devolucionPendiente()));

        assertThrows(BadRequestException.class, () -> service.cancelar(55L, null, 1));
        assertThrows(BadRequestException.class, () -> service.cancelar(55L, "   ", 1));
        assertThrows(BadRequestException.class, () -> service.cancelar(55L, "x".repeat(301), 1));

        verify(movimientoProductoService, never()).save(any());
    }

    @Test
    void noSePuedeCancelarUnaDevolucionYaAprobada() {
        prepararEntorno();
        DevolucionBodega aprobada = devolucionPendiente();
        aprobada.setEstado(EstadoDespachoBodegaEnum.REALIZADO);
        when(devolucionRepo.findParaResolver(55L)).thenReturn(Optional.of(aprobada));

        assertThrows(BadRequestException.class, () -> service.cancelar(55L, "Error de captura", 1));

        verify(movimientoProductoService, never()).save(any());
        verify(devolucionRepo, never()).save(any());
    }

    // ---------- consultas ----------

    @Test
    void elFiltroDeEstadoInvalidoSeRechaza() {
        assertThrows(BadRequestException.class,
                () -> service.findListado("", null, "NO_EXISTE", PageRequest.of(0, 5)));
    }

    @Test
    void findByIdInexistenteLanzaNotFound() {
        when(devolucionRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.findById(99L));
    }
}
