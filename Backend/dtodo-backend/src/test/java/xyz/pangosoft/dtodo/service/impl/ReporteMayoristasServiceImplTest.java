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
            new ReporteMayoristasServiceImpl(dataSource, mock(IRutaRepository.class),
                    mock(xyz.pangosoft.dtodo.repository.IUsuarioRepository.class));

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

        assertThrows(BadRequestException.class, () -> service.entregadoPorRutaYVendedor(null, 0, "2026-10-01", "2026-10-31", "PDF"));
        assertThrows(BadRequestException.class, () -> service.entregadoPorRutaYVendedor(null, null, null, "2026-10-31", "PDF"));
        assertThrows(BadRequestException.class, () -> service.entregadoPorRutaYVendedor(null, null, "2026-10-01", "2026-10-31", "CSV"));

        verifyNoInteractions(dataSource);
    }

    @Test
    void elExcelDeProformasPorVendedorEsUnaTablaConNumerosYTotales() throws Exception {
        java.util.Map<String, Object> fila = new java.util.HashMap<>();
        fila.put("vendedor", "Bryan Vargas");
        fila.put("generadas", 10L);
        fila.put("valor_proformado", new java.math.BigDecimal("5000.00"));
        fila.put("facturadas", 4L);
        fila.put("valor_facturado", new java.math.BigDecimal("2500.00"));
        fila.put("por_autorizar", 1L);
        fila.put("tasa_conversion", new java.math.BigDecimal("0.4"));
        java.util.Map<String, Object> parametros = new java.util.HashMap<>();
        parametros.put("RUTA", "Todas las rutas");
        parametros.put("RANGO", "Del 01/10/2026 al 31/10/2026");

        byte[] xlsx = ReporteMayoristasServiceImpl.excelDe(ReporteMayoristasServiceImpl.PLANTILLA_PROFORMAS_VENDEDOR,
                java.util.Collections.singletonList(fila), parametros, "Proformas por vendedor");

        try (org.apache.poi.ss.usermodel.Workbook libro = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(xlsx))) {
            org.apache.poi.ss.usermodel.Sheet hoja = libro.getSheetAt(0);
            org.apache.poi.ss.usermodel.Row datos = hoja.getRow(5);
            assertEquals("Bryan Vargas", datos.getCell(0).getStringCellValue());
            assertEquals(10.0, datos.getCell(1).getNumericCellValue());
            assertEquals(5000.0, datos.getCell(2).getNumericCellValue());
            assertEquals(0.4, datos.getCell(6).getNumericCellValue(), 0.0001);
            assertEquals(10.0, hoja.getRow(6).getCell(1).getNumericCellValue());
        }
    }
}
