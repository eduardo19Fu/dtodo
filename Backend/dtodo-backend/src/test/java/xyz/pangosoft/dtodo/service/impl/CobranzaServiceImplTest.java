package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.dto.CobranzaClienteDto;
import xyz.pangosoft.dtodo.dto.CobranzaDto;
import xyz.pangosoft.dtodo.dto.MovimientoEstadoCuentaDto;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.ReciboCaja;
import xyz.pangosoft.dtodo.model.Ruta;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.enums.CondicionPagoEnum;
import xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum;
import xyz.pangosoft.dtodo.model.enums.FormaPagoReciboEnum;
import xyz.pangosoft.dtodo.repository.IClienteRepository;
import xyz.pangosoft.dtodo.repository.IProformaEntregaRepository;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.repository.IReciboCajaRepository;

class CobranzaServiceImplTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 30);

    private final IClienteRepository clientes = mock(IClienteRepository.class);
    private final IProformaRepository proformas = mock(IProformaRepository.class);
    private final IProformaEntregaRepository entregas = mock(IProformaEntregaRepository.class);
    private final IReciboCajaRepository recibos = mock(IReciboCajaRepository.class);
    private final CobranzaServiceImpl service = new CobranzaServiceImpl(clientes, proformas, entregas, recibos);

    private final Ruta ruta1 = Ruta.builder().idRuta(1).nombre("RUTA 1").build();
    private final Ruta ruta2 = Ruta.builder().idRuta(2).nombre("RUTA 2").build();

    private Cliente cliente(int id, String nombre, Ruta ruta) {
        return Cliente.builder().idCliente(id).nombre(nombre).ruta(ruta).limiteCredito(new BigDecimal("5000")).diasCredito(30).build();
    }

    private Proforma pedido(long id, Cliente cliente, CondicionPagoEnum condicion, Integer dias) {
        Proforma p = new Proforma();
        p.setIdProforma(id);
        p.setNoProforma("P-" + id);
        p.setCliente(cliente);
        p.setTotal(new BigDecimal("1000.00"));
        p.setCondicionPago(condicion);
        p.setDiasCredito(dias);
        p.setEstadoPedido(EstadoPedidoEnum.ENTREGADA);
        return p;
    }

    /** Fila de {@code entregadoDeTodos}: cliente, pedido, unidades, importe de la línea, unidades de la línea, fecha. */
    private static Object[] entrega(int idCliente, long idPedido, String importe, LocalDate fecha) {
        return new Object[] { idCliente, idPedido, 10, new BigDecimal(importe), 10, fecha };
    }

    private static List<Object[]> filas(Object[]... filas) {
        return new ArrayList<>(Arrays.asList(filas));
    }

    private void datos(List<Proforma> pedidos, List<Object[]> filasEntregas, List<Object[]> filasAbonos) {
        when(proformas.pedidosVigentes()).thenReturn(pedidos);
        when(entregas.entregadoDeTodos()).thenReturn(filasEntregas);
        when(recibos.abonosDeTodos()).thenReturn(filasAbonos);
    }

    private static CobranzaClienteDto de(CobranzaDto cobranza, int idCliente) {
        return cobranza.getClientes().stream().filter(c -> c.getIdCliente() == idCliente).findFirst().get();
    }

    // ------------------------------------------------------------------------------------------ antigüedad

    @Test
    void cadaPedidoCaeEnSuTramoSegunLosDiasQueLlevaVencido() {
        Cliente ana = cliente(1, "Libreria Ana", ruta1);
        // Primera entrega + 30 días de crédito = vencimiento; HOY es 30-nov-2026.
        Proforma porVencer = pedido(1, ana, CondicionPagoEnum.CREDITO, 30);      // entregado 10-nov => vence 10-dic (por vencer)
        Proforma vence30 = pedido(2, ana, CondicionPagoEnum.CREDITO, 30);        // entregado 1-nov => vence 1-dic (aún no)
        Proforma v1a30 = pedido(3, ana, CondicionPagoEnum.CREDITO, 30);          // entregado 1-oct => vence 31-oct => 30 días
        Proforma v31a60 = pedido(4, ana, CondicionPagoEnum.CREDITO, 30);         // entregado 1-sep => vence 1-oct => 60 días
        Proforma masDe60 = pedido(5, ana, CondicionPagoEnum.CREDITO, 30);        // entregado 1-ago => vence 31-ago => 91 días
        datos(Arrays.asList(porVencer, vence30, v1a30, v31a60, masDe60), Arrays.asList(
                entrega(1, 1, "100.00", LocalDate.of(2026, 11, 10)), entrega(1, 2, "200.00", LocalDate.of(2026, 11, 1)),
                entrega(1, 3, "300.00", LocalDate.of(2026, 10, 1)), entrega(1, 4, "400.00", LocalDate.of(2026, 9, 1)),
                entrega(1, 5, "500.00", LocalDate.of(2026, 8, 1))), new ArrayList<>());

        CobranzaClienteDto fila = de(service.cuentasPorCobrar(null, false, HOY), 1);

        assertEquals(new BigDecimal("300.00"), fila.getPorVencer());
        assertEquals(new BigDecimal("300.00"), fila.getVencido1a30());
        assertEquals(new BigDecimal("400.00"), fila.getVencido31a60());
        assertEquals(new BigDecimal("500.00"), fila.getVencidoMasDe60());
        assertEquals(new BigDecimal("1200.00"), fila.getVencido());
        assertEquals(new BigDecimal("1500.00"), fila.getTotal());
        assertEquals(91, fila.getDiasMaxAtraso());
    }

    @Test
    void elDiaDelVencimientoTodaviaNoEstaVencidoYAlSiguienteSi() {
        Cliente ana = cliente(1, "Libreria Ana", ruta1);
        Proforma hoyVence = pedido(1, ana, CondicionPagoEnum.CREDITO, 30);
        Proforma ayerVencio = pedido(2, ana, CondicionPagoEnum.CREDITO, 30);
        datos(Arrays.asList(hoyVence, ayerVencio), Arrays.asList(
                entrega(1, 1, "100.00", HOY.minusDays(30)), entrega(1, 2, "50.00", HOY.minusDays(31))), new ArrayList<>());

        CobranzaClienteDto fila = de(service.cuentasPorCobrar(null, false, HOY), 1);

        assertEquals(new BigDecimal("100.00"), fila.getPorVencer());
        assertEquals(new BigDecimal("50.00"), fila.getVencido1a30());
        assertEquals(1, fila.getDiasMaxAtraso());
    }

    @Test
    void unPedidoDeContadoEntregadoSinCobrarVenceElDiaDeLaEntrega() {
        Cliente ana = cliente(1, "Libreria Ana", ruta1);
        datos(Arrays.asList(pedido(1, ana, CondicionPagoEnum.CONTADO, null)),
                filas(entrega(1, 1, "100.00", HOY.minusDays(5))), new ArrayList<>());

        CobranzaClienteDto fila = de(service.cuentasPorCobrar(null, false, HOY), 1);

        assertEquals(new BigDecimal("100.00"), fila.getVencido1a30());
        assertEquals(5, fila.getDiasMaxAtraso());
    }

    @Test
    void loPagadoSeDescuentaYUnClienteAlDiaNoApareceNiVence() {
        Cliente ana = cliente(1, "Libreria Ana", ruta1);
        Cliente luis = cliente(2, "Tienda Luis", ruta1);
        datos(Arrays.asList(pedido(1, ana, CondicionPagoEnum.CREDITO, 30), pedido(2, luis, CondicionPagoEnum.CREDITO, 30)),
                filas(entrega(1, 1, "100.00", HOY.minusDays(60)), entrega(2, 2, "80.00", HOY.minusDays(60))),
                filas(new Object[] { 1, 1L, null, new BigDecimal("40.00") }, new Object[] { 2, 2L, null, new BigDecimal("80.00") }));

        CobranzaDto cobranza = service.cuentasPorCobrar(null, false, HOY);

        assertEquals(1, cobranza.getClientes().size());
        assertEquals(new BigDecimal("60.00"), de(cobranza, 1).getTotal());
    }

    @Test
    void losAbonosSinPedidoPaganLoMasAntiguoPrimero() {
        Cliente ana = cliente(1, "Libreria Ana", ruta1);
        datos(Arrays.asList(pedido(1, ana, CondicionPagoEnum.CREDITO, 30), pedido(2, ana, CondicionPagoEnum.CREDITO, 30)),
                filas(entrega(1, 1, "100.00", HOY.minusDays(90)), entrega(1, 2, "100.00", HOY.minusDays(40))),
                filas(new Object[] { 1, null, null, new BigDecimal("100.00") }));

        CobranzaClienteDto fila = de(service.cuentasPorCobrar(null, false, HOY), 1);

        // El abono salda el pedido más antiguo; queda el más reciente, vencido hace 10 días.
        assertEquals(new BigDecimal("100.00"), fila.getTotal());
        assertEquals(new BigDecimal("100.00"), fila.getVencido1a30());
        assertEquals(10, fila.getDiasMaxAtraso());
    }

    // ------------------------------------------------------------------------------------------ lista y filtros

    @Test
    void laListaVaDeMayorAMenorVencidoConSusTotales() {
        Cliente ana = cliente(1, "Libreria Ana", ruta1);
        Cliente luis = cliente(2, "Tienda Luis", ruta2);
        Cliente mara = cliente(3, "Papeleria Mara", ruta1);
        datos(Arrays.asList(pedido(1, ana, CondicionPagoEnum.CREDITO, 30), pedido(2, luis, CondicionPagoEnum.CREDITO, 30),
                pedido(3, mara, CondicionPagoEnum.CREDITO, 30)),
                filas(entrega(1, 1, "100.00", HOY.minusDays(40)), entrega(2, 2, "900.00", HOY.minusDays(45)),
                        entrega(3, 3, "500.00", HOY.minusDays(5))), new ArrayList<>());

        CobranzaDto cobranza = service.cuentasPorCobrar(null, false, HOY);

        assertEquals(Arrays.asList("Tienda Luis", "Libreria Ana", "Papeleria Mara"),
                cobranza.getClientes().stream().map(CobranzaClienteDto::getCliente).collect(java.util.stream.Collectors.toList()));
        assertEquals(new BigDecimal("1500.00"), cobranza.getTotal());
        assertEquals(new BigDecimal("1000.00"), cobranza.getVencido());
        assertEquals(new BigDecimal("500.00"), cobranza.getPorVencer());
        assertEquals(2, cobranza.getClientesConVencido());
    }

    @Test
    void filtraPorRutaYSoloVencidos() {
        Cliente ana = cliente(1, "Libreria Ana", ruta1);
        Cliente luis = cliente(2, "Tienda Luis", ruta2);
        Cliente mara = cliente(3, "Papeleria Mara", ruta1);
        datos(Arrays.asList(pedido(1, ana, CondicionPagoEnum.CREDITO, 30), pedido(2, luis, CondicionPagoEnum.CREDITO, 30),
                pedido(3, mara, CondicionPagoEnum.CREDITO, 30)),
                filas(entrega(1, 1, "100.00", HOY.minusDays(40)), entrega(2, 2, "900.00", HOY.minusDays(45)),
                        entrega(3, 3, "500.00", HOY.minusDays(5))), new ArrayList<>());

        assertEquals(2, service.cuentasPorCobrar(1, false, HOY).getClientes().size());
        CobranzaDto vencidosRuta1 = service.cuentasPorCobrar(1, true, HOY);
        assertEquals(1, vencidosRuta1.getClientes().size());
        assertEquals("Libreria Ana", vencidosRuta1.getClientes().get(0).getCliente());
        assertTrue(service.cuentasPorCobrar(99, false, HOY).getClientes().isEmpty());
    }

    // ------------------------------------------------------------------------------------------ estado de cuenta

    private static Object[] lineaEntrega(long idEntrega, long idPedido, String noPedido, LocalDate fecha, int unidades, String importe,
            int unidadesLinea) {
        return new Object[] { idEntrega, idPedido, noPedido, fecha, unidades, new BigDecimal(importe), unidadesLinea };
    }

    private ReciboCaja recibo(long id, LocalDate fecha, String monto, FormaPagoReciboEnum forma, String referencia) {
        return ReciboCaja.builder().idRecibo(id).sucursal(Sucursal.builder().idSucursal(2).build()).numero(id).monto(new BigDecimal(monto))
                .fechaPago(fecha).formaPago(forma).referencia(referencia).build();
    }

    @Test
    void elEstadoDeCuentaOrdenaPorFechaConElCargoAntesQueElAbonoYAcumulaElSaldo() {
        List<Object[]> lineas = Arrays.asList(
                lineaEntrega(10, 1, "P-1", LocalDate.of(2026, 10, 5), 4, "100.00", 10),
                lineaEntrega(10, 1, "P-1", LocalDate.of(2026, 10, 5), 5, "50.00", 5),
                lineaEntrega(11, 1, "P-1", LocalDate.of(2026, 10, 20), 6, "100.00", 10));
        List<ReciboCaja> recibosDelCliente = Arrays.asList(
                recibo(1, LocalDate.of(2026, 10, 5), "30.00", FormaPagoReciboEnum.EFECTIVO, null),
                recibo(2, LocalDate.of(2026, 10, 25), "200.00", FormaPagoReciboEnum.DEPOSITO, "BR-99"));

        List<MovimientoEstadoCuentaDto> movimientos = CobranzaServiceImpl.armarEstadoDeCuenta(lineas, recibosDelCliente);

        assertEquals(4, movimientos.size());
        // Entrega 10 = 4 × 10 + 5 × 10 = 90; el recibo del mismo día va después.
        assertEquals("Pedido P-1", movimientos.get(0).getDocumento());
        assertEquals(new BigDecimal("90.00"), movimientos.get(0).getCargo());
        assertEquals(new BigDecimal("90.00"), movimientos.get(0).getSaldo());
        assertEquals("Recibo S2-000001", movimientos.get(1).getDocumento());
        assertEquals(new BigDecimal("60.00"), movimientos.get(1).getSaldo());
        assertEquals(new BigDecimal("60.00"), movimientos.get(2).getCargo());
        assertEquals(new BigDecimal("120.00"), movimientos.get(2).getSaldo());
        assertEquals("Depósito BR-99", movimientos.get(3).getDetalle());
        assertEquals(new BigDecimal("-80.00"), movimientos.get(3).getSaldo());
        assertEquals("25/10/2026", movimientos.get(3).getFechaTexto());
    }

    @Test
    void sinMovimientosElEstadoDeCuentaEstaVacioYUnClienteInexistenteEsNotFound() {
        assertTrue(CobranzaServiceImpl.armarEstadoDeCuenta(new ArrayList<>(), new ArrayList<>()).isEmpty());
        when(clientes.findById(77)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.estadoDeCuenta(77));
    }
}
