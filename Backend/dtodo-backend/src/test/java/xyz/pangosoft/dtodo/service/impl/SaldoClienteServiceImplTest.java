package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.dto.PedidoSaldoDto;
import xyz.pangosoft.dtodo.dto.SaldoClienteDto;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.enums.CondicionPagoEnum;
import xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum;
import xyz.pangosoft.dtodo.repository.IClienteRepository;
import xyz.pangosoft.dtodo.repository.IProformaEntregaRepository;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.repository.IReciboCajaRepository;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;

class SaldoClienteServiceImplTest {

    private final Cliente cliente = Cliente.builder().idCliente(5).nombre("Libreria Uno").limiteCredito(new BigDecimal("1000.00"))
            .diasCredito(30).build();

    private Proforma pedido(long id, CondicionPagoEnum condicion, EstadoPedidoEnum etapa) {
        Proforma p = new Proforma();
        p.setIdProforma(id);
        p.setNoProforma("P-" + id);
        p.setTotal(new BigDecimal("500.00"));
        p.setCondicionPago(condicion);
        p.setDiasCredito(condicion == CondicionPagoEnum.CREDITO ? 30 : null);
        p.setEstadoPedido(etapa);
        return p;
    }

    /** Una línea entregada: pedido, unidades entregadas, importe de la línea, unidades de la línea, fecha. */
    private static Object[] entregada(long idPedido, int unidades, String importe, int unidadesLinea, LocalDate fecha) {
        return new Object[] { idPedido, unidades, new BigDecimal(importe), unidadesLinea, fecha };
    }

    /** Un recibo: pedido, pedido de origen de la factura, monto. */
    private static Object[] abono(Long idPedido, Long idPedidoDeFactura, String monto) {
        return new Object[] { idPedido, idPedidoDeFactura, new BigDecimal(monto) };
    }

    private static List<Object[]> filas(Object[]... filas) {
        return new ArrayList<>(Arrays.asList(filas));
    }

    private static SaldoClienteDto calcular(Cliente cliente, List<Proforma> pedidos, List<Object[]> entregas, List<Object[]> abonos) {
        return SaldoClienteServiceImpl.calcular(cliente, pedidos, entregas, abonos);
    }

    private static PedidoSaldoDto de(SaldoClienteDto saldo, long idPedido) {
        return saldo.getPedidos().stream().filter(p -> p.getIdProforma() == idPedido).findFirst().get();
    }

    @Test
    void elSaldoEsLoEntregadoMenosLoAbonado() {
        Proforma p = pedido(1, CondicionPagoEnum.CREDITO, EstadoPedidoEnum.ENTREGADA_PARCIAL);

        SaldoClienteDto saldo = calcular(cliente, Arrays.asList(p),
                filas(entregada(1, 4, "100.00", 10, LocalDate.of(2026, 10, 5)), entregada(1, 3, "100.00", 10, LocalDate.of(2026, 10, 9))),
                filas(abono(1L, null, "30.00")));

        assertEquals(new BigDecimal("70.00"), saldo.getEntregado());
        assertEquals(new BigDecimal("30.00"), saldo.getAbonado());
        assertEquals(new BigDecimal("40.00"), saldo.getSaldo());
        assertEquals(new BigDecimal("960.00"), saldo.getDisponible());
        PedidoSaldoDto dto = de(saldo, 1);
        assertEquals(new BigDecimal("40.00"), dto.getPendiente());
        assertEquals(LocalDate.of(2026, 10, 5), dto.getPrimeraEntrega());
        assertEquals(LocalDate.of(2026, 11, 4), dto.getVencimiento());
    }

    @Test
    void unPagoAdelantadoDeUnPedidoSinEntregarQuedaAFavorDelCliente() {
        Proforma p = pedido(1, CondicionPagoEnum.CONTADO, EstadoPedidoEnum.CONFIRMADA);

        SaldoClienteDto saldo = calcular(cliente, Arrays.asList(p), Collections.emptyList(), filas(abono(1L, null, "500.00")));

        assertEquals(new BigDecimal("-500.00"), saldo.getSaldo());
        assertEquals(new BigDecimal("1000.00"), saldo.getDisponible());
        PedidoSaldoDto dto = de(saldo, 1);
        assertEquals(new BigDecimal("500.00"), dto.getCobrado());
        assertEquals(new BigDecimal("0.00"), dto.getPendiente().setScale(2));
        assertNull(dto.getVencimiento());
    }

