package xyz.pangosoft.dtodo.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import xyz.pangosoft.dtodo.dto.ProformaDto;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.Factura;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.Usuario;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ejecuta contra H2 las consultas del listado de proformas, que unen cada proforma con la factura que se
 * generó a partir de ella (unión por entidad sobre {@code facturas.id_proforma_origen}).
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("context-test")
@Transactional
class ProformaListadoFacturacionTest {

    @Autowired
    private IProformaRepository proformaRepository;
    @Autowired
    private IFacturaRepository facturaRepository;
    @Autowired
    private EntityManager em;

    private Proforma proforma(String numero, Estado estado, Usuario usuario, Cliente cliente) {
        Proforma proforma = new Proforma();
        proforma.setNoProforma(numero);
        proforma.setTotal(new BigDecimal("100.00"));
        proforma.setEstado(estado);
        proforma.setUsuario(usuario);
        proforma.setCliente(cliente);
        em.persist(proforma);
        return proforma;
    }

    private Proforma[] escenario() {
        Estado activo = Estado.builder().estado("ACTIVO").build();
        Estado pagado = Estado.builder().estado("PAGADO").build();
        em.persist(activo);
        em.persist(pagado);
        Usuario vendedor = Usuario.builder().usuario("vend").primerNombre("Ana").apellido("Lopez").password("x").build();
        Usuario cajero = Usuario.builder().usuario("caja").primerNombre("Luis").apellido("Perez").password("x").build();
        em.persist(vendedor);
        em.persist(cajero);
        Cliente cliente = Cliente.builder().nombre("Libreria Uno").nit("4514627-6").build();
        em.persist(cliente);

        Proforma facturada = proforma("111P", activo, vendedor, cliente);
        Proforma sinFactura = proforma("222P", activo, vendedor, cliente);

        Factura factura = new Factura();
        factura.setSerie("A");
        factura.setNoFactura(55L);
        factura.setTotal(new BigDecimal("100.00"));
        factura.setIdProformaOrigen(facturada.getIdProforma());
        factura.setEstado(pagado);
        factura.setUsuario(cajero);
        factura.setCliente(cliente);
        em.persist(factura);
        em.flush();
        return new Proforma[] {facturada, sinFactura};
    }

    private static ProformaDto buscar(List<ProformaDto> lista, String numero) {
        Optional<ProformaDto> dto = lista.stream().filter(p -> numero.equals(p.getNoProforma())).findFirst();
        assertTrue(dto.isPresent(), "No apareció la proforma " + numero);
        return dto.get();
    }

    private void verificar(List<ProformaDto> lista) {
        assertEquals(2, lista.size());

        ProformaDto facturada = buscar(lista, "111P");
        assertNotNull(facturada.getIdFactura());
        assertEquals("A", facturada.getSerieFactura());
        assertEquals(55L, facturada.getNoFactura());
        assertEquals("PAGADO", facturada.getEstadoFactura());
        assertEquals("Luis Perez", facturada.getFacturadoPor());
        assertEquals("Ana Lopez", facturada.getVendedor());

        ProformaDto sinFactura = buscar(lista, "222P");
        assertNull(sinFactura.getIdFactura());
        assertNull(sinFactura.getEstadoFactura());
        assertNull(sinFactura.getFacturadoPor());
    }

    private static Date ayer() {
        return new Date(System.currentTimeMillis() - 24L * 3600 * 1000);
    }

    private static Date manana() {
        return new Date(System.currentTimeMillis() + 24L * 3600 * 1000);
    }

    @Test
    void elListadoPorFechasIndicaCualesProformasYaTienenFactura() {
        escenario();

        Page<ProformaDto> pagina = proformaRepository.findAllListadoDto(ayer(), manana(), null, PageRequest.of(0, 10));

        verificar(pagina.getContent());
        assertEquals(2, pagina.getTotalElements());
    }

    @Test
    void laBusquedaConservaElEstadoDeFacturacion() {
        escenario();

        Page<ProformaDto> pagina = proformaRepository.searchListadoDto(ayer(), manana(), null, "libreria", PageRequest.of(0, 10));

        verificar(pagina.getContent());
    }

