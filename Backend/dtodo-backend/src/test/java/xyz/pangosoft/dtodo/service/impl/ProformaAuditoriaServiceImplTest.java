package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.DetalleProforma;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.enums.AccionAuditoriaEnum;
import xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import xyz.pangosoft.dtodo.repository.IProformaAuditoriaRepository;
import xyz.pangosoft.dtodo.service.IUsuarioService;

class ProformaAuditoriaServiceImplTest {

    private final IProformaAuditoriaRepository repository = mock(IProformaAuditoriaRepository.class);
    private final ProformaAuditoriaServiceImpl service = new ProformaAuditoriaServiceImpl(repository, mock(IUsuarioService.class));

    @Test
    void elMotivoSeLimpiaYUnoVacioEsNuloEnElOpcional() {
        assertEquals("cambio de cantidad", service.motivoOpcional("  cambio de cantidad "));
        assertNull(service.motivoOpcional("   "));
        assertNull(service.motivoOpcional(null));
    }

    @Test
    void elMotivoObligatorioRechazaVacioYMuyLargo() {
        ReglaNegocioException vacio = assertThrows(ReglaNegocioException.class,
                () -> service.motivoObligatorio(" ", "editar una proforma mayorista"));
        assertTrue(vacio.getMessage().contains("editar una proforma mayorista"));

        assertThrows(ReglaNegocioException.class,
                () -> service.motivoObligatorio(new String(new char[501]).replace('\0', 'a'), "anular"));
        assertEquals("ok", service.motivoObligatorio(" ok ", "anular"));
    }

    @Test
    void elResumenDescribeTipoClienteLineasTotalDescuentoYAutorizacion() {
        Proforma proforma = new Proforma();
        proforma.setTipoProforma(TipoProformaEnum.MAYORISTA);
        proforma.setCliente(Cliente.builder().nombre(" Libreria Uno ").build());
        proforma.setTotal(new BigDecimal("850.5"));
        proforma.setEstadoAutorizacion(EstadoAutorizacionEnum.PENDIENTE);
        DetalleProforma a = new DetalleProforma();
        a.setDescuento(5f);
        DetalleProforma b = new DetalleProforma();
        b.setDescuento(20f);
        proforma.getItemsProforma().add(a);
        proforma.getItemsProforma().add(b);

        assertEquals("Tipo MAYORISTA; cliente Libreria Uno; 2 líneas; total Q850.50; descuento máximo 20.00 %; "
                + "autorización PENDIENTE", service.resumen(proforma));
    }

    @Test
    void elResumenAguantaUnaProformaIncompleta() {
        assertEquals("", service.resumen(null));
        assertEquals("Tipo MINORISTA; cliente sin cliente; 0 líneas; total Q0.00; descuento máximo 0.00 %",
                service.resumen(new Proforma()));
    }

    @Test
    void sinUsuarioAutenticadoNoSeGuardaNiFalla() {
        service.registrar(5L, AccionAuditoriaEnum.CREACION, null, "detalle");

        verifyNoInteractions(repository);
    }
}
