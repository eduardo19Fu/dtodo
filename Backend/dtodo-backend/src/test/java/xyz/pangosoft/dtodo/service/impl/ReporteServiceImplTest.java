package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.service.IBajoStockReporteService;
import xyz.pangosoft.dtodo.service.IComprasPeriodoReporteService;
import xyz.pangosoft.dtodo.service.IFacturaService;
import xyz.pangosoft.dtodo.service.IInventarioMovimientosReporteService;
import xyz.pangosoft.dtodo.service.IProductoService;
import xyz.pangosoft.dtodo.service.IProformaService;
import xyz.pangosoft.dtodo.service.IRentabilidadProductoReporteService;
import xyz.pangosoft.dtodo.service.IResumenNotasCreditoReporteService;
import xyz.pangosoft.dtodo.service.IVentasClienteReporteService;
import xyz.pangosoft.dtodo.service.IVentasProductoReporteService;

class ReporteServiceImplTest {

    private IFacturaService facturaService;
    private IInventarioMovimientosReporteService inventarioMovimientosReporteService;
    private IProductoService productoService;
    private IProformaService proformaService;
    private IResumenNotasCreditoReporteService resumenNotasCreditoReporteService;
    private IComprasPeriodoReporteService comprasPeriodoReporteService;
    private IVentasProductoReporteService ventasProductoReporteService;
    private IVentasClienteReporteService ventasClienteReporteService;
    private IRentabilidadProductoReporteService rentabilidadProductoReporteService;
    private IBajoStockReporteService bajoStockReporteService;
    private ReporteServiceImpl service;

    @BeforeEach
    void setUp() {
        facturaService = mock(IFacturaService.class);
        inventarioMovimientosReporteService = mock(IInventarioMovimientosReporteService.class);
        productoService = mock(IProductoService.class);
        proformaService = mock(IProformaService.class);
        resumenNotasCreditoReporteService = mock(IResumenNotasCreditoReporteService.class);
        comprasPeriodoReporteService = mock(IComprasPeriodoReporteService.class);
        ventasProductoReporteService = mock(IVentasProductoReporteService.class);
        ventasClienteReporteService = mock(IVentasClienteReporteService.class);
        rentabilidadProductoReporteService = mock(IRentabilidadProductoReporteService.class);
        bajoStockReporteService = mock(IBajoStockReporteService.class);
        service = new ReporteServiceImpl(
                facturaService, inventarioMovimientosReporteService, productoService, proformaService,
                resumenNotasCreditoReporteService, comprasPeriodoReporteService, ventasProductoReporteService,
                ventasClienteReporteService, rentabilidadProductoReporteService, bajoStockReporteService);
    }

    @Test
    void polizaIndividualDelegaConFiltrosValidados() {
        byte[] esperado = new byte[] { 1, 2, 3 };
        when(facturaService.resportDailySales(2, 7, "2026-09-01", "2026-09-19"))
                .thenReturn(esperado);

        byte[] resultado = service.generarPolizaIndividual(
                2, 7, "2026-09-01", "2026-09-19");

        assertArrayEquals(esperado, resultado);
        verify(facturaService).resportDailySales(2, 7, "2026-09-01", "2026-09-19");
    }

    @Test
    void rechazaRangoInvertidoAntesDeConsultarServicios() {
        assertThrows(BadRequestException.class, () -> service.generarPolizaGeneral(
                1, "2026-09-20", "2026-09-19"));

        verifyNoInteractions(facturaService, inventarioMovimientosReporteService, productoService, proformaService,
                resumenNotasCreditoReporteService, comprasPeriodoReporteService, ventasProductoReporteService,
                ventasClienteReporteService, rentabilidadProductoReporteService, bajoStockReporteService);
    }

    @Test
    void rechazaFormatoDeFechaInvalido() {
        assertThrows(BadRequestException.class, () -> service.generarPolizaGeneral(
                1, "19/09/2026", "20/09/2026"));
    }

