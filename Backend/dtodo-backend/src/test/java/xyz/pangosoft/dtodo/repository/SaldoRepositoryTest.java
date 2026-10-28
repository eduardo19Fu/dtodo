package xyz.pangosoft.dtodo.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.DetalleProforma;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.Factura;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.ProformaEntrega;
import xyz.pangosoft.dtodo.model.ProformaEntregaDetalle;
import xyz.pangosoft.dtodo.model.ReciboCaja;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum;
import xyz.pangosoft.dtodo.model.enums.FormaPagoReciboEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;

/** Ejecuta contra H2 las consultas de lo entregado y lo cobrado que alimentan el saldo de los clientes. */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("context-test")
@Transactional
class SaldoRepositoryTest {

    @Autowired
    private IProformaEntregaRepository entregaRepository;
    @Autowired
    private IReciboCajaRepository reciboRepository;
    @Autowired
    private IProformaRepository proformaRepository;
    @Autowired
    private EntityManager em;

    private Estado activo;
    private Estado anulado;
    private Usuario ana;
    private Sucursal central;
    private Cliente cliente;
    private Cliente otro;

    private void base() {
        activo = Estado.builder().estado("ACTIVO").build();
        anulado = Estado.builder().estado("ANULADO").build();
        em.persist(activo);
        em.persist(anulado);
        ana = Usuario.builder().usuario("ana").primerNombre("Ana").apellido("Lopez").password("x").build();
        em.persist(ana);
        central = Sucursal.builder().nombre("Central").direccion("Jalapa").codigoEstablecimientoSat(1).esPrincipal(true).estado(activo).build();
        em.persist(central);
        cliente = Cliente.builder().nombre("LIBRERIA UNO").build();
        otro = Cliente.builder().nombre("OTRA").build();
        em.persist(cliente);
        em.persist(otro);
    }

    private Proforma pedido(String numero, Cliente deCliente, Estado estado, TipoProformaEnum tipo, EstadoPedidoEnum etapa) {
        Proforma proforma = new Proforma();
        proforma.setNoProforma(numero);
        proforma.setTotal(new BigDecimal("500.00"));
        proforma.setEstado(estado);
        proforma.setUsuario(ana);
        proforma.setCliente(deCliente);
        proforma.setTipoProforma(tipo);
        proforma.setEstadoPedido(etapa);
        DetalleProforma linea = new DetalleProforma();
        linea.setCantidad(10);
        linea.setSubTotalDescuento(new BigDecimal("100.00"));
        proforma.getItemsProforma().add(linea);
        em.persist(proforma);
        return proforma;
    }

    private void entrega(Proforma pedido, int unidades, LocalDate fecha, boolean anulada) {
        ProformaEntrega entrega = ProformaEntrega.builder().idProforma(pedido.getIdProforma()).sucursal(central).usuario(ana)
                .fechaEntrega(fecha).fechaRegistro(LocalDateTime.now()).anulada(anulada).build();
        entrega.getDetalles().add(ProformaEntregaDetalle.builder().entrega(entrega)
                .idDetalle(pedido.getItemsProforma().get(0).getIdDetalle()).cantidad(unidades).cantidadCubierta(unidades)
                .cantidadDescontada(unidades).build());
        em.persist(entrega);
    }

    private Factura factura(Proforma origen) {
        Factura factura = new Factura();
        factura.setNoFactura(1001L);
        factura.setCliente(origen.getCliente());
        factura.setEstado(activo);
        factura.setIdProformaOrigen(origen.getIdProforma());
        em.persist(factura);
        return factura;
    }

    private void recibo(Cliente deCliente, Proforma pedido, Factura factura, String monto, boolean anulado) {
        em.persist(ReciboCaja.builder().sucursal(central).numero((long) (Math.random() * 1_000_000)).cliente(deCliente).proforma(pedido)
                .factura(factura).monto(new BigDecimal(monto)).formaPago(FormaPagoReciboEnum.EFECTIVO).fechaPago(LocalDate.now())
                .fechaRegistro(LocalDateTime.now()).usuario(ana).anulado(anulado).build());
    }

