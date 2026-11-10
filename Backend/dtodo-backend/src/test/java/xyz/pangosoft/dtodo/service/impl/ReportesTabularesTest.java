package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/** Los reportes tabulares de bodega, compras y ventas salen en PDF y en Excel desde las mismas columnas. */
class ReportesTabularesTest {

    private static Map<String, Object> fila(Object... pares) {
        Map<String, Object> fila = new HashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            fila.put((String) pares[i], pares[i + 1]);
        }
        return fila;
    }

    private static Map<String, Object> parametros(Object... pares) {
        Map<String, Object> parametros = fila(pares);
        parametros.putIfAbsent("RANGO", "Del 01/11/2026 al 30/11/2026");
        parametros.putIfAbsent("SUCURSAL", "Central");
        return parametros;
    }

    private static String texto(byte[] xlsx) throws Exception {
        StringBuilder texto = new StringBuilder();
        DataFormatter formato = new DataFormatter();
        try (Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            Sheet hoja = libro.getSheetAt(0);
            for (Row row : hoja) {
                for (Cell celda : row) {
                    texto.append(formato.formatCellValue(celda)).append('|');
                }
                texto.append('\n');
            }
        }
        return texto.toString();
    }

    private static void guardar(String nombre, byte[] pdf) throws Exception {
        String carpeta = System.getProperty("tabulares.salida");
        if (carpeta != null) {
            Files.write(new File(carpeta, nombre + ".pdf").toPath(), pdf);
        }
    }

    private static void verificar(String nombre, BiFunction<String, Void, byte[]> generador, String... esperados) throws Exception {
        byte[] pdf = generador.apply("PDF", null);
        guardar(nombre, pdf);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII), nombre);
        String excel = texto(generador.apply("XLSX", null));
        for (String esperado : esperados) {
            assertTrue(excel.contains(esperado), nombre + ": falta «" + esperado + "» en\n" + excel);
        }
    }

    @Test
    void despachosConCostosExcluyeLosCanceladosDelTotal() throws Exception {
        List<Map<String, Object>> filas = Arrays.asList(
                fila("numero", 12L, "fecha", Timestamp.valueOf(LocalDateTime.of(2026, 11, 3, 9, 30)), "bodega", "Principal",
                        "sucursal", "Central", "recibe", "Ana", "despacha", "Luis Perez", "estado_etiqueta", "Realizado",
                        "estado_codigo", "REALIZADO", "lineas", 3L, "unidades", 40L, "total", new BigDecimal("500.00")),
                fila("numero", 13L, "fecha", Timestamp.valueOf(LocalDateTime.of(2026, 11, 4, 9, 30)), "bodega", "Principal",
                        "sucursal", "Norte", "recibe", "Mario", "despacha", "Luis Perez", "estado_etiqueta", "Cancelado",
                        "estado_codigo", "CANCELADO", "lineas", 1L, "unidades", 5L, "total", new BigDecimal("99.00")));
        Map<String, Object> p = parametros("BODEGA", "Principal", "ESTADO_ETIQUETA", "Todos los estados", "VER_COSTOS", Boolean.TRUE);

        verificar("despachos", (formato, v) -> ReportesTabulares.despachos(filas, p, formato),
                "Despachos de bodega", "No.|Fecha|Bodega|Destino|Recibe|Despachó|Estado|Líneas|Unidades|Total al costo",
                "000012", "Realizado", "Q500.00");
        String excel = texto(ReportesTabulares.despachos(filas, p, "XLSX"));
        assertFalse(excel.contains("Q99.00"), "el cancelado no muestra importe ni suma: " + excel);

        Map<String, Object> sinCostos = parametros("BODEGA", "Principal", "ESTADO_ETIQUETA", "Todos los estados", "VER_COSTOS", Boolean.FALSE);
        assertFalse(texto(ReportesTabulares.despachos(filas, sinCostos, "XLSX")).contains("Total al costo"));
    }

    @Test
    void existenciasMarcaLoQueEstaBajoElMinimo() throws Exception {
        List<Map<String, Object>> filas = Arrays.asList(
                fila("codigo", "A-1", "producto", "CUADERNO", "categoria", "Papelería", "existencia", 3, "stock_minimo", 5,
                        "costo_unitario", new BigDecimal("2.50"), "valor_total", new BigDecimal("7.50")),
                fila("codigo", "A-2", "producto", "LAPIZ", "categoria", "Papelería", "existencia", 50, "stock_minimo", 5,
                        "costo_unitario", new BigDecimal("1.00"), "valor_total", new BigDecimal("50.00")));
        Map<String, Object> p = parametros("BODEGA", "Principal · Zona 1", "VER_COSTOS", Boolean.TRUE);

        verificar("existencias", (formato, v) -> ReportesTabulares.existencias(filas, p, formato),
                "Código|Producto|Categoría|Existencia|Stock mínimo|Alerta|Costo unitario|Valor total", "Bajo el mínimo", "Q57.50".replace("Q57.50", "Q7.50"));
    }

    @Test
    void movimientosYComprasSalenConSusColumnas() throws Exception {
        List<Map<String, Object>> movimientos = Collections.singletonList(
                fila("fecha", Timestamp.valueOf(LocalDateTime.of(2026, 11, 3, 9, 30)), "tipo", "Ingreso", "codigo", "A-1",
                        "producto", "CUADERNO", "entrada", 10L, "salida", 0L, "saldo", 13L, "usuario", "admin", "detalle", "Compra"));
        verificar("movimientos", (formato, v) -> ReportesTabulares.movimientos(movimientos, parametros("BODEGA", "Principal"), formato),
                "Fecha|Tipo|Código|Producto|Entrada|Salida|Saldo|Usuario|Detalle", "CUADERNO");

        List<Map<String, Object>> compras = Arrays.asList(
                fila("numero", "0000007", "fecha_compra", java.sql.Date.valueOf("2026-11-05"), "no_comprobante", "FAC-1", "tipo_comprobante", "FACTURA",
                        "proveedor", "DISTRIBUIDORA X", "responsable", "Ana Lopez", "estado", "ACTIVA", "total", new BigDecimal("1200.00")),
                fila("numero", "0000008", "fecha_compra", java.sql.Date.valueOf("2026-11-06"), "no_comprobante", "FAC-2", "tipo_comprobante", "FACTURA",
                        "proveedor", "DISTRIBUIDORA Y", "responsable", "Ana Lopez", "estado", "ANULADA", "total", new BigDecimal("300.00")));
        verificar("compras", (formato, v) -> ReportesTabulares.compras(compras,
                parametros("PROVEEDOR", "Todos los proveedores", "FILTRO_ESTADO", "Todos los estados"), formato),
                "No.|Fecha|Comprobante|Tipo|Proveedor|Registrado por|Estado|Total", "Activa", "Anulada", "Q1,200.00");
    }

    @Test
    void ventasYRentabilidadSalenConSusColumnas() throws Exception {
        List<Map<String, Object>> clientes = Collections.singletonList(
                fila("cliente", "LIBRERIA UNO", "nit", "123456-7", "telefono", "5555-0000", "facturas", 4L, "unidades", 80L,
                        "venta_bruta", new BigDecimal("1000.00"), "descuento", new BigDecimal("100.00"),
                        "venta_neta", new BigDecimal("900.00"), "ultima_compra", Timestamp.valueOf(LocalDateTime.of(2026, 11, 20, 8, 0))));
        verificar("ventas_cliente", (formato, v) -> ReportesTabulares.ventasCliente(clientes, parametros("CLIENTE", "Todos los clientes"), formato),
                "Cliente|NIT|Teléfono|Facturas|Unidades|Venta bruta|Descuento|Venta neta|Última compra", "LIBRERIA UNO", "Q900.00");

        List<Map<String, Object>> productos = Collections.singletonList(
                fila("codigo", "A-1", "producto", "CUADERNO", "categoria", "Papelería", "unidades", 80L,
                        "venta_bruta", new BigDecimal("1000.00"), "descuento", new BigDecimal("100.00"), "venta_neta", new BigDecimal("900.00")));
        verificar("ventas_producto", (formato, v) -> ReportesTabulares.ventasProducto(productos, parametros("CATEGORIA", "Todas las categorías"), formato),
                "Código|Producto|Categoría|Unidades|Venta bruta|Descuento|Venta neta", "CUADERNO");

        List<Map<String, Object>> rentables = Arrays.asList(
                fila("codigo", "A-1", "producto", "CUADERNO", "categoria", "Papelería", "unidades", 80L, "venta_neta", new BigDecimal("900.00"),
                        "costo_estimado", new BigDecimal("600.00"), "margen", new BigDecimal("300.00"), "margen_porcentaje", new BigDecimal("0.3333")),
                fila("codigo", "A-2", "producto", "LAPIZ", "categoria", "Papelería", "unidades", 10L, "venta_neta", new BigDecimal("50.00"),
                        "costo_estimado", new BigDecimal("80.00"), "margen", new BigDecimal("-30.00"), "margen_porcentaje", new BigDecimal("-0.6")));
        verificar("rentabilidad", (formato, v) -> ReportesTabulares.rentabilidad(rentables, parametros("CATEGORIA", "Todas las categorías"), formato),
                "Código|Producto|Categoría|Unidades|Venta neta|Costo estimado|Margen|Margen %", "33.33%", "-60.00%");
    }

    @Test
    void sinFilasGeneraAvisoYNoFalla() throws Exception {
        List<Map<String, Object>> vacio = new ArrayList<>();
        Map<String, Object> p = parametros("BODEGA", "Principal", "ESTADO_ETIQUETA", "Todos", "VER_COSTOS", Boolean.FALSE);

        assertTrue(texto(ReportesTabulares.despachos(vacio, p, "XLSX")).contains("No hay registros"));
        assertEquals("%PDF", new String(ReportesTabulares.despachos(vacio, p, "PDF"), 0, 4, StandardCharsets.US_ASCII));
    }
}