    @Test
    void lasUltimasProformasConservanElEstadoDeFacturacion() {
        escenario();

        verificar(proformaRepository.findUltimasListadoDto(null, PageRequest.of(0, 10)));
    }

    @Test
    void elFiltroPorUsuarioSoloDevuelveLasDeEsteVendedor() {
        escenario();
        Integer idCajero = em.createQuery("select u.idUsuario from Usuario u where u.usuario = 'caja'", Integer.class).getSingleResult();

        assertFalse(proformaRepository.findUltimasListadoDto(null, PageRequest.of(0, 10)).isEmpty());
        assertTrue(proformaRepository.findUltimasListadoDto(idCajero, PageRequest.of(0, 10)).isEmpty());
    }

    private Factura factura(Proforma proforma, Estado estado, Usuario usuario, Cliente cliente, long numero) {
        Factura factura = new Factura();
        factura.setSerie("A");
        factura.setNoFactura(numero);
        factura.setTotal(new BigDecimal("100.00"));
        factura.setIdProformaOrigen(proforma.getIdProforma());
        factura.setEstado(estado);
        factura.setUsuario(usuario);
        factura.setCliente(cliente);
        em.persist(factura);
        return factura;
    }

    @Test
    void unaProformaReemitidaApareceUnaSolaVezConSuFacturaVigente() {
        Estado activo = Estado.builder().estado("ACTIVO").build();
        Estado pagado = Estado.builder().estado("PAGADO").build();
        Estado anulado = Estado.builder().estado("ANULADO").build();
        em.persist(activo);
        em.persist(pagado);
        em.persist(anulado);
        Usuario cajero = Usuario.builder().usuario("caja").primerNombre("Luis").apellido("Perez").password("x").build();
        em.persist(cajero);
        Cliente cliente = Cliente.builder().nombre("Libreria Uno").nit("4514627-6").build();
        em.persist(cliente);
        Proforma reemitida = proforma("111P", activo, cajero, cliente);
        Proforma soloAnulada = proforma("222P", activo, cajero, cliente);
        factura(reemitida, anulado, cajero, cliente, 10L);
        factura(reemitida, pagado, cajero, cliente, 11L);
        factura(soloAnulada, anulado, cajero, cliente, 12L);
        em.flush();

        List<ProformaDto> lista = proformaRepository.findAllListadoDto(ayer(), manana(), null, PageRequest.of(0, 10))
                .getContent();

        assertEquals(2, lista.size());
        ProformaDto vigente = buscar(lista, "111P");
        assertEquals(11L, vigente.getNoFactura());
        assertEquals("PAGADO", vigente.getEstadoFactura());
        ProformaDto anulada = buscar(lista, "222P");
        assertEquals(12L, anulada.getNoFactura());
        assertEquals("ANULADO", anulada.getEstadoFactura());
    }

    @Test
    void laBusquedaYLasUltimasTampocoRepitenUnaProformaReemitida() {
        Estado activo = Estado.builder().estado("ACTIVO").build();
        Estado pagado = Estado.builder().estado("PAGADO").build();
        Estado anulado = Estado.builder().estado("ANULADO").build();
        em.persist(activo);
        em.persist(pagado);
        em.persist(anulado);
        Usuario cajero = Usuario.builder().usuario("caja").primerNombre("Luis").apellido("Perez").password("x").build();
        em.persist(cajero);
        Cliente cliente = Cliente.builder().nombre("Libreria Uno").nit("4514627-6").build();
        em.persist(cliente);
        Proforma reemitida = proforma("111P", activo, cajero, cliente);
        factura(reemitida, anulado, cajero, cliente, 10L);
        factura(reemitida, pagado, cajero, cliente, 11L);
        em.flush();

        List<ProformaDto> busqueda = proformaRepository
                .searchListadoDto(ayer(), manana(), null, "libreria", PageRequest.of(0, 10)).getContent();
        List<ProformaDto> ultimas = proformaRepository.findUltimasListadoDto(null, PageRequest.of(0, 10));

        assertEquals(1, busqueda.size());
        assertEquals(11L, busqueda.get(0).getNoFactura());
        assertEquals(1, ultimas.size());
        assertEquals(11L, ultimas.get(0).getNoFactura());
    }
}
