package xyz.pangosoft.dtodo.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventarioBodegaExcelTest {

    private ByteArrayInputStream libro(String[] encabezados, Object[][] filas) throws Exception {
        try (Workbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Inventario");
            Row cabecera = hoja.createRow(0);
            for (int i = 0; i < encabezados.length; i++) {
                cabecera.createCell(i).setCellValue(encabezados[i]);
            }
            for (int i = 0; i < filas.length; i++) {
                Row fila = hoja.createRow(i + 1);
                for (int j = 0; j < filas[i].length; j++) {
                    Object valor = filas[i][j];
                    if (valor instanceof Number) {
                        fila.createCell(j).setCellValue(((Number) valor).doubleValue());
                    } else if (valor != null) {
                        fila.createCell(j).setCellValue(valor.toString());
                    }
                }
            }
            libro.write(salida);
            return new ByteArrayInputStream(salida.toByteArray());
        }
    }

    private static final String[] ENCABEZADOS = { "codigo_producto", "cantidad", "stock_minimo" };

    @Test
    void leeLasFilasValidasConSuNumeroDeFilaDeExcel() throws Exception {
        InventarioBodegaExcel.Lectura lectura = InventarioBodegaExcel.leer(
                libro(ENCABEZADOS, new Object[][] { { "ABC-1", 10, 2 }, { "XYZ-9", 5, null } }));

        assertTrue(lectura.errores().isEmpty());
        assertEquals(2, lectura.filasLeidas());
        assertEquals(2, lectura.filas().get(0).numeroFila());
        assertEquals("ABC-1", lectura.filas().get(0).codigo());
        assertEquals(10, lectura.filas().get(0).cantidad());
        assertEquals(2, lectura.filas().get(0).stockMinimo());
        assertNull(lectura.filas().get(1).stockMinimo());
    }

    @Test
    void aceptaCodigosNumericosSinConvertirlosANotacionCientifica() throws Exception {
        InventarioBodegaExcel.Lectura lectura = InventarioBodegaExcel.leer(
                libro(ENCABEZADOS, new Object[][] { { 7501234567890d, 3 } }));

        assertEquals("7501234567890", lectura.filas().get(0).codigo());
    }

    @Test
    void toleraEncabezadosConAcentosMayusculasYEspacios() throws Exception {
        InventarioBodegaExcel.Lectura lectura = InventarioBodegaExcel.leer(
                libro(new String[] { "Código", "CANTIDAD", "Stock Mínimo" }, new Object[][] { { "A", 1, 4 } }));

        assertEquals(1, lectura.filas().size());
        assertEquals(4, lectura.filas().get(0).stockMinimo());
    }

    @Test
    void laColumnaStockMinimoEsOpcional() throws Exception {
        InventarioBodegaExcel.Lectura lectura = InventarioBodegaExcel.leer(
                libro(new String[] { "codigo_producto", "cantidad" }, new Object[][] { { "A", 1 } }));

        assertEquals(1, lectura.filas().size());
        assertNull(lectura.filas().get(0).stockMinimo());
    }

    @Test
    void ignoraLasFilasCompletamenteVacias() throws Exception {
        InventarioBodegaExcel.Lectura lectura = InventarioBodegaExcel.leer(
                libro(ENCABEZADOS, new Object[][] { { "A", 1 }, { null, null }, { "B", 2 } }));

        assertEquals(2, lectura.filasLeidas());
        assertEquals(4, lectura.filas().get(1).numeroFila());
    }

    @Test
    void reportaCadaFilaConProblemasSinDetenerseEnLaPrimera() throws Exception {
        InventarioBodegaExcel.Lectura lectura = InventarioBodegaExcel.leer(libro(ENCABEZADOS, new Object[][] {
                { null, 5 },
                { "A", 0 },
                { "B", "dos" },
                { "C", 2.5 },
                { "D", -1 },
                { "E", 3, "x" },
                { "F", 3, -4 },
                { "OK", 1 } }));

        assertEquals(1, lectura.filas().size());
        assertEquals("OK", lectura.filas().get(0).codigo());
        assertEquals(7, lectura.errores().size());
        assertTrue(lectura.errores().get(0).startsWith("Fila 2:"));
        assertTrue(lectura.errores().get(6).startsWith("Fila 8:"));
    }

    @Test
    void rechazaUnArchivoSinLosEncabezadosObligatorios() throws Exception {
        BadRequestException error = assertThrows(BadRequestException.class, () -> InventarioBodegaExcel.leer(
                libro(new String[] { "foo", "bar" }, new Object[][] { { "A", 1 } })));

        assertTrue(error.getMessage().contains("codigo_producto"));
    }

    @Test
    void rechazaUnArchivoQueNoEsExcel() {
        assertThrows(BadRequestException.class,
                () -> InventarioBodegaExcel.leer(new ByteArrayInputStream("a,b,c".getBytes())));
    }

    @Test
    void rechazaUnArchivoQueSuperaElMaximoDeFilas() throws Exception {
        Object[][] filas = new Object[InventarioBodegaExcel.MAX_FILAS + 1][];
        for (int i = 0; i < filas.length; i++) {
            filas[i] = new Object[] { "C-" + i, 1 };
        }

        assertThrows(BadRequestException.class, () -> InventarioBodegaExcel.leer(libro(ENCABEZADOS, filas)));
    }

    @Test
    void laPlantillaGeneradaSePuedeLeerYTraeFilasDeEjemploQueNoExistenEnElCatalogo() {
        byte[] plantilla = InventarioBodegaExcel.generarPlantilla();

        InventarioBodegaExcel.Lectura lectura = InventarioBodegaExcel.leer(new ByteArrayInputStream(plantilla));

        assertTrue(lectura.errores().isEmpty());
        assertEquals(3, lectura.filas().size());
        assertTrue(lectura.filas().stream().allMatch(fila -> fila.codigo().startsWith("EJEMPLO-")));
    }

    @Test
    void laPlantillaIncluyeUnaHojaDeInstrucciones() throws Exception {
        try (Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(InventarioBodegaExcel.generarPlantilla()))) {
            assertEquals(2, libro.getNumberOfSheets());
            assertEquals("Inventario", libro.getSheetName(0));
            assertEquals("Instrucciones", libro.getSheetName(1));
        }
    }
}
