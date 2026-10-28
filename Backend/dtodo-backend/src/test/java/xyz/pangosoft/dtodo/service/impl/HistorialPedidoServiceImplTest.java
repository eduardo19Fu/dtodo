package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.dto.HistorialEventoDto;
import xyz.pangosoft.dtodo.dto.HistorialEventoDto.Categoria;
import xyz.pangosoft.dtodo.dto.PedidoEtapaDto;
import xyz.pangosoft.dtodo.dto.ProformaAuditoriaDto;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.Factura;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.ProformaEntrega;
import xyz.pangosoft.dtodo.model.ProformaEntregaDetalle;
import xyz.pangosoft.dtodo.model.ReciboCaja;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.AccionAuditoriaEnum;
import xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import xyz.pangosoft.dtodo.repository.IFacturaRepository;
import xyz.pangosoft.dtodo.repository.IProformaAuditoriaRepository;
import xyz.pangosoft.dtodo.repository.IProformaEntregaRepository;
import xyz.pangosoft.dtodo.repository.IProformaEtapaRepository;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.repository.IReciboCajaRepository;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;

class HistorialPedidoServiceImplTest {

    private final IProformaRepository proformas = mock(IProformaRepository.class);
    private final IProformaAuditoriaRepository auditoria = mock(IProformaAuditoriaRepository.class);
    private final IProformaEtapaRepository etapas = mock(IProformaEtapaRepository.class);
    private final IProformaEntregaRepository entregas = mock(IProformaEntregaRepository.class);
    private final IFacturaRepository facturas = mock(IFacturaRepository.class);
    private final IReciboCajaRepository recibos = mock(IReciboCajaRepository.class);
    private final IAmbitoVendedorService ambito = mock(IAmbitoVendedorService.class);
    private final HistorialPedidoServiceImpl service = new HistorialPedidoServiceImpl(proformas, auditoria, etapas, entregas, facturas,
            recibos, ambito);

    private final Usuario ana = Usuario.builder().idUsuario(1).primerNombre("Ana").apellido("Lopez").build();
    private Proforma pedido;

    private static LocalDateTime hora(int dia, int h) {
        return LocalDateTime.of(2026, 10, dia, h, 0);
    }

    @BeforeEach
    void escenario() {
        pedido = new Proforma();
        pedido.setIdProforma(7L);
        pedido.setTipoProforma(TipoProformaEnum.MAYORISTA);
        pedido.setEstadoPedido(EstadoPedidoEnum.ENTREGADA);
        when(proformas.findById(7L)).thenReturn(Optional.of(pedido));
    }

    @Test
    void juntaTodoLoQueSeGuardaDelPedidoDeLoMasAntiguoALoMasReciente() {
        when(auditoria.findHistorial(7L)).thenReturn(Arrays.asList(
                new ProformaAuditoriaDto(2L, AccionAuditoriaEnum.AUTORIZACION, "Irene Ortega", "buen cliente", "Dto 20 %", hora(5, 11)),
                new ProformaAuditoriaDto(1L, AccionAuditoriaEnum.CREACION, "Ana Lopez", null, "resumen", hora(5, 9))));
        when(etapas.findHistorial(7L)).thenReturn(Arrays.asList(
                new PedidoEtapaDto(1L, EstadoPedidoEnum.CONFIRMADA, "Ana Lopez", null, null, hora(5, 9)),
                new PedidoEtapaDto(2L, EstadoPedidoEnum.EMPACADA, "Luis Perez", "Maria (bodega)", null, hora(6, 8)),
                new PedidoEtapaDto(3L, EstadoPedidoEnum.EN_RUTA, "Luis Perez", null, null, hora(6, 14))));
        ProformaEntrega entrega = ProformaEntrega.builder().idEntrega(9L).usuario(ana).fechaRegistro(hora(7, 10))
                .fechaEntrega(LocalDate.of(2026, 10, 7)).entregadoPor("Pedro piloto").observaciones("sin novedad").build();
        entrega.getDetalles().add(ProformaEntregaDetalle.builder().cantidad(4).build());
        entrega.getDetalles().add(ProformaEntregaDetalle.builder().cantidad(1).build());
        when(entregas.findByIdProformaOrderByFechaRegistroAscIdEntregaAsc(7L)).thenReturn(Collections.singletonList(entrega));
        Factura factura = new Factura();
        factura.setIdFactura(40L);
        factura.setNoFactura(1234L);
        factura.setSerie("A");
        factura.setUsuario(ana);
        factura.setEstado(Estado.builder().estado("ACTIVO").build());
        factura.setFecha(Date.from(hora(8, 9).atZone(ZoneId.systemDefault()).toInstant()));
        when(facturas.findByIdProformaOrigenOrderByIdFactura(7L)).thenReturn(Collections.singletonList(factura));
        ReciboCaja recibo = ReciboCaja.builder().idRecibo(3L).sucursal(Sucursal.builder().idSucursal(2).build()).numero(15L).usuario(ana)
                .fechaRegistro(hora(8, 15)).build();
        when(recibos.findByProforma_IdProformaOrderByFechaRegistroAscIdReciboAsc(7L)).thenReturn(Collections.singletonList(recibo));

        List<HistorialEventoDto> eventos = service.historial(7L);

        assertEquals(Arrays.asList("Pedido creado", "Etapa: Confirmado", "Descuento autorizado", "Etapa: Empacado", "Etapa: En ruta",
                "Entrega registrada", "Factura A-1234 emitida", "Recibo S2-000015 emitido"),
                eventos.stream().map(HistorialEventoDto::getTitulo).collect(Collectors.toList()));
        HistorialEventoDto empacado = eventos.get(3);
        assertEquals("Luis Perez", empacado.getUsuario());
        assertEquals("Maria (bodega)", empacado.getResponsable());
        assertEquals(Categoria.ETAPA, empacado.getCategoria());
        HistorialEventoDto autorizacion = eventos.get(2);
        assertEquals("buen cliente", autorizacion.getMotivo());
        assertEquals(Categoria.AUTORIZACION, autorizacion.getCategoria());
        HistorialEventoDto entregaEvento = eventos.get(5);
        assertEquals("5 unidades en 2 líneas · entregado el 07/10/2026 · entregó Pedro piloto", entregaEvento.getDetalle());
        assertEquals("sin novedad", entregaEvento.getMotivo());
        verify(ambito).exigirProformaPropia(pedido);
    }

