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
}
