package xyz.pangosoft.dtodo.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

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

        // "A" con cantidad 0 es válida: registra el producto sin existencias
        assertEquals(2, lectura.filas().size());
        assertEquals("A", lectura.filas().get(0).codigo());
        assertEquals(0, lectura.filas().get(0).cantidad());
        assertEquals("OK", lectura.filas().get(1).codigo());
        assertEquals(6, lectura.errores().size());
        assertTrue(lectura.errores().get(0).startsWith("Fila 2:"));
        assertTrue(lectura.errores().get(5).startsWith("Fila 8:"));
    }

    @Test
    void aceptaCantidadCeroParaRegistrarProductosSinExistencias() throws Exception {
        InventarioBodegaExcel.Lectura lectura = InventarioBodegaExcel.leer(
                libro(ENCABEZADOS, new Object[][] { { "SIN-STOCK", 0, 3 }, { "OTRO", 5 } }));

        assertTrue(lectura.errores().isEmpty());
        assertEquals(0, lectura.filas().get(0).cantidad());
        assertEquals(3, lectura.filas().get(0).stockMinimo());
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
        assertEquals(6, lectura.filas().size());
        assertTrue(lectura.filas().stream().allMatch(fila -> fila.codigo().startsWith("EJEMPLO-") || fila.codigo().startsWith("NUEVO-")));
        assertTrue(lectura.filas().stream().anyMatch(fila -> fila.cantidad() == 0));
    }

    @Test
    void laPlantillaMuestraUnProductoNuevoConTodosSusDatosYOtroConMarcaYCategoriaVacias() {
        InventarioBodegaExcel.Lectura lectura = InventarioBodegaExcel.leer(
                new ByteArrayInputStream(InventarioBodegaExcel.generarPlantilla()));

        InventarioBodegaExcel.Fila completo = lectura.filas().get(4);
        assertEquals("NUEVO-001", completo.codigo());
        assertEquals("CUADERNO RAYADO 100 HOJAS", completo.producto().nombre());
        assertEquals("8.5", completo.producto().precioCompra());
        assertEquals("12", completo.producto().precioVenta());
        assertEquals("MAPED", completo.producto().marca());
        assertEquals("BLOCK", completo.producto().categoria());

        InventarioBodegaExcel.Fila sinMarca = lectura.filas().get(5);
        assertEquals("LAPICERO AZUL", sinMarca.producto().nombre());
        assertEquals("", sinMarca.producto().marca());
        assertEquals("", sinMarca.producto().categoria());
        // Los productos que ya existen no llevan datos de catálogo
        assertEquals(InventarioBodegaExcel.DatosProducto.VACIOS, lectura.filas().get(0).producto());
    }

    @Test
    void laPlantillaTrae8ColumnasConLasDeCatalogoAlFinal() throws Exception {
        try (Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(InventarioBodegaExcel.generarPlantilla()))) {
            Row encabezado = libro.getSheetAt(0).getRow(0);

            assertEquals(8, encabezado.getLastCellNum());
            assertEquals("codigo_producto", encabezado.getCell(0).getStringCellValue());
            assertEquals("nombre", encabezado.getCell(3).getStringCellValue());
            assertEquals("categoria", encabezado.getCell(7).getStringCellValue());
        }
    }

    @Test
    void leeLasColumnasDeCatalogoTalComoSeEscribieronYToleraSusEncabezados() throws Exception {
        InventarioBodegaExcel.Lectura lectura = InventarioBodegaExcel.leer(libro(
                new String[] { "codigo_producto", "cantidad", "nombre", "Precio Compra", "precio_venta", "Marca", "Categoría" },
                new Object[][] { { "N-1", 4, "Lapicero azul", 3.25, 5, "Maped", "Block" } }));

        assertTrue(lectura.errores().isEmpty());
        InventarioBodegaExcel.DatosProducto datos = lectura.filas().get(0).producto();
        assertEquals("Lapicero azul", datos.nombre());
        assertEquals("3.25", datos.precioCompra());
        assertEquals("5", datos.precioVenta());
        assertEquals("Maped", datos.marca());
        assertEquals("Block", datos.categoria());
    }

    @Test
    void sinColumnasDeCatalogoLosDatosDelProductoQuedanVacios() throws Exception {
        InventarioBodegaExcel.Lectura lectura = InventarioBodegaExcel.leer(
                libro(ENCABEZADOS, new Object[][] { { "A", 1 } }));

        assertEquals(InventarioBodegaExcel.DatosProducto.VACIOS, lectura.filas().get(0).producto());
    }

    @Test
    void unaFilaConDatosDeCatalogoPeroSinCodigoSeReportaYNoSeIgnora() throws Exception {
        InventarioBodegaExcel.Lectura lectura = InventarioBodegaExcel.leer(libro(
                new String[] { "codigo_producto", "cantidad", "nombre" }, new Object[][] { { null, null, "Sin código" } }));

        assertTrue(lectura.filas().isEmpty());
        assertEquals(1, lectura.errores().size());
        assertTrue(lectura.errores().get(0).startsWith("Fila 2:"));
    }

    @Test
    void interpretaPreciosConSimboloMilesYComaDecimal() {
        assertEquals(new BigDecimal("12.50"), InventarioBodegaExcel.precio("12.5"));
        assertEquals(new BigDecimal("1200.50"), InventarioBodegaExcel.precio("Q1,200.50"));
        assertEquals(new BigDecimal("8.50"), InventarioBodegaExcel.precio("8,5"));
        assertEquals(new BigDecimal("1200.00"), InventarioBodegaExcel.precio("1,200"));
        assertEquals(new BigDecimal("0.00"), InventarioBodegaExcel.precio("0"));
        assertEquals(new BigDecimal("3.46"), InventarioBodegaExcel.precio("3.455"));
    }

    @Test
    void rechazaPreciosVaciosNegativosOQueNoSonNumeros() {
        assertNull(InventarioBodegaExcel.precio(""));
        assertNull(InventarioBodegaExcel.precio(null));
        assertNull(InventarioBodegaExcel.precio("abc"));
        assertNull(InventarioBodegaExcel.precio("-3"));
        assertNull(InventarioBodegaExcel.precio("1.2.3"));
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
