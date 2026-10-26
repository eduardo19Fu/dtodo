package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.Factura;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.enums.AccionAuditoriaEnum;
import xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import xyz.pangosoft.dtodo.repository.IFacturaRepository;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.service.IProformaAuditoriaService;

class AutorizacionProformaServiceImplTest {

    private final IProformaRepository proformaRepository = mock(IProformaRepository.class);
    private final IFacturaRepository facturaRepository = mock(IFacturaRepository.class);
    private final IProformaAuditoriaService auditoria = mock(IProformaAuditoriaService.class);
    private final AutorizacionProformaServiceImpl service =
            new AutorizacionProformaServiceImpl(proformaRepository, facturaRepository, auditoria);

    @BeforeEach
    void motivos() {
        when(auditoria.motivoObligatorio(any(), any())).thenAnswer(inv -> {
            String motivo = inv.getArgument(0);
            if (motivo == null || motivo.trim().isEmpty()) {
                throw new ReglaNegocioException("Indique el motivo");
            }
            return motivo.trim();
        });
        when(auditoria.resumen(any())).thenReturn("resumen");
    }

    private Proforma proforma(EstadoAutorizacionEnum estado) {
        Proforma proforma = new Proforma();
        proforma.setIdProforma(7L);
        proforma.setNoProforma("123P");
        proforma.setTipoProforma(TipoProformaEnum.MAYORISTA);
        proforma.setEstadoAutorizacion(estado);
        when(proformaRepository.findById(7L)).thenReturn(Optional.of(proforma));
        return proforma;
    }

    @Test
    void aprobarCambiaElEstadoYQuedaEnLaAuditoriaConSuMotivo() {
        Proforma proforma = proforma(EstadoAutorizacionEnum.PENDIENTE);

        service.aprobar(7L, "  cliente de volumen  ");

        assertEquals(EstadoAutorizacionEnum.APROBADA, proforma.getEstadoAutorizacion());
        verify(proformaRepository).save(proforma);
        verify(auditoria).registrar(eq(7L), eq(AccionAuditoriaEnum.AUTORIZACION), eq("cliente de volumen"), any());
    }

    @Test
    void rechazarCambiaElEstadoYQuedaEnLaAuditoria() {
        Proforma proforma = proforma(EstadoAutorizacionEnum.PENDIENTE);

        service.rechazar(7L, "el margen no alcanza");

        assertEquals(EstadoAutorizacionEnum.RECHAZADA, proforma.getEstadoAutorizacion());
        verify(auditoria).registrar(eq(7L), eq(AccionAuditoriaEnum.RECHAZO), eq("el margen no alcanza"), any());
    }

    @Test
    void sinMotivoNoSeAprueba() {
        Proforma proforma = proforma(EstadoAutorizacionEnum.PENDIENTE);

        assertThrows(ReglaNegocioException.class, () -> service.aprobar(7L, "  "));

        assertEquals(EstadoAutorizacionEnum.PENDIENTE, proforma.getEstadoAutorizacion());
        verify(proformaRepository, never()).save(any());
    }

    @Test
    void soloSeResuelveUnaProformaPendiente() {
        proforma(EstadoAutorizacionEnum.APROBADA);
        assertThrows(ReglaNegocioException.class, () -> service.rechazar(7L, "motivo"));

        proforma(null);
        assertThrows(ReglaNegocioException.class, () -> service.aprobar(7L, "motivo"));
    }

    @Test
    void unaProformaFacturadaNoSeResuelve() {
        proforma(EstadoAutorizacionEnum.PENDIENTE);
        when(facturaRepository.findFirstByIdProformaOrigen(7L)).thenReturn(Optional.of(new Factura()));

        assertThrows(ReglaNegocioException.class, () -> service.aprobar(7L, "motivo"));
    }

    @Test
    void siLaProformaNoExisteEsNotFound() {
        when(proformaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.aprobar(99L, "motivo"));
    }

