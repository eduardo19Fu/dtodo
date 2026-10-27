package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.dto.AvanceComisionDto;
import xyz.pangosoft.dtodo.dto.FilaComisionDto;
import xyz.pangosoft.dtodo.dto.MetaComisionDto;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.service.IComisionService;

class ReporteComisionesServiceImplTest {

    private final IComisionService comisionService = mock(IComisionService.class);
    private final ReporteComisionesServiceImpl service = new ReporteComisionesServiceImpl(comisionService);

    private AvanceComisionDto avance(boolean liquidado) {
        FilaComisionDto ana = new FilaComisionDto();
        ana.setIdVendedor(1);
        ana.setVendedor("Ana Lopez");
        ana.setMontoEntregado(new BigDecimal("750000.00"));
        ana.setMetaAlcanzada(new BigDecimal("700000.00"));
        ana.setPorcentaje(new BigDecimal("1.00"));
        ana.setComision(new BigDecimal("7500.00"));
        ana.setSiguienteMeta(new BigDecimal("1000000.00"));
        ana.setFaltaParaSiguiente(new BigDecimal("250000.00"));
        ana.setCompletoAnio(liquidado ? Boolean.TRUE : null);
        FilaComisionDto luis = new FilaComisionDto();
        luis.setIdVendedor(2);
        luis.setVendedor("Luis Perez");
        luis.setCompletoAnio(liquidado ? Boolean.FALSE : null);

        AvanceComisionDto avance = new AvanceComisionDto();
        avance.setAnio(2026);
        avance.setAnioTerminado(true);
        avance.setLiquidado(liquidado);
        avance.setFechaLiquidacion(liquidado ? LocalDateTime.of(2027, 1, 5, 9, 0) : null);
        avance.setLiquidadoPor(liquidado ? "Irene Ortega" : null);
        avance.setMetas(Arrays.asList(new MetaComisionDto(new BigDecimal("300000.00"), new BigDecimal("0.50")),
                new MetaComisionDto(new BigDecimal("700000.00"), new BigDecimal("1.00"))));
        avance.setFilas(Arrays.asList(ana, luis));
        avance.setTotalEntregado(new BigDecimal("750000.00"));
        avance.setTotalComision(new BigDecimal("7500.00"));
        return avance;
    }

    private static String textoDe(byte[] xlsx) throws Exception {
        StringBuilder texto = new StringBuilder();
        DataFormatter formato = new DataFormatter();
        try (Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            Sheet hoja = libro.getSheetAt(0);
            for (Row fila : hoja) {
                for (Cell celda : fila) {
                    texto.append(formato.formatCellValue(celda)).append('|');
                }
                texto.append('\n');
            }
        }
        return texto.toString();
    }

    @Test
    void elPdfSeGeneraConLaPlantillaReal() {
        when(comisionService.avance(2026)).thenReturn(avance(true));

        byte[] pdf = service.comisionesDelAnio(2026, "PDF");

        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void elExcelMuestraMetasFilasYTotales() throws Exception {
        when(comisionService.avance(2026)).thenReturn(avance(true));

        String texto = textoDe(service.comisionesDelAnio(2026, "XLSX"));

        assertTrue(texto.contains("Comisiones de vendedores 2026"), texto);
        assertTrue(texto.contains("Año liquidado el 05/01/2027 por Irene Ortega"), texto);
        assertTrue(texto.contains("Q300,000 → 0.5 %"), texto);
        assertTrue(texto.contains("Ana Lopez"), texto);
        assertTrue(texto.contains("7,500.00"), texto);
        assertTrue(texto.contains("Sí"), texto);
        assertTrue(texto.contains("No"), texto);
    }

    @Test
    void sinLiquidarLaDecisionQuedaSinDefinir() throws Exception {
        when(comisionService.avance(2026)).thenReturn(avance(false));

        String texto = textoDe(service.comisionesDelAnio(2026, "XLSX"));

        assertTrue(texto.contains("Año terminado, pendiente de liquidar"), texto);
        assertTrue(texto.contains("Sin definir"), texto);
    }

    @Test
    void unAnioSinVendedoresMuestraElAviso() throws Exception {
        AvanceComisionDto vacio = new AvanceComisionDto();
        vacio.setAnio(2026);
        vacio.setFilas(Collections.emptyList());
        when(comisionService.avance(2026)).thenReturn(vacio);

        String texto = textoDe(service.comisionesDelAnio(2026, "XLSX"));

        assertTrue(texto.contains("No hay vendedores para este año."), texto);
        assertTrue(texto.contains("sin metas definidas"), texto);
    }

    @Test
    void elFormatoInvalidoSeRechaza() {
        assertThrows(BadRequestException.class, () -> service.comisionesDelAnio(2026, "CSV"));
    }
}