    @Test
    void existenciasSiempreUsaLaSucursalSolicitada() {
        byte[] esperado = new byte[] { 4, 5 };
        when(productoService.productosExcel(3)).thenReturn(esperado);

        byte[] resultado = service.generarExistencias(3);

        assertArrayEquals(esperado, resultado);
        verify(productoService).productosExcel(3);
    }

    @Test
    void movimientosInventarioDelegaFormatoYFechasValidadas() {
        byte[] esperado = new byte[] { 6, 7 };
        when(inventarioMovimientosReporteService.generar(
                3, java.time.LocalDate.of(2026, 9, 1), java.time.LocalDate.of(2026, 9, 19), "XLSX"))
                .thenReturn(esperado);

        byte[] resultado = service.generarMovimientosInventario(
                3, "2026-09-01", "2026-09-19", "xlsx");

        assertArrayEquals(esperado, resultado);
        verify(inventarioMovimientosReporteService).generar(
                3, java.time.LocalDate.of(2026, 9, 1), java.time.LocalDate.of(2026, 9, 19), "XLSX");
    }

    @Test
    void ventasProductoDelegaCategoriaYFormatoValidados() {
        byte[] esperado = new byte[] { 12, 13 };
        when(ventasProductoReporteService.generar(
                2, java.time.LocalDate.of(2026, 9, 1), java.time.LocalDate.of(2026, 9, 19), 3, "XLSX"))
                .thenReturn(esperado);

        byte[] resultado = service.generarVentasProducto(
                2, "2026-09-01", "2026-09-19", 3, "xlsx");

        assertArrayEquals(esperado, resultado);
        verify(ventasProductoReporteService).generar(
                2, java.time.LocalDate.of(2026, 9, 1), java.time.LocalDate.of(2026, 9, 19), 3, "XLSX");
    }

    @Test
    void ventasProductoRechazaCategoriaInvalida() {
        assertThrows(BadRequestException.class, () -> service.generarVentasProducto(
                1, "2026-09-01", "2026-09-19", 0, "PDF"));

        verifyNoInteractions(ventasProductoReporteService);
    }

    @Test
    void ventasClienteDelegaFiltroValidado() {
        byte[] esperado = new byte[] { 14, 15 };
        when(ventasClienteReporteService.generar(
                2, java.time.LocalDate.of(2026, 9, 1), java.time.LocalDate.of(2026, 9, 19), 8, "XLSX"))
                .thenReturn(esperado);

        byte[] resultado = service.generarVentasCliente(
                2, "2026-09-01", "2026-09-19", 8, "xlsx");

        assertArrayEquals(esperado, resultado);
        verify(ventasClienteReporteService).generar(
                2, java.time.LocalDate.of(2026, 9, 1), java.time.LocalDate.of(2026, 9, 19), 8, "XLSX");
    }

    @Test
    void ventasClienteRechazaClienteInvalido() {
        assertThrows(BadRequestException.class, () -> service.generarVentasCliente(
                1, "2026-09-01", "2026-09-19", 0, "PDF"));

        verifyNoInteractions(ventasClienteReporteService);
    }

    @Test
    void ventasClienteRechazaFormatoInvalido() {
        assertThrows(BadRequestException.class, () -> service.generarVentasCliente(
                1, "2026-09-01", "2026-09-19", null, "CSV"));

        verifyNoInteractions(ventasClienteReporteService);
    }

    @Test
    void rentabilidadProductoDelegaCategoriaYFormatoValidados() {
        byte[] esperado = new byte[] { 16, 17 };
        when(rentabilidadProductoReporteService.generar(
                2, java.time.LocalDate.of(2026, 9, 1), java.time.LocalDate.of(2026, 9, 19), 3, "PDF"))
                .thenReturn(esperado);

        byte[] resultado = service.generarRentabilidadProducto(
                2, "2026-09-01", "2026-09-19", 3, "pdf");

        assertArrayEquals(esperado, resultado);
        verify(rentabilidadProductoReporteService).generar(
                2, java.time.LocalDate.of(2026, 9, 1), java.time.LocalDate.of(2026, 9, 19), 3, "PDF");
    }

