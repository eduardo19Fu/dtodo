package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.repository.IRutaRepository;

/** Los reportes de mayoristas validan sus filtros antes de tocar la base de datos. */
class ReporteMayoristasServiceImplTest {

    private final DataSource dataSource = mock(DataSource.class);
    private final ReporteMayoristasServiceImpl service =
            new ReporteMayoristasServiceImpl(dataSource, mock(IRutaRepository.class));

    @Test
    void elFormatoPorOmisionEsPdfYSoloSeAceptanPdfYXlsx() {
        assertEquals("PDF", ReporteMayoristasServiceImpl.validarFormato(null));
        assertEquals("XLSX", ReporteMayoristasServiceImpl.validarFormato("xlsx"));
        assertThrows(BadRequestException.class, () -> ReporteMayoristasServiceImpl.validarFormato("CSV"));
    }

    @Test
    void elPeriodoDebeEstarCompletoSerValidoYEnOrden() {
        LocalDate[] rango = ReporteMayoristasServiceImpl.validarRango("2026-10-01", "2026-10-31");
        assertEquals(LocalDate.of(2026, 10, 1), rango[0]);
        assertEquals(LocalDate.of(2026, 10, 31), rango[1]);

        assertThrows(BadRequestException.class, () -> ReporteMayoristasServiceImpl.validarRango(null, "2026-10-31"));
        assertThrows(BadRequestException.class, () -> ReporteMayoristasServiceImpl.validarRango(" ", " "));
        assertThrows(BadRequestException.class, () -> ReporteMayoristasServiceImpl.validarRango("01/10/2026", "2026-10-31"));
        assertThrows(BadRequestException.class, () -> ReporteMayoristasServiceImpl.validarRango("2026-10-31", "2026-10-01"));
    }

    @Test
    void laRutaNoPuedeSerCeroNiNegativaPeroPuedeFaltar() {
        ReporteMayoristasServiceImpl.validarRuta(null);
        ReporteMayoristasServiceImpl.validarRuta(3);
        assertThrows(BadRequestException.class, () -> ReporteMayoristasServiceImpl.validarRuta(0));
        assertThrows(BadRequestException.class, () -> ReporteMayoristasServiceImpl.validarRuta(-1));
    }

    @Test
    void unFiltroInvalidoNuncaAbreUnaConexion() {
        assertThrows(BadRequestException.class, () -> service.clientesPorRuta(1, "CSV"));
        assertThrows(BadRequestException.class, () -> service.clientesSinCompras(1, "2026-10-31", "2026-10-01", "PDF"));
        assertThrows(BadRequestException.class, () -> service.proformasPorVendedor(-5, "2026-10-01", "2026-10-31", "PDF"));

        verifyNoInteractions(dataSource);
    }
}
