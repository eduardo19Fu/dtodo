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
import xyz.pangosoft.dtodo.service.IComprasPeriodoReporteService;
import xyz.pangosoft.dtodo.service.IFacturaService;
import xyz.pangosoft.dtodo.service.IMovimientoProductoService;
import xyz.pangosoft.dtodo.service.IProductoService;
import xyz.pangosoft.dtodo.service.IProformaService;
import xyz.pangosoft.dtodo.service.IResumenNotasCreditoReporteService;

class ReporteServiceImplTest {

    private IFacturaService facturaService;
    private IMovimientoProductoService movimientoService;
    private IProductoService productoService;
    private IProformaService proformaService;
    private IResumenNotasCreditoReporteService resumenNotasCreditoReporteService;
    private IComprasPeriodoReporteService comprasPeriodoReporteService;
    private ReporteServiceImpl service;

    @BeforeEach
    void setUp() {
        facturaService = mock(IFacturaService.class);
        movimientoService = mock(IMovimientoProductoService.class);
        productoService = mock(IProductoService.class);
        proformaService = mock(IProformaService.class);
        resumenNotasCreditoReporteService = mock(IResumenNotasCreditoReporteService.class);
        comprasPeriodoReporteService = mock(IComprasPeriodoReporteService.class);
        service = new ReporteServiceImpl(
                facturaService, movimientoService, productoService, proformaService,
                resumenNotasCreditoReporteService, comprasPeriodoReporteService);
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

        verifyNoInteractions(facturaService, movimientoService, productoService, proformaService,
                resumenNotasCreditoReporteService, comprasPeriodoReporteService);
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