    @Test
    void elReciboDeUnaFacturaPagaElPedidoDeOrigenDeLaFactura() {
        Proforma p = pedido(1, CondicionPagoEnum.CREDITO, EstadoPedidoEnum.ENTREGADA);

        SaldoClienteDto saldo = calcular(cliente, Arrays.asList(p), filas(entregada(1, 10, "100.00", 10, LocalDate.of(2026, 10, 5))),
                filas(abono(null, 1L, "60.00")));

        assertEquals(new BigDecimal("60.00"), de(saldo, 1).getCobrado());
        assertEquals(new BigDecimal("40.00"), de(saldo, 1).getPendiente());
    }

    @Test
    void losAbonosSinPedidoPaganPrimeroLoMasAntiguo() {
        Proforma viejo = pedido(2, CondicionPagoEnum.CREDITO, EstadoPedidoEnum.ENTREGADA_PARCIAL);
        Proforma nuevo = pedido(1, CondicionPagoEnum.CREDITO, EstadoPedidoEnum.ENTREGADA_PARCIAL);

        SaldoClienteDto saldo = calcular(cliente, Arrays.asList(nuevo, viejo), filas(entregada(1, 10, "100.00", 10, LocalDate.of(2026, 10, 20)), entregada(2, 10, "80.00", 10, LocalDate.of(2026, 10, 1))),
                filas(abono(null, null, "100.00")));

        // El pedido 2 se entregó primero (aunque su número sea mayor): 80 de abono lo saldan y 20 pasan al otro.
        assertEquals(new BigDecimal("80.00"), de(saldo, 2).getAplicadoDeAbonos());
        assertEquals(new BigDecimal("0.00"), de(saldo, 2).getPendiente().setScale(2));
        assertEquals(new BigDecimal("20.00"), de(saldo, 1).getAplicadoDeAbonos());
        assertEquals(new BigDecimal("80.00"), de(saldo, 1).getPendiente());
        assertEquals(new BigDecimal("80.00"), saldo.getSaldo());
    }

    @Test
    void elPagoLigadoNoSeRepartePeroElSobranteDeUnPedidoNoPagaOtro() {
        Proforma a = pedido(1, CondicionPagoEnum.CREDITO, EstadoPedidoEnum.ENTREGADA);
        Proforma b = pedido(2, CondicionPagoEnum.CREDITO, EstadoPedidoEnum.ENTREGADA);

        SaldoClienteDto saldo = calcular(cliente, Arrays.asList(a, b), filas(entregada(1, 10, "100.00", 10, LocalDate.of(2026, 10, 1)), entregada(2, 10, "100.00", 10, LocalDate.of(2026, 10, 2))),
                filas(abono(1L, null, "150.00")));

        assertEquals(new BigDecimal("100.00"), de(saldo, 2).getPendiente());
        assertEquals(new BigDecimal("50.00"), saldo.getSaldo());
    }

    @Test
    void sinLimiteNoHayDisponibleYUnPedidoEntregadoYPagadoNoSeLista() {
        Cliente sinLimite = Cliente.builder().idCliente(6).nombre("Sin limite").build();
        Proforma cerrado = pedido(1, CondicionPagoEnum.CONTADO, EstadoPedidoEnum.ENTREGADA);
        Proforma abierto = pedido(2, CondicionPagoEnum.CONTADO, EstadoPedidoEnum.EN_RUTA);

        SaldoClienteDto saldo = calcular(sinLimite, Arrays.asList(cerrado, abierto),
                filas(entregada(1, 10, "100.00", 10, LocalDate.of(2026, 10, 1))), filas(abono(1L, null, "100.00")));

        assertNull(saldo.getDisponible());
        assertEquals(1, saldo.getPedidos().size());
        assertEquals(Long.valueOf(2), saldo.getPedidos().get(0).getIdProforma());
    }

    @Test
    void elDisponibleNuncaSuperaElLimiteAunqueElSaldoSeaAFavor() {
        SaldoClienteDto saldo = calcular(cliente, Collections.emptyList(), Collections.emptyList(), filas(abono(null, null, "300.00")));

        assertEquals(new BigDecimal("1000.00"), saldo.getDisponible());
        assertEquals(new BigDecimal("-300.00"), saldo.getSaldo());
    }

    @Test
    void sobrepasarElLimiteDejaElDisponibleEnNegativo() {
        Proforma p = pedido(1, CondicionPagoEnum.CREDITO, EstadoPedidoEnum.ENTREGADA);

        SaldoClienteDto saldo = calcular(cliente, Arrays.asList(p), filas(entregada(1, 10, "1200.00", 10, LocalDate.now())),
                new ArrayList<>());

        assertEquals(new BigDecimal("-200.00"), saldo.getDisponible());
    }

