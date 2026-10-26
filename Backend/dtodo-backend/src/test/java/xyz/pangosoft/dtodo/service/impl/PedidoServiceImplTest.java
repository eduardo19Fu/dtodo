package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import xyz.pangosoft.dtodo.dto.PedidoDetalleDto;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.DetalleProforma;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.ProformaEtapa;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum;
import xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import xyz.pangosoft.dtodo.repository.IProformaEtapaRepository;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;
import xyz.pangosoft.dtodo.service.IProformaAuditoriaService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

class PedidoServiceImplTest {

    private final IProformaRepository proformaRepository = mock(IProformaRepository.class);
    private final IProformaEtapaRepository etapaRepository = mock(IProformaEtapaRepository.class);
    private final IAmbitoVendedorService ambito = mock(IAmbitoVendedorService.class);
    private final IUsuarioService usuarioService = mock(IUsuarioService.class);
    private final IProformaAuditoriaService auditoria = mock(IProformaAuditoriaService.class);
    private final PedidoServiceImpl service =
            new PedidoServiceImpl(proformaRepository, etapaRepository, ambito, usuarioService, auditoria);

    @BeforeEach
    void sesion() {
        when(ambito.idUsuarioActual()).thenReturn(9);
        when(usuarioService.findById(9)).thenReturn(Usuario.builder().idUsuario(9).primerNombre("Irene").apellido("Ortega").build());
        when(auditoria.motivoObligatorio(any(), any())).thenAnswer(inv -> {
            String motivo = inv.getArgument(0);
            if (motivo == null || motivo.trim().isEmpty()) {
                throw new ReglaNegocioException("Indique el motivo");
            }
            return motivo.trim();
        });
    }

    private Proforma pedido(EstadoPedidoEnum estado) {
        Proforma proforma = new Proforma();
        proforma.setIdProforma(7L);
        proforma.setNoProforma("123P");
        proforma.setTipoProforma(TipoProformaEnum.MAYORISTA);
        proforma.setEstadoPedido(estado);
        when(proformaRepository.findById(7L)).thenReturn(Optional.of(proforma));
        return proforma;
    }

    private ProformaEtapa etapaGuardada() {
        ArgumentCaptor<ProformaEtapa> captor = ArgumentCaptor.forClass(ProformaEtapa.class);
        verify(etapaRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void bodegaAvanzaElPedidoEtapaPorEtapaYDejaQuienYCuando() {
        Proforma proforma = pedido(EstadoPedidoEnum.CONFIRMADA);

        service.avanzar(7L, "  Irene Ortega  ");
        assertEquals(EstadoPedidoEnum.EN_PREPARACION, proforma.getEstadoPedido());
        ProformaEtapa etapa = etapaGuardada();
        assertEquals(EstadoPedidoEnum.EN_PREPARACION, etapa.getEtapa());
        assertEquals("Irene Ortega", etapa.getResponsable());
        assertEquals(Integer.valueOf(9), etapa.getUsuario().getIdUsuario());
        assertTrue(etapa.getFecha() != null);

        service.avanzar(7L, null);
        assertEquals(EstadoPedidoEnum.EMPACADA, proforma.getEstadoPedido());
        service.avanzar(7L, "");
        assertEquals(EstadoPedidoEnum.EN_RUTA, proforma.getEstadoPedido());
    }

    @Test
    void unPedidoQueYaSalioARutaNoAvanzaEnBodega() {
        pedido(EstadoPedidoEnum.EN_RUTA);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> service.avanzar(7L, null));

        assertTrue(error.getMessage().contains("ya salió a ruta"));
        verify(etapaRepository, never()).save(any());
    }

    @Test
    void unPedidoEntregadoNoAvanza() {
        pedido(EstadoPedidoEnum.ENTREGADA_PARCIAL);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> service.avanzar(7L, null));

        assertTrue(error.getMessage().contains("ya se entregó"));
    }

