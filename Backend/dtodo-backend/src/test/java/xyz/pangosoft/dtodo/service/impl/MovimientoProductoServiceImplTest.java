package xyz.pangosoft.dtodo.service.impl;

import java.time.LocalDateTime;
import java.util.Arrays;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.model.InventarioSucursal;
import xyz.pangosoft.dtodo.model.DetalleFactura;
import xyz.pangosoft.dtodo.model.MovimientoProducto;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.enums.TipoMovimientoEnum;
import xyz.pangosoft.dtodo.repository.IMovimientoProductoRepository;
import xyz.pangosoft.dtodo.service.IEstadoService;
import xyz.pangosoft.dtodo.service.IInventarioSucursalService;
import xyz.pangosoft.dtodo.service.IProductoService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class MovimientoProductoServiceImplTest {

    private final IMovimientoProductoRepository repository = mock(IMovimientoProductoRepository.class);
    private final IInventarioSucursalService inventarioSucursalService = mock(IInventarioSucursalService.class);
    private final MovimientoProductoServiceImpl service = new MovimientoProductoServiceImpl(
            repository,
            mock(IEstadoService.class),
            mock(IProductoService.class),
            inventarioSucursalService,
            mock(DataSource.class)
    );

    @Test
    void incluyeCompletoElDiaFinalDelRango() {
        PageRequest pageable = PageRequest.of(0, 5);
        when(repository.findListado(any(), any(), any(), any(), any())).thenReturn(Page.empty());

        service.findListado("2026-08-01", "2026-08-05", 1, "", pageable);

        verify(repository).findListado(
                eq(LocalDateTime.of(2026, 8, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 8, 6, 0, 0)),
                eq(1),
                eq(""),
                eq(pageable)
        );
    }

    @Test
    void limitaLaConsultaInicialALosUltimosQuinientosMovimientosDeLaSucursal() {
        PageRequest pageable = PageRequest.of(0, 5);
        when(repository.findUltimosIds(any(), any())).thenReturn(Arrays.asList(10L, 9L, 8L));
        when(repository.findListadoLimitado(any(), any(), any())).thenReturn(Page.empty());

        service.findListado(null, null, 1, "", pageable);

        verify(repository).findUltimosIds(1, PageRequest.of(0, 500));
        verify(repository).findListadoLimitado(Arrays.asList(10L, 9L, 8L), "", pageable);
    }

    @Test
    void rechazaUnRangoInvertido() {
        assertThrows(BadRequestException.class,
                () -> service.findListado("2026-08-05", "2026-08-01", 1, "", PageRequest.of(0, 5)));

        verifyNoInteractions(repository);
    }

    @Test
    void unaVentaDescuentaElStockDeLaSucursalDelMovimiento() {
        Sucursal sucursal = Sucursal.builder().idSucursal(1).build();
        Producto producto = Producto.builder().idProducto(10).build();
        InventarioSucursal inventario = InventarioSucursal.builder()
                .sucursal(sucursal).producto(producto).stock(50).build();
        when(inventarioSucursalService.obtenerParaActualizar(sucursal, producto)).thenReturn(inventario);
        when(inventarioSucursalService.guardar(any(InventarioSucursal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MovimientoProducto movimiento = MovimientoProducto.builder()
                .sucursal(sucursal).producto(producto)
                .tipoMovimiento(TipoMovimientoEnum.VENTA).cantidad(5)
                .build();

        boolean resultado = service.calcularStock(movimiento);

        assertEquals(true, resultado);
        assertEquals(50, movimiento.getStockInicial());
        assertEquals(45, inventario.getStock());
    }

    @Test
    void unaCompraAumentaElStockDeLaSucursalDelMovimiento() {
        Sucursal sucursal = Sucursal.builder().idSucursal(2).build();
        Producto producto = Producto.builder().idProducto(11).build();
        InventarioSucursal inventario = InventarioSucursal.builder()
                .sucursal(sucursal).producto(producto).stock(10).build();
        when(inventarioSucursalService.obtenerParaActualizar(sucursal, producto)).thenReturn(inventario);
        when(inventarioSucursalService.guardar(any(InventarioSucursal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MovimientoProducto movimiento = MovimientoProducto.builder()
                .sucursal(sucursal).producto(producto)
                .tipoMovimiento(TipoMovimientoEnum.COMPRA).cantidad(20)
                .build();

        boolean resultado = service.calcularStock(movimiento);

        assertEquals(true, resultado);
        assertEquals(30, inventario.getStock());
    }

    @Test
    void rechazaUnaSalidaQueDejariaElStockNegativo() {
        Sucursal sucursal = Sucursal.builder().idSucursal(1).build();
        Producto producto = Producto.builder().idProducto(12).nombre("Producto sin existencias").build();
        InventarioSucursal inventario = InventarioSucursal.builder()
                .sucursal(sucursal).producto(producto).stock(2).build();
        when(inventarioSucursalService.obtenerParaActualizar(sucursal, producto)).thenReturn(inventario);

        MovimientoProducto movimiento = MovimientoProducto.builder()
                .sucursal(sucursal).producto(producto)
                .tipoMovimiento(TipoMovimientoEnum.VENTA).cantidad(3)
                .build();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.calcularStock(movimiento));

        assertEquals(2, inventario.getStock());
        assertTrue(exception.getMessage().contains("Stock insuficiente"));
        verify(inventarioSucursalService, never()).guardar(any(InventarioSucursal.class));
    }

    @Test
    void validaLaSumaDeLineasRepetidasAntesDeFacturar() {
        Sucursal sucursal = Sucursal.builder().idSucursal(1).build();
        Producto producto = Producto.builder().idProducto(13).nombre("Producto repetido").build();
        InventarioSucursal inventario = InventarioSucursal.builder()
                .sucursal(sucursal).producto(producto).stock(5).build();
        when(inventarioSucursalService.obtenerParaActualizar(sucursal, producto)).thenReturn(inventario);
        DetalleFactura primera = DetalleFactura.builder().producto(producto).cantidad(3).build();
        DetalleFactura segunda = DetalleFactura.builder().producto(producto).cantidad(3).build();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.validarStockDisponible(Arrays.asList(primera, segunda), sucursal));

        assertTrue(exception.getMessage().contains("solicitado: 6"));
    }
}
