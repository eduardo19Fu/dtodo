package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.dto.CobranzaClienteDto;
import xyz.pangosoft.dtodo.dto.CobranzaDto;
import xyz.pangosoft.dtodo.dto.IndicadoresMesDto;
import xyz.pangosoft.dtodo.dto.IndicadoresMesDto.VentaVendedor;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import xyz.pangosoft.dtodo.repository.IDevolucionClienteRepository;
import xyz.pangosoft.dtodo.repository.IProformaEntregaRepository;
import xyz.pangosoft.dtodo.repository.IUsuarioRepository;
import xyz.pangosoft.dtodo.service.ICobranzaService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

class IndicadoresServiceImplTest {

    private final int anio = LocalDate.now().getYear() - 1;
    private final LocalDate octubre = LocalDate.of(anio, 10, 1);
    private final LocalDate noviembre = LocalDate.of(anio, 11, 1);
    private final LocalDate septiembre = LocalDate.of(anio, 9, 1);

    private final IProformaEntregaRepository entregas = mock(IProformaEntregaRepository.class);
    private final IDevolucionClienteRepository devoluciones = mock(IDevolucionClienteRepository.class);
    private final IUsuarioRepository usuarios = mock(IUsuarioRepository.class);
    private final IUsuarioService usuarioService = mock(IUsuarioService.class);
    private final ICobranzaService cobranza = mock(ICobranzaService.class);
    private final IndicadoresServiceImpl service = new IndicadoresServiceImpl(entregas, devoluciones, usuarios, usuarioService, cobranza);

    private final Usuario ana = Usuario.builder().idUsuario(1).primerNombre("Ana").apellido("Lopez").build();
    private final Usuario luis = Usuario.builder().idUsuario(2).primerNombre("Luis").apellido("Perez").build();
    private final Usuario exVendedor = Usuario.builder().idUsuario(3).primerNombre("Marta").apellido("Gomez").build();

    private void escenario() {
        when(usuarios.findVendedoresActivos()).thenReturn(Arrays.asList(ana, luis));
        when(usuarioService.findById(3)).thenReturn(exVendedor);
        when(cobranza.cuentasPorCobrar(null, false)).thenReturn(new CobranzaDto());
    }

    /** Una línea entregada: vendedor, unidades entregadas, importe de la línea, unidades de la línea, fecha. */
    private static Object[] entregada(int idVendedor, int unidades, String importe, int unidadesLinea) {
        return new Object[] { idVendedor, unidades, new BigDecimal(importe), unidadesLinea, LocalDate.now() };
    }

    private static Object[] devuelto(int idVendedor, String valor) {
        return new Object[] { idVendedor, new BigDecimal(valor) };
    }

    private static Object[] completado(int idVendedor, long idPedido, LocalDate estimada, LocalDate ultima) {
        return new Object[] { idVendedor, idPedido, estimada, ultima };
    }

    private static VentaVendedor de(IndicadoresMesDto dto, int idVendedor) {
        return dto.getVendedores().stream().filter(v -> v.getIdVendedor() == idVendedor).findFirst().get();
    }

    // ------------------------------------------------------------------------------------------ ventas

    @Test
    void lasVentasSonLoEntregadoMenosLoDevueltoPorVendedorDeMayorAMenor() {
        escenario();
        when(entregas.entregadoDelPeriodo(TipoProformaEnum.MAYORISTA, octubre, noviembre)).thenReturn(Arrays.asList(
                entregada(1, 4, "100.00", 10), entregada(1, 6, "100.00", 10), entregada(2, 10, "300.00", 10), entregada(3, 1, "50.00", 1)));
        when(devoluciones.devueltoDelPeriodoPorVendedor(octubre, noviembre)).thenReturn(Arrays.asList(devuelto(2, "60.00"), devuelto(3, "5.50")));

        IndicadoresMesDto dto = service.indicadores(anio, 10);

        assertEquals(octubre, dto.getDesde());
        assertEquals(LocalDate.of(anio, 10, 31), dto.getHasta());
        assertEquals(new BigDecimal("450.00"), dto.getTotalEntregado());
        assertEquals(new BigDecimal("65.50"), dto.getTotalDevuelto());
        assertEquals(new BigDecimal("384.50"), dto.getTotalNeto());
        assertEquals(Arrays.asList("Luis Perez", "Ana Lopez", "Marta Gomez"),
                dto.getVendedores().stream().map(VentaVendedor::getVendedor).collect(java.util.stream.Collectors.toList()));
        VentaVendedor luisVenta = de(dto, 2);
        assertEquals(new BigDecimal("300.00"), luisVenta.getEntregado());
        assertEquals(new BigDecimal("60.00"), luisVenta.getDevuelto());
        assertEquals(new BigDecimal("240.00"), luisVenta.getNeto());
        assertEquals(new BigDecimal("62.4"), luisVenta.getParticipacion());
        assertEquals(new BigDecimal("100.00"), de(dto, 1).getNeto());
        assertEquals(new BigDecimal("44.50"), de(dto, 3).getNeto());   // un vendedor que ya no es activo pero vendió
    }

