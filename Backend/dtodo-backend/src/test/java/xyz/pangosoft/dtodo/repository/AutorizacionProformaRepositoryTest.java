package xyz.pangosoft.dtodo.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import xyz.pangosoft.dtodo.dto.AutorizacionProformaDto;
import xyz.pangosoft.dtodo.dto.ProformaAuditoriaDto;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.DetalleProforma;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.Factura;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.ProformaAuditoria;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.AccionAuditoriaEnum;
import xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;

/** Ejecuta contra H2 las consultas de la bandeja de autorizaciones y del historial de auditoría. */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("context-test")
@Transactional
class AutorizacionProformaRepositoryTest {

    @Autowired
    private IProformaRepository proformaRepository;
    @Autowired
    private IProformaAuditoriaRepository auditoriaRepository;
    @Autowired
    private EntityManager em;

    private Usuario vendedor;
    private Cliente cliente;
    private Estado activo;

    private void base() {
        activo = Estado.builder().estado("ACTIVO").build();
        em.persist(activo);
        vendedor = Usuario.builder().usuario("vend").primerNombre("Ana").apellido("Lopez").password("x").build();
        em.persist(vendedor);
        cliente = Cliente.builder().nombre("Libreria Uno").nit("4514627-6").build();
        em.persist(cliente);
    }

    private Proforma proforma(String numero, TipoProformaEnum tipo, EstadoAutorizacionEnum autorizacion,
                              LocalDate vencimiento, float... descuentos) {
        Proforma proforma = new Proforma();
        proforma.setNoProforma(numero);
        proforma.setTotal(new BigDecimal("100.00"));
        proforma.setEstado(activo);
        proforma.setUsuario(vendedor);
        proforma.setCliente(cliente);
        proforma.setTipoProforma(tipo);
        proforma.setEstadoAutorizacion(autorizacion);
        proforma.setFechaVencimiento(vencimiento);
        for (float descuento : descuentos) {
            DetalleProforma linea = new DetalleProforma();
            linea.setCantidad(1);
            linea.setDescuento(descuento);
            proforma.getItemsProforma().add(linea);
        }
        em.persist(proforma);
        return proforma;
    }

    @Test
    void laBandejaListaSoloLasPendientesConSuDescuentoMaximo() {
        base();
        proforma("1P", TipoProformaEnum.MAYORISTA, EstadoAutorizacionEnum.PENDIENTE, null, 5f, 20f);
        proforma("2P", TipoProformaEnum.MAYORISTA, EstadoAutorizacionEnum.APROBADA, null, 20f);
        proforma("3P", TipoProformaEnum.MAYORISTA, EstadoAutorizacionEnum.RECHAZADA, null, 20f);
        proforma("4P", TipoProformaEnum.MAYORISTA, null, null, 10f);
        em.flush();

        List<AutorizacionProformaDto> pendientes = proformaRepository.findPorAutorizar(EstadoAutorizacionEnum.PENDIENTE);

        assertEquals(1, pendientes.size());
        AutorizacionProformaDto dto = pendientes.get(0);
        assertEquals("1P", dto.getNoProforma());
        assertEquals(20f, dto.getDescuentoMaximo());
        assertEquals("DESCUENTO", dto.getTipo());
        assertEquals("Libreria Uno", dto.getCliente());
        assertEquals("Ana Lopez", dto.getVendedor());
        assertNull(dto.getRuta());
    }

    @Test
    void lasVencidasExcluyenLasFacturadasLasAutorizadasLasVigentesYLasMinoristas() {
        base();
        LocalDate ayer = LocalDate.now().minusDays(1);
        proforma("1P", TipoProformaEnum.MAYORISTA, null, ayer, 5f);
        Proforma autorizada = proforma("2P", TipoProformaEnum.MAYORISTA, null, ayer, 5f);
        autorizada.setVencimientoAutorizado(true);
        Proforma facturada = proforma("3P", TipoProformaEnum.MAYORISTA, null, ayer, 5f);
        proforma("4P", TipoProformaEnum.MAYORISTA, null, LocalDate.now(), 5f);
        proforma("5P", TipoProformaEnum.MINORISTA, null, ayer, 5f);
        em.flush();
        Factura factura = new Factura();
        factura.setSerie("A");
        factura.setNoFactura(1L);
        factura.setTotal(new BigDecimal("100.00"));
        factura.setIdProformaOrigen(facturada.getIdProforma());
        factura.setEstado(activo);
        factura.setUsuario(vendedor);
        factura.setCliente(cliente);
        em.persist(factura);
        em.flush();

        List<AutorizacionProformaDto> vencidas = proformaRepository.findVencidasSinAutorizar(TipoProformaEnum.MAYORISTA,
                LocalDate.now());

        assertEquals(1, vencidas.size());
        assertEquals("1P", vencidas.get(0).getNoProforma());
        assertEquals("VENCIDA", vencidas.get(0).getTipo());
        assertEquals(ayer, vencidas.get(0).getFechaVencimiento());
    }

    @Test
    void elHistorialSeOrdenaDelMasRecienteAlMasAntiguoYTraeElNombreDelUsuario() {
        base();
        Proforma proforma = proforma("1P", TipoProformaEnum.MAYORISTA, null, null);
        em.flush();
        LocalDateTime ahora = LocalDateTime.now();
        auditoriaRepository.save(ProformaAuditoria.builder().idProforma(proforma.getIdProforma()).usuario(vendedor)
                .accion(AccionAuditoriaEnum.CREACION).fecha(ahora.minusHours(2)).build());
        auditoriaRepository.save(ProformaAuditoria.builder().idProforma(proforma.getIdProforma()).usuario(vendedor)
                .accion(AccionAuditoriaEnum.MODIFICACION).motivo("cambio de cantidad").detalle("antes y después")
                .fecha(ahora).build());
        auditoriaRepository.save(ProformaAuditoria.builder().idProforma(999L).usuario(vendedor)
                .accion(AccionAuditoriaEnum.CREACION).fecha(ahora).build());
        em.flush();

        List<ProformaAuditoriaDto> historial = auditoriaRepository.findHistorial(proforma.getIdProforma());

        assertEquals(2, historial.size());
        assertEquals(AccionAuditoriaEnum.MODIFICACION, historial.get(0).getAccion());
        assertEquals("cambio de cantidad", historial.get(0).getMotivo());
        assertEquals("Ana Lopez", historial.get(0).getUsuario());
        assertEquals(AccionAuditoriaEnum.CREACION, historial.get(1).getAccion());
        assertTrue(historial.get(0).getFecha().isAfter(historial.get(1).getFecha()));
    }
}