    @Test
    void loEntregadoDelClienteSoloCuentaEntregasVigentesDePedidosMayoristasVigentes() {
        base();
        Proforma vigente = pedido("1P", cliente, activo, TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.EN_RUTA);
        Proforma delPedidoAnulado = pedido("2P", cliente, anulado, TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.EN_RUTA);
        Proforma minorista = pedido("3P", cliente, activo, TipoProformaEnum.MINORISTA, null);
        Proforma deOtro = pedido("4P", otro, activo, TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.EN_RUTA);
        em.flush();
        entrega(vigente, 4, LocalDate.of(2026, 10, 5), false);
        entrega(vigente, 3, LocalDate.of(2026, 10, 9), true);
        entrega(delPedidoAnulado, 5, LocalDate.of(2026, 10, 6), false);
        entrega(minorista, 5, LocalDate.of(2026, 10, 6), false);
        entrega(deOtro, 5, LocalDate.of(2026, 10, 6), false);
        em.flush();

        List<Object[]> filas = entregaRepository.entregadoDelCliente(cliente.getIdCliente());

        assertEquals(1, filas.size());
        assertEquals(vigente.getIdProforma(), filas.get(0)[0]);
        assertEquals(4, ((Number) filas.get(0)[1]).intValue());
        assertEquals(new BigDecimal("100.00"), ((BigDecimal) filas.get(0)[2]).setScale(2));
        assertEquals(10, ((Number) filas.get(0)[3]).intValue());
        assertEquals(LocalDate.of(2026, 10, 5), filas.get(0)[4]);
        assertEquals(1, entregaRepository.entregadoDelPedido(vigente.getIdProforma()).size());
    }