    @Test
    void conElDescuentoPendienteORechazadoNoSePreparaElPedido() {
        Proforma proforma = pedido(EstadoPedidoEnum.CONFIRMADA);
        proforma.setEstadoAutorizacion(EstadoAutorizacionEnum.PENDIENTE);
        ReglaNegocioException pendiente = assertThrows(ReglaNegocioException.class, () -> service.avanzar(7L, null));
        assertTrue(pendiente.getMessage().contains("pendiente de autorización"));

        proforma.setEstadoAutorizacion(EstadoAutorizacionEnum.RECHAZADA);
        ReglaNegocioException rechazada = assertThrows(ReglaNegocioException.class, () -> service.avanzar(7L, null));
        assertTrue(rechazada.getMessage().contains("rechazado"));

        assertEquals(EstadoPedidoEnum.CONFIRMADA, proforma.getEstadoPedido());
        verify(etapaRepository, never()).save(any());

        proforma.setEstadoAutorizacion(EstadoAutorizacionEnum.APROBADA);
        service.avanzar(7L, null);
        assertEquals(EstadoPedidoEnum.EN_PREPARACION, proforma.getEstadoPedido());
    }

    @Test
    void elResponsableMuyLargoSeRechaza() {
        pedido(EstadoPedidoEnum.CONFIRMADA);

        assertThrows(ReglaNegocioException.class, () -> service.avanzar(7L, new String(new char[151]).replace('\0', 'a')));
    }

    @Test
    void unaProformaMinoristaOSinEtapaNoEsUnPedido() {
        Proforma minorista = pedido(EstadoPedidoEnum.CONFIRMADA);
        minorista.setTipoProforma(TipoProformaEnum.MINORISTA);
        assertThrows(NotFoundException.class, () -> service.avanzar(7L, null));
        assertThrows(NotFoundException.class, () -> service.detalle(7L));

        Proforma sinEtapa = pedido(null);
        assertThrows(NotFoundException.class, () -> service.avanzar(7L, null));

        when(proformaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.detalle(99L));
        assertNull(sinEtapa.getEstadoPedido());
    }

    @Test
    void retrocederPideMotivoYVuelveUnaEtapa() {
        Proforma proforma = pedido(EstadoPedidoEnum.EMPACADA);

        assertThrows(ReglaNegocioException.class, () -> service.retroceder(7L, " "));
        assertEquals(EstadoPedidoEnum.EMPACADA, proforma.getEstadoPedido());

        service.retroceder(7L, "faltó un producto");

        assertEquals(EstadoPedidoEnum.EN_PREPARACION, proforma.getEstadoPedido());
        ProformaEtapa etapa = etapaGuardada();
        assertEquals(EstadoPedidoEnum.EN_PREPARACION, etapa.getEtapa());
        assertTrue(etapa.getMotivo().contains("faltó un producto"));
    }