    @Test
    void unaEntregaAnuladaYUnReciboAnuladoGeneranSuPropioEvento() {
        ProformaEntrega entrega = ProformaEntrega.builder().idEntrega(9L).usuario(ana).fechaRegistro(hora(7, 10)).anulada(true)
                .fechaAnulacion(hora(9, 10)).usuarioAnulacion(ana).motivoAnulacion("se devolvió todo").build();
        entrega.getDetalles().add(ProformaEntregaDetalle.builder().cantidad(1).build());
        entrega.setDescontadoAMano(true);
        when(entregas.findByIdProformaOrderByFechaRegistroAscIdEntregaAsc(7L)).thenReturn(Collections.singletonList(entrega));
        ReciboCaja recibo = ReciboCaja.builder().idRecibo(3L).sucursal(Sucursal.builder().idSucursal(2).build()).numero(15L).usuario(ana)
                .fechaRegistro(hora(8, 15)).anulado(true).fechaAnulacion(hora(8, 16)).usuarioAnulacion(ana).motivoAnulacion("monto").build();
        when(recibos.findByProforma_IdProformaOrderByFechaRegistroAscIdReciboAsc(7L)).thenReturn(Collections.singletonList(recibo));

        List<HistorialEventoDto> eventos = service.historial(7L);

        assertEquals(Arrays.asList("Entrega registrada", "Recibo S2-000015 emitido", "Recibo S2-000015 anulado", "Entrega anulada"),
                eventos.stream().map(HistorialEventoDto::getTitulo).collect(Collectors.toList()));
        assertEquals("se devolvió todo", eventos.get(3).getMotivo());
        assertEquals(Categoria.ANULACION, eventos.get(3).getCategoria());
        assertTrue(eventos.get(0).getDetalle().contains("inventario descontado a mano"));
    }

    @Test
    void unaFacturaAnuladaLoDice() {
        Factura factura = new Factura();
        factura.setIdFactura(40L);
        factura.setEstado(Estado.builder().estado("ANULADO").build());
        factura.setFecha(Date.from(hora(8, 9).atZone(ZoneId.systemDefault()).toInstant()));
        when(facturas.findByIdProformaOrigenOrderByIdFactura(7L)).thenReturn(Collections.singletonList(factura));

        HistorialEventoDto evento = service.historial(7L).get(0);

        assertEquals("Factura 40 emitida", evento.getTitulo());
        assertEquals("La factura está anulada.", evento.getDetalle());
    }

    @Test
    void cadaAccionDeAuditoriaTieneSuTituloYCategoria() {
        for (AccionAuditoriaEnum accion : AccionAuditoriaEnum.values()) {
            HistorialEventoDto evento = HistorialPedidoServiceImpl.deAuditoria(new ProformaAuditoriaDto(1L, accion, "Ana", null, null, hora(5, 9)));

            assertTrue(evento.getTitulo() != null && !evento.getTitulo().isEmpty(), accion.name());
            assertTrue(evento.getCategoria() != null, accion.name());
        }
        assertEquals(Categoria.DEVOLUCION, HistorialPedidoServiceImpl.deAuditoria(
                new ProformaAuditoriaDto(1L, AccionAuditoriaEnum.DEVOLUCION_CLIENTE, "Ana", "dañado", "x", hora(5, 9))).getCategoria());
        assertEquals("Crédito rechazado", HistorialPedidoServiceImpl.deAuditoria(
                new ProformaAuditoriaDto(1L, AccionAuditoriaEnum.RECHAZO_CREDITO, "Ana", null, null, hora(5, 9))).getTitulo());
    }

    @Test
    void elHistorialSoloSeConsultaDeUnPedidoMayoristaYDentroDelAmbito() {
        pedido.setTipoProforma(TipoProformaEnum.MINORISTA);
        assertThrows(NotFoundException.class, () -> service.historial(7L));

        pedido.setTipoProforma(TipoProformaEnum.MAYORISTA);
        pedido.setEstadoPedido(null);
        assertThrows(NotFoundException.class, () -> service.historial(7L));

        when(proformas.findById(8L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.historial(8L));

        pedido.setEstadoPedido(EstadoPedidoEnum.CONFIRMADA);
        doThrow(new NotFoundException("no existe")).when(ambito).exigirProformaPropia(pedido);
        assertThrows(NotFoundException.class, () -> service.historial(7L));
    }

    @Test
    void sinNadaRegistradoDevuelveUnaListaVacia() {
        assertTrue(service.historial(7L).isEmpty());
        assertNull(HistorialPedidoServiceImpl.deFactura(new Factura()).getUsuario());
    }
}