    @Test
    void losAbonosTraenTambienLosQueNoIndicanPedido() {
        base();
        Proforma pedido = pedido("1P", cliente, activo, TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.CONFIRMADA);
        em.flush();
        Factura factura = factura(pedido);
        recibo(cliente, pedido, null, "100.00", false);
        recibo(cliente, null, factura, "50.00", false);
        recibo(cliente, null, null, "25.00", false);
        recibo(cliente, pedido, null, "999.00", true);
        recibo(otro, null, null, "7.00", false);
        em.flush();

        List<Object[]> filas = reciboRepository.abonosDelCliente(cliente.getIdCliente());

        assertEquals(3, filas.size());
        BigDecimal sinDestino = filas.stream().filter(f -> f[0] == null && f[1] == null).map(f -> (BigDecimal) f[2])
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("25.00").compareTo(sinDestino));
        assertTrue(filas.stream().anyMatch(f -> f[0] == null && pedido.getIdProforma().equals(f[1])), "El recibo de la factura apunta al pedido");
    }

    @Test
    void lo_cobrado_del_pedido_suma_los_ligados_al_pedido_o_a_su_factura_y_no_los_anulados() {
        base();
        Proforma pedido = pedido("1P", cliente, activo, TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.CONFIRMADA);
        Proforma otroPedido = pedido("2P", cliente, activo, TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.CONFIRMADA);
        em.flush();
        Factura factura = factura(pedido);
        recibo(cliente, pedido, null, "100.00", false);
        recibo(cliente, null, factura, "50.00", false);
        recibo(cliente, pedido, null, "999.00", true);
        recibo(cliente, otroPedido, null, "30.00", false);
        recibo(cliente, null, null, "25.00", false);
        em.flush();

        assertEquals(0, new BigDecimal("150.00").compareTo(reciboRepository.cobradoDelPedido(pedido.getIdProforma())));
        assertEquals(0, new BigDecimal("30.00").compareTo(reciboRepository.cobradoDelPedido(otroPedido.getIdProforma())));
    }

    @Test
    void losPedidosVigentesDelClienteSonSoloMayoristasConEtapaNoAnulados() {
        base();
        pedido("1P", cliente, activo, TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.CONFIRMADA);
        pedido("2P", cliente, anulado, TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.CONFIRMADA);
        pedido("3P", cliente, activo, TipoProformaEnum.MINORISTA, null);
        pedido("4P", otro, activo, TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.CONFIRMADA);
        em.flush();

        List<Proforma> lista = proformaRepository.pedidosVigentesDelCliente(cliente.getIdCliente());

        assertEquals(1, lista.size());
        assertEquals("1P", lista.get(0).getNoProforma());
    }

    @Test
    void laBandejaDeCreditoTraeSoloLosPedidosPendientesConSuExcesoYElLimiteDelCliente() {
        base();
        cliente.setLimiteCredito(new BigDecimal("1000.00"));
        em.merge(cliente);
        Proforma pendiente = pedido("1P", cliente, activo, TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.CONFIRMADA);
        pendiente.setCreditoAutorizacion(xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum.PENDIENTE);
        pendiente.setExcesoCredito(new BigDecimal("250.00"));
        Proforma aprobada = pedido("2P", cliente, activo, TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.CONFIRMADA);
        aprobada.setCreditoAutorizacion(xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum.APROBADA);
        pedido("3P", cliente, activo, TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.CONFIRMADA);
        em.flush();

        List<xyz.pangosoft.dtodo.dto.AutorizacionProformaDto> lista =
                proformaRepository.findCreditoPorAutorizar(xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum.PENDIENTE);

        assertEquals(1, lista.size());
        assertEquals("1P", lista.get(0).getNoProforma());
        assertEquals("CREDITO", lista.get(0).getTipo());
        assertEquals(0, new BigDecimal("250.00").compareTo(lista.get(0).getExcesoCredito()));
        assertEquals(0, new BigDecimal("1000.00").compareTo(lista.get(0).getLimiteCredito()));
    }

    @Test
    void lasConsultasParaTodosLosClientesTraenElClienteYLasDelDetalleLasEntregasLinea() {
        base();
        Proforma uno = pedido("1P", cliente, activo, TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.EN_RUTA);
        Proforma dos = pedido("2P", otro, activo, TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.EN_RUTA);
        Proforma minorista = pedido("3P", cliente, activo, TipoProformaEnum.MINORISTA, null);
        em.flush();
        entrega(uno, 4, LocalDate.of(2026, 10, 5), false);
        entrega(dos, 2, LocalDate.of(2026, 10, 6), false);
        entrega(uno, 1, LocalDate.of(2026, 10, 7), true);
        entrega(minorista, 3, LocalDate.of(2026, 10, 8), false);
        recibo(cliente, uno, null, "10.00", false);
        recibo(otro, null, null, "5.00", false);
        recibo(otro, null, null, "99.00", true);
        em.flush();

        List<Object[]> todas = entregaRepository.entregadoDeTodos();
        assertEquals(2, todas.size());
        assertTrue(todas.stream().anyMatch(f -> cliente.getIdCliente().equals(f[0]) && uno.getIdProforma().equals(f[1])));
        assertTrue(todas.stream().anyMatch(f -> otro.getIdCliente().equals(f[0]) && dos.getIdProforma().equals(f[1])));

        List<Object[]> abonos = reciboRepository.abonosDeTodos();
        assertEquals(2, abonos.size());

        List<Object[]> detalle = entregaRepository.entregasDelClienteDetalle(cliente.getIdCliente());
        assertEquals(1, detalle.size());
        assertEquals("1P", detalle.get(0)[2]);
        assertEquals(LocalDate.of(2026, 10, 5), detalle.get(0)[3]);
        assertEquals(4, ((Number) detalle.get(0)[4]).intValue());

        assertEquals(2, proformaRepository.pedidosVigentes().size());
        assertEquals(1, reciboRepository.findByCliente_IdClienteAndAnuladoFalseOrderByFechaPagoAscIdReciboAsc(cliente.getIdCliente()).size());
        assertEquals(1, reciboRepository.findByCliente_IdClienteAndAnuladoFalseOrderByFechaPagoAscIdReciboAsc(otro.getIdCliente()).size());
    }

    @Autowired
    private IDevolucionClienteRepository devolucionRepository;
    @Autowired
    private IFacturaRepository facturaRepository;

    private xyz.pangosoft.dtodo.model.DevolucionCliente devolucion(Proforma deLaPedido, Cliente deCliente, LocalDate fecha, String... valores) {
        xyz.pangosoft.dtodo.model.DevolucionCliente d = xyz.pangosoft.dtodo.model.DevolucionCliente.builder()
                .idProforma(deLaPedido.getIdProforma()).cliente(deCliente).fecha(fecha).motivo("dañado").total(BigDecimal.ZERO)
                .usuario(ana).fechaRegistro(LocalDateTime.now()).build();
        for (String valor : valores) {
            d.getDetalles().add(xyz.pangosoft.dtodo.model.DevolucionClienteDetalle.builder().devolucion(d).idDetalle(1L).cantidad(1)
                    .destino(xyz.pangosoft.dtodo.model.enums.DestinoDevolucionEnum.REINGRESO).valor(new BigDecimal(valor)).build());
        }
        em.persist(d);
        return d;
    }

    @Test
    void lasConsultasDeDevolucionesSumanPorPedidoClienteYVendedor() {
        base();
        Proforma uno = pedido("1P", cliente, activo, TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.ENTREGADA);
        Proforma dos = pedido("2P", otro, activo, TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.ENTREGADA);
        em.flush();
        devolucion(uno, cliente, LocalDate.of(2026, 10, 10), "10.00", "5.50");
        devolucion(uno, cliente, LocalDate.of(2026, 11, 2), "4.50");
        devolucion(dos, otro, LocalDate.of(2026, 10, 12), "7.00");
        em.flush();

        List<Object[]> delCliente = devolucionRepository.devueltoDelCliente(cliente.getIdCliente());
        assertEquals(1, delCliente.size());
        assertEquals(uno.getIdProforma(), delCliente.get(0)[0]);
        assertEquals(0, new BigDecimal("20.00").compareTo((BigDecimal) delCliente.get(0)[1]));

        assertEquals(2, devolucionRepository.devueltoDeTodos().size());
        assertEquals(0, new BigDecimal("20.00").compareTo(devolucionRepository.devueltoDelPedido(uno.getIdProforma())));
        assertEquals(0, BigDecimal.ZERO.compareTo(devolucionRepository.devueltoDelPedido(999L)));

        // Octubre: Ana (vendedora de ambos pedidos) devolvió 15.50 + 7.00.
        List<Object[]> octubre = devolucionRepository.devueltoDelPeriodoPorVendedor(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1));
        assertEquals(1, octubre.size());
        assertEquals(ana.getIdUsuario(), octubre.get(0)[0]);
        assertEquals(0, new BigDecimal("22.50").compareTo((BigDecimal) octubre.get(0)[1]));

        assertEquals(2, devolucionRepository.findByCliente_IdClienteOrderByFechaAscIdDevolucionAsc(cliente.getIdCliente()).size());
        assertEquals(1, devolucionRepository.buscar(otro.getIdCliente(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                org.springframework.data.domain.PageRequest.of(0, 10)).getTotalElements());
        assertEquals(3, devolucionRepository.buscar(null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                org.springframework.data.domain.PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void lasConsultasDelHistorialTraenTodasLasFacturasYRecibosDelPedido() {
        base();
        Proforma uno = pedido("1P", cliente, activo, TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.ENTREGADA);
        Proforma dos = pedido("2P", cliente, activo, TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.ENTREGADA);
        em.flush();
        Factura vigente = factura(uno);
        Factura anulada = factura(uno);
        anulada.setEstado(anulado);
        factura(dos);
        recibo(cliente, uno, null, "10.00", false);
        recibo(cliente, uno, null, "20.00", true);
        recibo(cliente, dos, null, "30.00", false);
        recibo(cliente, null, null, "40.00", false);
        em.flush();

        assertEquals(2, facturaRepository.findByIdProformaOrigenOrderByIdFactura(uno.getIdProforma()).size());
        assertEquals(1, facturaRepository.findByIdProformaOrigenOrderByIdFactura(dos.getIdProforma()).size());
        assertEquals(2, reciboRepository.findByProforma_IdProformaOrderByFechaRegistroAscIdReciboAsc(uno.getIdProforma()).size());
        assertEquals(1, reciboRepository.findByProforma_IdProformaOrderByFechaRegistroAscIdReciboAsc(dos.getIdProforma()).size());
    }

    @Test
    void losPedidosCompletadosDelPeriodoTraenLaUltimaEntregaVigenteDeLosQueTienenFechaEstimada() {
        base();
        Proforma aTiempo = pedido("1P", cliente, activo, TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.ENTREGADA);
        aTiempo.setFechaEntregaEstimada(LocalDate.of(2026, 10, 20));
        Proforma enPartes = pedido("2P", cliente, activo, TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.ENTREGADA);
        enPartes.setFechaEntregaEstimada(LocalDate.of(2026, 10, 20));
        Proforma sinFecha = pedido("3P", cliente, activo, TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.ENTREGADA);
        Proforma incompleto = pedido("4P", cliente, activo, TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.ENTREGADA_PARCIAL);
        incompleto.setFechaEntregaEstimada(LocalDate.of(2026, 10, 20));
        Proforma deOtroMes = pedido("5P", cliente, activo, TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.ENTREGADA);
        deOtroMes.setFechaEntregaEstimada(LocalDate.of(2026, 11, 20));
        em.flush();
        entrega(aTiempo, 10, LocalDate.of(2026, 10, 18), false);
        entrega(enPartes, 5, LocalDate.of(2026, 10, 10), false);
        entrega(enPartes, 5, LocalDate.of(2026, 10, 25), false);
        entrega(enPartes, 1, LocalDate.of(2026, 10, 30), true);     // anulada: no cuenta como la última
        entrega(sinFecha, 10, LocalDate.of(2026, 10, 12), false);
        entrega(incompleto, 5, LocalDate.of(2026, 10, 12), false);
        entrega(deOtroMes, 10, LocalDate.of(2026, 11, 5), false);
        em.flush();

        List<Object[]> filas = entregaRepository.pedidosCompletadosDelPeriodo(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1));

        assertEquals(2, filas.size());
        Object[] puntual = filas.stream().filter(f -> aTiempo.getIdProforma().equals(f[1])).findFirst().get();
        assertEquals(ana.getIdUsuario(), puntual[0]);
        assertEquals(LocalDate.of(2026, 10, 20), puntual[2]);
        assertEquals(LocalDate.of(2026, 10, 18), puntual[3]);
        Object[] tarde = filas.stream().filter(f -> enPartes.getIdProforma().equals(f[1])).findFirst().get();
        assertEquals(LocalDate.of(2026, 10, 25), tarde[3]);
    }

    @Test
    void lasDevolucionesPorVendedorYFechaSeAgrupanPorDia() {
        base();
        Proforma uno = pedido("1P", cliente, activo, TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.ENTREGADA);
        em.flush();
        devolucion(uno, cliente, LocalDate.of(2026, 10, 10), "10.00", "5.50");
        devolucion(uno, cliente, LocalDate.of(2026, 10, 10), "4.50");
        devolucion(uno, cliente, LocalDate.of(2026, 12, 20), "7.00");
        devolucion(uno, cliente, LocalDate.of(2027, 1, 3), "99.00");
        em.flush();

        List<Object[]> filas = devolucionRepository.devueltoPorVendedorYFecha(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1));

        assertEquals(2, filas.size());
        Object[] deOctubre = filas.stream().filter(f -> LocalDate.of(2026, 10, 10).equals(f[1])).findFirst().get();
        assertEquals(ana.getIdUsuario(), deOctubre[0]);
        assertEquals(0, new BigDecimal("20.00").compareTo((BigDecimal) deOctubre[2]));
    }
}