    @Test
    void rentabilidadProductoRechazaCategoriaInvalida() {
        assertThrows(BadRequestException.class, () -> service.generarRentabilidadProducto(
                1, "2026-09-01", "2026-09-19", 0, "PDF"));

        verifyNoInteractions(rentabilidadProductoReporteService);
    }

    @Test
    void bajoStockDelegaCategoriaYFormatoValidados() {
        byte[] esperado = new byte[] { 18, 19 };
        when(bajoStockReporteService.generar(2, 3, "XLSX")).thenReturn(esperado);

        byte[] resultado = service.generarBajoStock(2, 3, "xlsx");

        assertArrayEquals(esperado, resultado);
        verify(bajoStockReporteService).generar(2, 3, "XLSX");
    }

    @Test
    void bajoStockRechazaCategoriaInvalida() {
        assertThrows(BadRequestException.class, () -> service.generarBajoStock(1, 0, "PDF"));

        verifyNoInteractions(bajoStockReporteService);
    }

    @Test
    void resumenNotasCreditoDelegaFiltrosValidados() {
        byte[] esperado = new byte[] { 8, 9 };
        when(resumenNotasCreditoReporteService.generar(
                2, java.time.LocalDate.of(2026, 9, 1), java.time.LocalDate.of(2026, 9, 19),
                xyz.pangosoft.dtodo.model.enums.EstadoNotaCreditoEnum.ENTREGADO, "XLSX"))
                .thenReturn(esperado);

        byte[] resultado = service.generarResumenNotasCredito(
                2, "2026-09-01", "2026-09-19", "entregado", "xlsx");

        assertArrayEquals(esperado, resultado);
        verify(resumenNotasCreditoReporteService).generar(
                2, java.time.LocalDate.of(2026, 9, 1), java.time.LocalDate.of(2026, 9, 19),
                xyz.pangosoft.dtodo.model.enums.EstadoNotaCreditoEnum.ENTREGADO, "XLSX");
    }

    @Test
    void resumenNotasCreditoRechazaEstadoInvalido() {
        assertThrows(BadRequestException.class, () -> service.generarResumenNotasCredito(
                1, "2026-09-01", "2026-09-19", "DESCONOCIDO", "PDF"));

        verifyNoInteractions(resumenNotasCreditoReporteService);
    }

    @Test
    void comprasPeriodoDelegaFiltrosValidados() {
        byte[] esperado = new byte[] { 10, 11 };
        when(comprasPeriodoReporteService.generar(
                2, java.time.LocalDate.of(2026, 9, 1), java.time.LocalDate.of(2026, 9, 19), 4,
                xyz.pangosoft.dtodo.model.enums.EstadoCompraEnum.ACTIVA, "PDF"))
                .thenReturn(esperado);

        byte[] resultado = service.generarComprasPeriodo(
                2, "2026-09-01", "2026-09-19", 4, "activa", "pdf");

        assertArrayEquals(esperado, resultado);
        verify(comprasPeriodoReporteService).generar(
                2, java.time.LocalDate.of(2026, 9, 1), java.time.LocalDate.of(2026, 9, 19), 4,
                xyz.pangosoft.dtodo.model.enums.EstadoCompraEnum.ACTIVA, "PDF");
    }

    @Test
    void comprasPeriodoRechazaProveedorInvalido() {
        assertThrows(BadRequestException.class, () -> service.generarComprasPeriodo(
                1, "2026-09-01", "2026-09-19", 0, null, "PDF"));

        verifyNoInteractions(comprasPeriodoReporteService);
    }
}
