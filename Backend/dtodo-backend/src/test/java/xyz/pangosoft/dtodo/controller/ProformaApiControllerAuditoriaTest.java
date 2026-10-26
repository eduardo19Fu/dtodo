package xyz.pangosoft.dtodo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.enums.AccionAuditoriaEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;
import xyz.pangosoft.dtodo.service.IAutorizacionProformaService;
import xyz.pangosoft.dtodo.service.IDescuentoService;
import xyz.pangosoft.dtodo.service.IEstadoService;
import xyz.pangosoft.dtodo.service.IProformaAuditoriaService;
import xyz.pangosoft.dtodo.service.IProformaMayoristaService;
import xyz.pangosoft.dtodo.service.IProformaService;

/** El controlador de proformas pide motivo al editar o anular una mayorista y deja todo en el historial. */
class ProformaApiControllerAuditoriaTest {

    private final IProformaService proformaService = mock(IProformaService.class);
    private final IAmbitoVendedorService ambito = mock(IAmbitoVendedorService.class);
    private final IDescuentoService descuentoService = mock(IDescuentoService.class);
    private final IProformaMayoristaService mayoristaService = mock(IProformaMayoristaService.class);
    private final IProformaAuditoriaService auditoria = mock(IProformaAuditoriaService.class);
    private final IAutorizacionProformaService autorizacion = mock(IAutorizacionProformaService.class);
    private final ProformaApiController controller = new ProformaApiController(proformaService,
            mock(IEstadoService.class), ambito, descuentoService, mayoristaService, auditoria, autorizacion);

    @BeforeEach
    void motivos() {
        when(auditoria.motivoObligatorio(any(), any())).thenAnswer(inv -> {
            String motivo = inv.getArgument(0);
            if (motivo == null || motivo.trim().isEmpty()) {
                throw new ReglaNegocioException("Indique el motivo");
            }
            return motivo.trim();
        });
        when(auditoria.motivoOpcional(any())).thenAnswer(inv -> {
            String motivo = inv.getArgument(0);
            return motivo == null || motivo.trim().isEmpty() ? null : motivo.trim();
        });
        when(auditoria.resumen(any())).thenReturn("resumen");
    }

    private Proforma proforma(Long id, TipoProformaEnum tipo) {
        Proforma proforma = new Proforma();
        proforma.setIdProforma(id);
        proforma.setNoProforma("123P");
        proforma.setTipoProforma(tipo);
        return proforma;
    }

    @Test
    void crearEvaluaLaAutorizacionDespuesDeTipificarLasLineasYAudita() {
        Proforma nueva = proforma(null, TipoProformaEnum.MAYORISTA);
        when(proformaService.save(nueva, null)).thenReturn(proforma(7L, TipoProformaEnum.MAYORISTA));

        controller.create(nueva);

        InOrder orden = inOrder(mayoristaService, descuentoService, proformaService);
        orden.verify(mayoristaService).preparar(nueva);
        orden.verify(descuentoService).prepararLineas(nueva);
        orden.verify(mayoristaService).evaluarAutorizacion(nueva);
        orden.verify(proformaService).save(nueva, null);
        verify(auditoria).registrar(eq(7L), eq(AccionAuditoriaEnum.CREACION), isNull(), any());
    }

    @Test
    void editarUnaMayoristaSinMotivoSeRechazaYNoGuarda() {
        when(proformaService.findProforma(7L)).thenReturn(proforma(7L, TipoProformaEnum.MAYORISTA));

        assertThrows(ReglaNegocioException.class,
                () -> controller.udpate(proforma(7L, TipoProformaEnum.MAYORISTA), 7L, "  "));

        verify(proformaService, never()).save(any(), anyLong());
    }

    @Test
    void editarUnaMayoristaFacturadaSeRechaza() {
        Proforma existente = proforma(7L, TipoProformaEnum.MAYORISTA);
        when(proformaService.findProforma(7L)).thenReturn(existente);
        org.mockito.Mockito.doThrow(new ReglaNegocioException("ya fue facturada")).when(autorizacion)
                .exigirSinFactura(existente);

        assertThrows(ReglaNegocioException.class,
                () -> controller.udpate(proforma(7L, TipoProformaEnum.MAYORISTA), 7L, "corrección"));

        verify(proformaService, never()).save(any(), anyLong());
    }

    @Test
    void editarUnaMayoristaConMotivoGuardaYAuditaAntesYDespues() {
        Proforma existente = proforma(7L, TipoProformaEnum.MAYORISTA);
        Proforma cambios = proforma(7L, TipoProformaEnum.MAYORISTA);
        Proforma guardada = proforma(7L, TipoProformaEnum.MAYORISTA);
        when(proformaService.findProforma(7L)).thenReturn(existente);
        when(proformaService.save(cambios, 7L)).thenReturn(guardada);

        controller.udpate(cambios, 7L, " cambio de cantidad ");

        verify(mayoristaService).evaluarAutorizacion(cambios);
        verify(auditoria).registrar(eq(7L), eq(AccionAuditoriaEnum.MODIFICACION), eq("cambio de cantidad"),
                contains("Antes: resumen"));
    }

    @Test
    void editarUnaMinoristaNoExigeMotivoPeroLoGuardaSiLoTrae() {
        Proforma existente = proforma(7L, TipoProformaEnum.MINORISTA);
        Proforma cambios = proforma(7L, TipoProformaEnum.MINORISTA);
        when(proformaService.findProforma(7L)).thenReturn(existente);
        when(proformaService.save(cambios, 7L)).thenReturn(proforma(7L, TipoProformaEnum.MINORISTA));

        controller.udpate(cambios, 7L, null);

        verify(autorizacion, never()).exigirSinFactura(any());
        verify(auditoria).registrar(eq(7L), eq(AccionAuditoriaEnum.MODIFICACION), isNull(), any());
    }

    @Test
    void anularUnaMayoristaPideMotivoYQuedaAuditada() {
        Proforma proforma = proforma(7L, TipoProformaEnum.MAYORISTA);
        when(proformaService.findProforma(7L)).thenReturn(proforma);

        assertThrows(ReglaNegocioException.class, () -> controller.delete(7L, null));
        verify(proformaService, never()).delete(any());

        assertEquals(200, controller.delete(7L, "pedido duplicado").getStatusCodeValue());
        verify(proformaService).delete(proforma);
        verify(auditoria).registrar(eq(7L), eq(AccionAuditoriaEnum.ANULACION), eq("pedido duplicado"),
                contains("123P"));
    }

    @Test
    void anularUnaMinoristaNoPideMotivo() {
        when(proformaService.findProforma(8L)).thenReturn(proforma(8L, TipoProformaEnum.MINORISTA));

        assertTrue(controller.delete(8L, null).getStatusCode().is2xxSuccessful());

        verify(auditoria).registrar(eq(8L), eq(AccionAuditoriaEnum.ANULACION), isNull(), any());
    }
}