    @Test
    void unVendedorActivoSinMovimientosApareceEnCero() {
        escenario();

        IndicadoresMesDto dto = service.indicadores(anio, 10);

        assertEquals(2, dto.getVendedores().size());
        assertEquals(BigDecimal.ZERO, dto.getTotalNeto());
        assertEquals(BigDecimal.ZERO, de(dto, 1).getParticipacion());
        assertNull(dto.getPorcentajeATiempo());
        assertNull(de(dto, 1).getPorcentajeATiempo());
    }

    @Test
    void comparaConLasVentasNetasDelMesAnterior() {
        escenario();
        when(entregas.entregadoDelPeriodo(TipoProformaEnum.MAYORISTA, septiembre, octubre)).thenReturn(Collections.singletonList(
                entregada(1, 10, "500.00", 10)));
        when(devoluciones.devueltoDelPeriodoPorVendedor(septiembre, octubre)).thenReturn(Collections.singletonList(devuelto(1, "20.00")));

        assertEquals(new BigDecimal("480.00"), service.indicadores(anio, 10).getNetoMesAnterior());
    }

    // ------------------------------------------------------------------------------------------ a tiempo

    @Test
    void unPedidoEstaATiempoSiSuUltimaEntregaFueEnOAntesDeLaFechaEstimada() {
        escenario();
        LocalDate estimada = LocalDate.of(anio, 10, 15);
        when(entregas.pedidosCompletadosDelPeriodo(octubre, noviembre)).thenReturn(Arrays.asList(
                completado(1, 10, estimada, estimada),                 // el mismo día: a tiempo
                completado(1, 11, estimada, estimada.minusDays(3)),    // antes: a tiempo
                completado(1, 12, estimada, estimada.plusDays(1)),     // un día tarde
                completado(2, 13, estimada, estimada.plusDays(5))));   // tarde

        IndicadoresMesDto dto = service.indicadores(anio, 10);

        assertEquals(4, dto.getPedidosConFecha());
        assertEquals(2, dto.getPedidosATiempo());
        assertEquals(new BigDecimal("50.0"), dto.getPorcentajeATiempo());
        assertEquals(3, de(dto, 1).getPedidosConFecha());
        assertEquals(new BigDecimal("66.7"), de(dto, 1).getPorcentajeATiempo());
        assertEquals(new BigDecimal("0.0"), de(dto, 2).getPorcentajeATiempo());
    }

    @Test
    void elPorcentajeSeRedondeaAUnDecimalYSinPedidosEsNulo() {
        assertEquals(new BigDecimal("33.3"), IndicadoresServiceImpl.porcentaje(1, 3));
        assertEquals(new BigDecimal("100.0"), IndicadoresServiceImpl.porcentaje(5, 5));
        assertNull(IndicadoresServiceImpl.porcentaje(0, 0));
    }

    // ------------------------------------------------------------------------------------------ por cobrar y validación

    @Test
    void traeLasCuentasPorCobrarDeLaCobranzaDeHoy() {
        escenario();
        CobranzaClienteDto a = new CobranzaClienteDto();
        CobranzaClienteDto b = new CobranzaClienteDto();
        CobranzaDto deHoy = new CobranzaDto();
        deHoy.setClientes(Arrays.asList(a, b));
        deHoy.setTotal(new BigDecimal("900.00"));
        deHoy.setPorVencer(new BigDecimal("400.00"));
        deHoy.setVencido(new BigDecimal("500.00"));
        deHoy.setClientesConVencido(1);
        when(cobranza.cuentasPorCobrar(null, false)).thenReturn(deHoy);

        IndicadoresMesDto.CuentasPorCobrar cuentas = service.indicadores(anio, 10).getCuentasPorCobrar();

        assertEquals(new BigDecimal("900.00"), cuentas.getTotal());
        assertEquals(new BigDecimal("400.00"), cuentas.getPorVencer());
        assertEquals(new BigDecimal("500.00"), cuentas.getVencido());
        assertEquals(2, cuentas.getClientesConDeuda());
        assertEquals(1, cuentas.getClientesConVencido());
    }

    @Test
    void soloAceptaMesesValidosHastaElActual() {
        escenario();
        LocalDate hoy = LocalDate.now();

        assertThrows(ReglaNegocioException.class, () -> service.indicadores(2019, 12));
        assertThrows(ReglaNegocioException.class, () -> service.indicadores(anio, 0));
        assertThrows(ReglaNegocioException.class, () -> service.indicadores(anio, 13));
        assertThrows(ReglaNegocioException.class, () -> service.indicadores(hoy.plusMonths(1).getYear(), hoy.plusMonths(1).getMonthValue()));
        assertEquals(hoy.getMonthValue(), service.indicadores(hoy.getYear(), hoy.getMonthValue()).getMes());
        assertTrue(service.indicadores(2020, 1).getVendedores().size() >= 0);
    }
}