    @Test
    void noSeFacturaUnaPendienteNiUnaRechazada() {
        proforma(EstadoAutorizacionEnum.PENDIENTE);
        ReglaNegocioException pendiente = assertThrows(ReglaNegocioException.class, () -> service.exigirFacturable(7L));
        assertTrue(pendiente.getMessage().contains("pendiente de autorización"));

        proforma(EstadoAutorizacionEnum.RECHAZADA);
        ReglaNegocioException rechazada = assertThrows(ReglaNegocioException.class, () -> service.exigirFacturable(7L));
        assertTrue(rechazada.getMessage().contains("rechazado"));
    }

    @Test
    void seFacturaUnaAprobadaUnaSinAutorizacionYUnaVentaDirecta() {
        proforma(EstadoAutorizacionEnum.APROBADA);
        service.exigirFacturable(7L);

        proforma(null);
        service.exigirFacturable(7L);

        service.exigirFacturable(null);
        when(proformaRepository.findById(404L)).thenReturn(Optional.empty());
        service.exigirFacturable(404L);
    }

    @Test
    void unaVencidaSinAutorizarNoSeFacturaPeroAutorizadaSi() {
        Proforma proforma = proforma(null);
        proforma.setFechaVencimiento(LocalDate.now().minusDays(1));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> service.exigirFacturable(7L));
        assertTrue(error.getMessage().contains("venció"));

        proforma.setVencimientoAutorizado(true);
        service.exigirFacturable(7L);
    }

    @Test
    void unaProformaQueVenceHoyTodaviaSePuedeFacturar() {
        Proforma proforma = proforma(null);
        proforma.setFechaVencimiento(LocalDate.now());

        service.exigirFacturable(7L);
    }

    @Test
    void autorizarVencidaMarcaLaProformaYAudita() {
        Proforma proforma = proforma(null);
        proforma.setFechaVencimiento(LocalDate.now().minusDays(3));

        service.autorizarVencida(7L, "el cliente pagó el depósito");

        assertTrue(proforma.isVencimientoAutorizado());
        verify(proformaRepository).save(proforma);
        verify(auditoria).registrar(eq(7L), eq(AccionAuditoriaEnum.AUTORIZACION_VENCIDA),
                eq("el cliente pagó el depósito"), any());
    }

    @Test
    void autorizarVencidaRequiereUnaProformaVencidaQueNoLoEstabaYMotivo() {
        Proforma vigente = proforma(null);
        vigente.setFechaVencimiento(LocalDate.now().plusDays(2));
        assertThrows(ReglaNegocioException.class, () -> service.autorizarVencida(7L, "motivo"));

        Proforma yaAutorizada = proforma(null);
        yaAutorizada.setFechaVencimiento(LocalDate.now().minusDays(2));
        yaAutorizada.setVencimientoAutorizado(true);
        assertThrows(ReglaNegocioException.class, () -> service.autorizarVencida(7L, "motivo"));

        Proforma sinMotivo = proforma(null);
        sinMotivo.setFechaVencimiento(LocalDate.now().minusDays(2));
        assertThrows(ReglaNegocioException.class, () -> service.autorizarVencida(7L, ""));
        verify(auditoria, never()).registrar(any(), eq(AccionAuditoriaEnum.AUTORIZACION_VENCIDA), any(), any());
    }

    @Test
    void exigirSinFacturaRechazaLaProformaYaFacturada() {
        Proforma proforma = proforma(null);
        when(facturaRepository.findFirstByIdProformaOrigen(7L)).thenReturn(Optional.of(new Factura()));

        assertThrows(ReglaNegocioException.class, () -> service.exigirSinFactura(proforma));

        when(facturaRepository.findFirstByIdProformaOrigen(7L)).thenReturn(Optional.empty());
        service.exigirSinFactura(proforma);
        service.exigirSinFactura(null);
    }
}