    @Test
    void noSeRetrocedeDesdeLaPrimeraEtapaNiUnPedidoEntregado() {
        pedido(EstadoPedidoEnum.CONFIRMADA);
        assertThrows(ReglaNegocioException.class, () -> service.retroceder(7L, "motivo"));

        pedido(EstadoPedidoEnum.ENTREGADA);
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> service.retroceder(7L, "motivo"));
        assertTrue(error.getMessage().contains("ya se entregó"));
    }

    @Test
    void registrarConfirmacionDejaLaPrimeraEtapaSoloEnUnaMayoristaConfirmada() {
        Proforma mayorista = pedido(EstadoPedidoEnum.CONFIRMADA);
        service.registrarConfirmacion(mayorista);
        assertEquals(EstadoPedidoEnum.CONFIRMADA, etapaGuardada().getEtapa());

        Proforma minorista = new Proforma();
        minorista.setIdProforma(8L);
        minorista.setTipoProforma(TipoProformaEnum.MINORISTA);
        service.registrarConfirmacion(minorista);
        service.registrarConfirmacion(null);
        verify(etapaRepository, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void editarUnPedidoEnPreparacionLoReiniciaYDejaConstancia() {
        Proforma proforma = pedido(EstadoPedidoEnum.EMPACADA);

        service.reiniciarSiEditado(proforma, "cambio de cantidad");

        assertEquals(EstadoPedidoEnum.CONFIRMADA, proforma.getEstadoPedido());
        ProformaEtapa etapa = etapaGuardada();
        assertEquals(EstadoPedidoEnum.CONFIRMADA, etapa.getEtapa());
        assertTrue(etapa.getMotivo().contains("EMPACADA"));
        assertTrue(etapa.getMotivo().contains("cambio de cantidad"));
    }

    @Test
    void editarUnPedidoConfirmadoEntregadoOSinEtapaNoCambiaNada() {
        for (EstadoPedidoEnum estado : new EstadoPedidoEnum[] { EstadoPedidoEnum.CONFIRMADA, EstadoPedidoEnum.ENTREGADA,
                EstadoPedidoEnum.ENTREGADA_PARCIAL, null }) {
            Proforma proforma = new Proforma();
            proforma.setEstadoPedido(estado);
            service.reiniciarSiEditado(proforma, "motivo");
            assertEquals(estado, proforma.getEstadoPedido());
        }
        service.reiniciarSiEditado(null, "motivo");
        verify(etapaRepository, never()).save(any());
    }

    @Test
    void unPedidoEntregadoNoSePuedeEditar() {
        Proforma entregado = new Proforma();
        entregado.setNoProforma("9P");
        entregado.setEstadoPedido(EstadoPedidoEnum.ENTREGADA_PARCIAL);
        assertThrows(ReglaNegocioException.class, () -> service.exigirEditable(entregado));

        Proforma enRuta = new Proforma();
        enRuta.setEstadoPedido(EstadoPedidoEnum.EN_RUTA);
        service.exigirEditable(enRuta);
        service.exigirEditable(new Proforma());
        service.exigirEditable(null);
    }

    @Test
    void laListaSinEtapaTraeLosPedidosPorEntregarYElVendedorSoloVeLosSuyos() {
        when(ambito.forzarUsuario(null)).thenReturn(5);
        when(proformaRepository.findPedidos(any(), any(), any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());

        service.listar(3, null, LocalDate.of(2026, 10, 1), null);

        verify(proformaRepository).findPedidos(eq(TipoProformaEnum.MAYORISTA), eq(EstadoPedidoEnum.POR_ENTREGAR), eq(3),
                eq(5), any(), any(), any());
    }

    @Test
    void laListaConUnaEtapaFiltraSoloPorEsa() {
        when(ambito.forzarUsuario(null)).thenReturn(null);
        when(proformaRepository.findPedidos(any(), any(), any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());

        service.listar(null, EstadoPedidoEnum.EMPACADA, null, null);

        verify(proformaRepository).findPedidos(eq(TipoProformaEnum.MAYORISTA),
                eq(Collections.singletonList(EstadoPedidoEnum.EMPACADA)), eq(null), eq(null), any(), any(), any());
    }

    @Test
    void elDetalleTraeLineasOrdenadasPorProductoSinPrecios() {
        Proforma proforma = pedido(EstadoPedidoEnum.EN_PREPARACION);
        proforma.setObservaciones("empacar aparte");
        proforma.setCliente(Cliente.builder().nombre("ANA PEREZ").nombreNegocio("LIBRERIA ELIZYOV").direccion("Zona 1")
                .contacto("WENDY").telefonoContacto("5555-0000").build());
        proforma.setUsuario(Usuario.builder().primerNombre("Bryan").apellido("Vargas").build());
        proforma.getItemsProforma().add(linea("B-2", "REGLA", 5, null));
        proforma.getItemsProforma().add(linea("A-1", "CUADERNO", 10, "10 rojos"));

        PedidoDetalleDto detalle = service.detalle(7L);

        assertEquals("LIBRERIA ELIZYOV", detalle.getNegocio());
        assertEquals("Bryan Vargas", detalle.getVendedor());
        assertEquals("empacar aparte", detalle.getObservaciones());
        assertEquals(2, detalle.getLineas().size());
        assertEquals("CUADERNO", detalle.getLineas().get(0).getProducto());
        assertEquals("10 rojos", detalle.getLineas().get(0).getObservacion());
        assertEquals("REGLA", detalle.getLineas().get(1).getProducto());
        verify(ambito).exigirProformaPropia(proforma);
        verify(etapaRepository).findHistorial(7L);
    }

    private DetalleProforma linea(String codigo, String nombre, int cantidad, String observacion) {
        DetalleProforma linea = new DetalleProforma();
        linea.setProducto(Producto.builder().codProducto(codigo).nombre(nombre).build());
        linea.setCantidad(cantidad);
        linea.setObservacion(observacion);
        return linea;
    }
}
