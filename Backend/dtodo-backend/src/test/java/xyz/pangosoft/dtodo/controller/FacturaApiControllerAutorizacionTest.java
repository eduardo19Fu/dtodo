package xyz.pangosoft.dtodo.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.Factura;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.enums.AccionAuditoriaEnum;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;
import xyz.pangosoft.dtodo.service.IAutorizacionProformaService;
import xyz.pangosoft.dtodo.service.IFacturaService;
import xyz.pangosoft.dtodo.service.IProformaAuditoriaService;
import xyz.pangosoft.dtodo.service.IProformaService;

/** Al facturar desde una proforma se exige que esté autorizada y se audita el cambio de cliente. */
class FacturaApiControllerAutorizacionTest {

    private final IFacturaService facturaService = mock(IFacturaService.class);
    private final IAutorizacionProformaService autorizacion = mock(IAutorizacionProformaService.class);
    private final IProformaAuditoriaService auditoria = mock(IProformaAuditoriaService.class);
    private final IProformaService proformaService = mock(IProformaService.class);
    private final FacturaApiController controller = new FacturaApiController(facturaService,
            mock(IAmbitoVendedorService.class), autorizacion, auditoria, proformaService);

    private Cliente cliente(int id, String nombre) {
        return Cliente.builder().idCliente(id).nombre(nombre).nit("1234567-8").build();
    }

    private Factura factura(Cliente cliente) {
        Factura factura = new Factura();
        factura.setIdProformaOrigen(7L);
        factura.setSerie("A");
        factura.setNoFactura(55L);
        factura.setCliente(cliente);
        return factura;
    }

    private Proforma proformaDe(Cliente cliente) {
        Proforma proforma = new Proforma();
        proforma.setIdProforma(7L);
        proforma.setCliente(cliente);
        when(proformaService.findProforma(7L)).thenReturn(proforma);
        return proforma;
    }

    @Test
    void unaProformaPendienteNoLlegaANingunaFactura() {
        Factura factura = factura(cliente(1, "Uno"));
        doThrow(new ReglaNegocioException("pendiente de autorización")).when(autorizacion).exigirFacturable(7L);

        assertThrows(ReglaNegocioException.class, () -> controller.createV2(factura));

        verify(facturaService, never()).facturaFel(any());
    }

    @Test
    void facturarAOtroClienteQuedaEnElHistorialDeLaProforma() {
        proformaDe(cliente(1, "Libreria Uno"));
        Factura factura = factura(cliente(2, "Papeleria Dos"));
        when(facturaService.facturaFel(factura)).thenReturn(factura);

        controller.createV2(factura);

        verify(auditoria).registrar(eq(7L), eq(AccionAuditoriaEnum.CAMBIO_CLIENTE_FACTURA), isNull(),
                contains("Papeleria Dos"));
    }

    @Test
    void facturarAlMismoClienteNoAuditaNada() {
        proformaDe(cliente(1, "Libreria Uno"));
        Factura factura = factura(cliente(1, "Libreria Uno"));
        when(facturaService.facturaFel(factura)).thenReturn(factura);

        controller.createV2(factura);

        verify(auditoria, never()).registrar(any(), any(), any(), any());
    }

    @Test
    void unFalloAlAuditarNoOcultaLaFacturaYaCertificada() {
        proformaDe(cliente(1, "Libreria Uno"));
        Factura factura = factura(cliente(2, "Papeleria Dos"));
        when(facturaService.facturaFel(factura)).thenReturn(factura);
        doThrow(new IllegalStateException("BD caída")).when(auditoria).registrar(any(), any(), any(), any());

        controller.createV2(factura);

        verify(facturaService).facturaFel(factura);
    }
}
