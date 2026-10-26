package xyz.pangosoft.dtodo.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import xyz.pangosoft.dtodo.dto.PedidoDto;
import xyz.pangosoft.dtodo.dto.PedidoEtapaDto;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.DetalleProforma;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.ProformaEtapa;
import xyz.pangosoft.dtodo.model.Ruta;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;

/** Ejecuta contra H2 la consulta de la lista de pedidos y el historial de etapas. */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("context-test")
@Transactional
class PedidoRepositoryTest {

    private static final Date DESDE = new Date(0L);
    private static final Date HASTA = new Date(4102444800000L);

    @Autowired
    private IProformaRepository proformaRepository;
    @Autowired
    private IProformaEtapaRepository etapaRepository;
    @Autowired
    private EntityManager em;

    private Estado activo;
    private Usuario ana;
    private Usuario luis;
    private Cliente cliente;
    private Ruta ruta1;
    private Ruta ruta2;

    private void base() {
        activo = Estado.builder().estado("ACTIVO").build();
        em.persist(activo);
        ana = Usuario.builder().usuario("ana").primerNombre("Ana").apellido("Lopez").password("x").build();
        luis = Usuario.builder().usuario("luis").primerNombre("Luis").apellido("Perez").password("x").build();
        em.persist(ana);
        em.persist(luis);
        cliente = Cliente.builder().nombre("ANA PEREZ").nombreNegocio("LIBRERIA ELIZYOV").nit("4514627-6").build();
        em.persist(cliente);
        Sucursal central = Sucursal.builder().nombre("Central").direccion("Jalapa").codigoEstablecimientoSat(1)
                .esPrincipal(true).estado(activo).build();
        em.persist(central);
        ruta1 = Ruta.builder().nombre("RUTA 1").sucursal(central).estado(activo).build();
        ruta2 = Ruta.builder().nombre("RUTA 2").sucursal(central).estado(activo).build();
        em.persist(ruta1);
        em.persist(ruta2);
    }

    private Proforma pedido(String numero, TipoProformaEnum tipo, EstadoPedidoEnum estado, Usuario vendedor, Ruta ruta,
                            int... cantidades) {
        Proforma proforma = new Proforma();
        proforma.setNoProforma(numero);
        proforma.setTotal(new BigDecimal("100.00"));
        proforma.setEstado(activo);
        proforma.setUsuario(vendedor);
        proforma.setCliente(cliente);
        proforma.setTipoProforma(tipo);
        proforma.setEstadoPedido(estado);
        proforma.setRuta(ruta);
        for (int cantidad : cantidades) {
            DetalleProforma linea = new DetalleProforma();
            linea.setCantidad(cantidad);
            proforma.getItemsProforma().add(linea);
        }
        em.persist(proforma);
        return proforma;
    }

    private List<PedidoDto> pedidos(List<EstadoPedidoEnum> estados, Integer idRuta, Integer idUsuario) {
        return proformaRepository.findPedidos(TipoProformaEnum.MAYORISTA, estados, idRuta, idUsuario, DESDE, HASTA,
                PageRequest.of(0, 50));
    }

    private static List<String> numeros(List<PedidoDto> lista) {
        return lista.stream().map(PedidoDto::getNoProforma).collect(Collectors.toList());
    }

    @Test
    void laListaTraeSoloPedidosMayoristasPorEntregarConSusUnidades() {
        base();
        pedido("1P", TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.CONFIRMADA, ana, ruta1, 10, 5);
        pedido("2P", TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.ENTREGADA, ana, ruta1, 3);
        pedido("3P", TipoProformaEnum.MINORISTA, null, ana, null, 3);
        pedido("4P", TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.ENTREGADA_PARCIAL, ana, ruta1, 2);
        em.flush();

        List<PedidoDto> lista = pedidos(EstadoPedidoEnum.POR_ENTREGAR, null, null);

        assertEquals(Arrays.asList("1P", "4P"), numeros(lista).stream().sorted().collect(Collectors.toList()));
        PedidoDto primero = lista.stream().filter(p -> "1P".equals(p.getNoProforma())).findFirst().get();
        assertEquals(15L, primero.getUnidades());
        assertEquals("RUTA 1", primero.getRuta());
        assertEquals("LIBRERIA ELIZYOV", primero.getNegocio());
        assertEquals("Ana Lopez", primero.getVendedor());
        assertEquals(EstadoPedidoEnum.CONFIRMADA, primero.getEstadoPedido());
    }

    @Test
    void laListaFiltraPorRutaPorEtapaYPorVendedor() {
        base();
        pedido("1P", TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.EMPACADA, ana, ruta1, 1);
        pedido("2P", TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.EMPACADA, luis, ruta2, 1);
        pedido("3P", TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.EN_RUTA, ana, ruta2, 1);
        em.flush();

        assertEquals(Arrays.asList("2P"), numeros(pedidos(Collections.singletonList(EstadoPedidoEnum.EMPACADA),
                ruta2.getIdRuta(), null)));
        assertEquals(Arrays.asList("3P"), numeros(pedidos(Collections.singletonList(EstadoPedidoEnum.EN_RUTA), null, null)));
        assertEquals(Arrays.asList("1P"), numeros(pedidos(EstadoPedidoEnum.POR_ENTREGAR, null, ana.getIdUsuario()))
                .stream().filter("1P"::equals).collect(Collectors.toList()));
        assertEquals(2, pedidos(EstadoPedidoEnum.POR_ENTREGAR, null, ana.getIdUsuario()).size());
    }

    @Test
    void laListaFiltraPorFechaDeEmision() {
        base();
        pedido("1P", TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.CONFIRMADA, ana, ruta1, 1);
        em.flush();
        Date manana = new Date(System.currentTimeMillis() + 24L * 3600 * 1000);
        Date pasado = new Date(System.currentTimeMillis() + 48L * 3600 * 1000);

        List<PedidoDto> futuros = proformaRepository.findPedidos(TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.POR_ENTREGAR,
                null, null, manana, pasado, PageRequest.of(0, 50));

        assertTrue(futuros.isEmpty());
    }

    @Test
    void elHistorialDeEtapasVaDeLaMasAntiguaALaMasRecienteConElNombreDelUsuario() {
        base();
        Proforma proforma = pedido("1P", TipoProformaEnum.MAYORISTA, EstadoPedidoEnum.EN_PREPARACION, ana, ruta1, 1);
        em.flush();
        LocalDateTime ahora = LocalDateTime.now();
        etapaRepository.save(ProformaEtapa.builder().idProforma(proforma.getIdProforma()).etapa(EstadoPedidoEnum.EN_PREPARACION)
                .usuario(luis).responsable("Irene Ortega").fecha(ahora).build());
        etapaRepository.save(ProformaEtapa.builder().idProforma(proforma.getIdProforma()).etapa(EstadoPedidoEnum.CONFIRMADA)
                .usuario(ana).fecha(ahora.minusHours(2)).build());
        etapaRepository.save(ProformaEtapa.builder().idProforma(999L).etapa(EstadoPedidoEnum.CONFIRMADA)
                .usuario(ana).fecha(ahora).build());
        em.flush();

        List<PedidoEtapaDto> historial = etapaRepository.findHistorial(proforma.getIdProforma());

        assertEquals(2, historial.size());
        assertEquals(EstadoPedidoEnum.CONFIRMADA, historial.get(0).getEtapa());
        assertEquals("Ana Lopez", historial.get(0).getUsuario());
        assertEquals(EstadoPedidoEnum.EN_PREPARACION, historial.get(1).getEtapa());
        assertEquals("Irene Ortega", historial.get(1).getResponsable());
    }
}
