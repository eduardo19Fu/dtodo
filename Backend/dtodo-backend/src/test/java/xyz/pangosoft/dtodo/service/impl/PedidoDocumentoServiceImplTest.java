package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.repository.IRutaRepository;

/** Los documentos del pedido se validan antes de tocar la base de datos. */
class PedidoDocumentoServiceImplTest {

    private final DataSource dataSource = mock(DataSource.class);
    private final IProformaRepository proformaRepository = mock(IProformaRepository.class);
    private final IRutaRepository rutaRepository = mock(IRutaRepository.class);
    private final PedidoDocumentoServiceImpl service =
            new PedidoDocumentoServiceImpl(dataSource, proformaRepository, rutaRepository);

    @Test
    void laFechaVaciaEsHoyYUnaMalEscritaSeRechaza() {
        assertEquals(LocalDate.now(), PedidoDocumentoServiceImpl.parsearFecha(null));
        assertEquals(LocalDate.now(), PedidoDocumentoServiceImpl.parsearFecha(" "));
        assertEquals(LocalDate.of(2026, 10, 28), PedidoDocumentoServiceImpl.parsearFecha("2026-10-28"));
        assertThrows(BadRequestException.class, () -> PedidoDocumentoServiceImpl.parsearFecha("28/10/2026"));
    }

    @Test
    void laNotaDeEnvioExigeUnaRutaQueExista() {
        assertThrows(BadRequestException.class, () -> service.notaEnvio(null, "2026-10-28"));
        assertThrows(BadRequestException.class, () -> service.notaEnvio(0, "2026-10-28"));
        when(rutaRepository.findById(9)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.notaEnvio(9, "2026-10-28"));

        verifyNoInteractions(dataSource);
    }

    @Test
    void elPickingYElEmpaqueSoloSeImprimenDeUnPedidoMayorista() {
        when(proformaRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.picking(404L));

        Proforma minorista = new Proforma();
        minorista.setTipoProforma(TipoProformaEnum.MINORISTA);
        when(proformaRepository.findById(5L)).thenReturn(Optional.of(minorista));
        assertThrows(NotFoundException.class, () -> service.empaque(5L));

        Proforma sinEtapa = new Proforma();
        sinEtapa.setTipoProforma(TipoProformaEnum.MAYORISTA);
        when(proformaRepository.findById(6L)).thenReturn(Optional.of(sinEtapa));
        assertThrows(NotFoundException.class, () -> service.picking(6L));

        verifyNoInteractions(dataSource);
    }
}