    @Test
    void elValorIgnoraLineasSinUnidadesYRedondeaAlSumar() {
        BigDecimal valor = SaldoClienteServiceImpl.valor(filas(entregada(1, 1, "10.00", 3, null), entregada(1, 1, "10.00", 3, null), entregada(1, 1, "10.00", 3, null),
                entregada(1, 5, "99.00", 0, null)));

        assertEquals(new BigDecimal("10.00"), valor);
    }

    @Test
    void elServicioRespondeNotFoundSiElClienteNoExisteYValidaElAmbito() {
        IClienteRepository clientes = mock(IClienteRepository.class);
        IAmbitoVendedorService ambito = mock(IAmbitoVendedorService.class);
        IProformaRepository proformas = mock(IProformaRepository.class);
        IProformaEntregaRepository entregas = mock(IProformaEntregaRepository.class);
        IReciboCajaRepository recibos = mock(IReciboCajaRepository.class);
        SaldoClienteServiceImpl service = new SaldoClienteServiceImpl(clientes, proformas, entregas, recibos, ambito);
        when(clientes.findById(77)).thenReturn(Optional.empty());
        when(clientes.findById(5)).thenReturn(Optional.of(cliente));
        when(proformas.pedidosVigentesDelCliente(5)).thenReturn(new ArrayList<>());
        when(entregas.entregadoDelCliente(5)).thenReturn(new ArrayList<>());
        when(recibos.abonosDelCliente(5)).thenReturn(new ArrayList<>());

        assertThrows(NotFoundException.class, () -> service.saldo(77));
        assertTrue(service.saldo(5).getPedidos().isEmpty());
        verify(ambito).exigirClienteEnAmbito(cliente);
    }

    @Test
    void laExposicionDeCreditoSumaElSaldoPendienteYLoQueFaltaEntregarDeLosOtrosPedidosACredito() {
        IClienteRepository clientes = mock(IClienteRepository.class);
        IProformaRepository proformas = mock(IProformaRepository.class);
        IProformaEntregaRepository entregas = mock(IProformaEntregaRepository.class);
        IReciboCajaRepository recibos = mock(IReciboCajaRepository.class);
        SaldoClienteServiceImpl service = new SaldoClienteServiceImpl(clientes, proformas, entregas, recibos,
                mock(IAmbitoVendedorService.class));
        when(clientes.findById(5)).thenReturn(Optional.of(cliente));
        // Pedido 1 (crédito, total 500): se entregaron 100 y se pagaron 30 => debe 70 y le faltan 400 por entregar.
        // Pedido 2 (crédito, total 500): sin entregar => 500 comprometidos. Pedido 3 (contado) no cuenta como crédito.
        Proforma uno = pedido(1, CondicionPagoEnum.CREDITO, EstadoPedidoEnum.ENTREGADA_PARCIAL);
        Proforma dos = pedido(2, CondicionPagoEnum.CREDITO, EstadoPedidoEnum.CONFIRMADA);
        Proforma tres = pedido(3, CondicionPagoEnum.CONTADO, EstadoPedidoEnum.CONFIRMADA);
        when(proformas.pedidosVigentesDelCliente(5)).thenReturn(new ArrayList<>(Arrays.asList(uno, dos, tres)));
        when(entregas.entregadoDelCliente(5)).thenReturn(filas(entregada(1, 10, "100.00", 10, LocalDate.of(2026, 10, 1))));
        when(recibos.abonosDelCliente(5)).thenReturn(filas(abono(1L, null, "30.00")));

        assertEquals(new BigDecimal("970.00"), service.exposicionDeCredito(5, null));
        // Al evaluar el pedido 2 no se cuenta a sí mismo.
        assertEquals(new BigDecimal("470.00"), service.exposicionDeCredito(5, 2L));
    }

    @Test
    void conSaldoAFavorLaExposicionNoEsNegativa() {
        IClienteRepository clientes = mock(IClienteRepository.class);
        IProformaRepository proformas = mock(IProformaRepository.class);
        IProformaEntregaRepository entregas = mock(IProformaEntregaRepository.class);
        IReciboCajaRepository recibos = mock(IReciboCajaRepository.class);
        SaldoClienteServiceImpl service = new SaldoClienteServiceImpl(clientes, proformas, entregas, recibos,
                mock(IAmbitoVendedorService.class));
        when(clientes.findById(5)).thenReturn(Optional.of(cliente));
        when(proformas.pedidosVigentesDelCliente(5)).thenReturn(new ArrayList<>());
        when(entregas.entregadoDelCliente(5)).thenReturn(new ArrayList<>());
        when(recibos.abonosDelCliente(5)).thenReturn(filas(abono(null, null, "300.00")));

        assertEquals(new BigDecimal("0.00"), service.exposicionDeCredito(5, null));
    }
}
