package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;

/**
 * Compila cada plantilla de bodegas, la llena con filas de muestra (sin base de datos) y comprueba que
 * exporta un PDF y un XLSX válidos, también cuando no hay datos.
 */
class BodegaTemplatesTest {

    private JasperReport compilar(String nombre) throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/" + nombre + ".jrxml")) {
            assertNotNull(template, "La plantilla debe existir: " + nombre);
            return JasperCompileManager.compileReport(template);
        }
    }

    private boolean tieneParametro(JasperReport report, String nombre) {
        return Arrays.stream(report.getParameters()).anyMatch(parameter -> nombre.equals(parameter.getName()));
    }

    private void assertPdf(byte[] pdf) {
        assertTrue(pdf.length > 1000);
        assertTrue(pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F');
    }

    private void assertXlsx(byte[] xlsx) {
        assertTrue(xlsx.length > 1000);
        assertTrue(xlsx[0] == 'P' && xlsx[1] == 'K');
    }

    private byte[] exportarXlsx(JasperPrint print, String hoja) throws Exception {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            JRXlsxExporter exporter = new JRXlsxExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(output));
            SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
            configuration.setDetectCellType(true);
            configuration.setRemoveEmptySpaceBetweenRows(true);
            configuration.setWhitePageBackground(false);
            configuration.setOnePagePerSheet(false);
            configuration.setSheetNames(new String[] { hoja });
            exporter.setConfiguration(configuration);
            exporter.exportReport();
            return output.toByteArray();
        }
    }

    private Map<String, Object> rango(Map<String, Object> parametros) {
        parametros.put("RANGO", "Del 01/10/2026 al 02/10/2026");
        parametros.put("FECHA_INICIO", Timestamp.valueOf(LocalDateTime.of(2026, 10, 1, 0, 0)));
        parametros.put("FECHA_FIN_EXCLUSIVA", Timestamp.valueOf(LocalDateTime.of(2026, 10, 3, 0, 0)));
        return parametros;
    }

    // ---------- existencias ----------

    private Map<String, Object> parametrosExistencias(String formato) {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put(JRParameter.IS_IGNORE_PAGINATION, "XLSX".equals(formato));
        parametros.put("ID_BODEGA", 1);
        parametros.put("BODEGA", "Principal · Zona 1");
        parametros.put("FORMATO", formato);
        return parametros;
    }

    private List<Map<String, ?>> filasExistencias() {
        List<Map<String, ?>> filas = new ArrayList<>();
        filas.add(filaExistencia("A-1", "Cuaderno universitario", "Librería", 3, 10, "12.50"));
        filas.add(filaExistencia("B-2", "Lápiz HB", "Oficina", 200, null, "1.25"));
        return filas;
    }

    private Map<String, Object> filaExistencia(String codigo, String producto, String categoria,
            int existencia, Integer minimo, String costo) {
        Map<String, Object> fila = new HashMap<>();
        fila.put("codigo", codigo);
        fila.put("producto", producto);
        fila.put("categoria", categoria);
        fila.put("existencia", existencia);
        fila.put("stock_minimo", minimo);
        fila.put("costo_unitario", new BigDecimal(costo));
        fila.put("valor_total", new BigDecimal(costo).multiply(BigDecimal.valueOf(existencia)));
        return fila;
    }

    @Test
    void existenciasCompilaYSeLlenaSinDatos() throws Exception {
        JasperReport report = compilar("bodega_existencias");
        assertTrue(tieneParametro(report, "ID_BODEGA"));

        JasperPrint print = assertDoesNotThrow(() -> JasperFillManager.fillReport(
                report, parametrosExistencias("XLSX"), new JREmptyDataSource(0)));
        assertXlsx(exportarXlsx(print, "Existencias de bodega"));
    }

    @Test
    void existenciasGeneraPdfYXlsxConFilasYTotales() throws Exception {
        JasperReport report = compilar("bodega_existencias");

        JasperPrint pdf = JasperFillManager.fillReport(
                report, parametrosExistencias("PDF"), new JRMapCollectionDataSource(filasExistencias()));
        assertPdf(JasperExportManager.exportReportToPdf(pdf));

        JasperPrint xlsx = JasperFillManager.fillReport(
                report, parametrosExistencias("XLSX"), new JRMapCollectionDataSource(filasExistencias()));
        assertXlsx(exportarXlsx(xlsx, "Existencias de bodega"));
    }

    // ---------- movimientos ----------

    private Map<String, Object> parametrosMovimientos(String formato) {
        Map<String, Object> parametros = rango(new HashMap<>());
        parametros.put(JRParameter.IS_IGNORE_PAGINATION, "XLSX".equals(formato));
        parametros.put("ID_BODEGA", 1);
        parametros.put("BODEGA", "Principal · Zona 1");
        parametros.put("FORMATO", formato);
        return parametros;
    }

    private List<Map<String, ?>> filasMovimientos() {
        List<Map<String, ?>> filas = new ArrayList<>();
        filas.add(filaMovimiento("Ingreso", 100L, 0L, 100L, "Carga inicial"));
        filas.add(filaMovimiento("Despacho", 0L, 40L, 60L, "Despacho #1 hacia Norte"));
        return filas;
    }

    private Map<String, Object> filaMovimiento(String tipo, long entrada, long salida, long saldo, String detalle) {
        Map<String, Object> fila = new HashMap<>();
        fila.put("fecha", Timestamp.valueOf(LocalDateTime.of(2026, 10, 2, 9, 30)));
        fila.put("tipo", tipo);
        fila.put("codigo", "A-1");
        fila.put("producto", "Cuaderno universitario");
        fila.put("entrada", entrada);
        fila.put("salida", salida);
        fila.put("saldo", saldo);
        fila.put("usuario", "bodeguero");
        fila.put("detalle", detalle);
        return fila;
    }

    @Test
    void movimientosCompilaYSeLlenaSinDatos() throws Exception {
        JasperReport report = compilar("bodega_movimientos");
        assertTrue(tieneParametro(report, "FECHA_FIN_EXCLUSIVA"));

        JasperPrint print = assertDoesNotThrow(() -> JasperFillManager.fillReport(
                report, parametrosMovimientos("XLSX"), new JREmptyDataSource(0)));
        assertXlsx(exportarXlsx(print, "Movimientos de bodega"));
    }

    @Test
    void movimientosGeneraPdfYXlsxSinDejarCeldasDeEntradaOSalidaEnCero() throws Exception {
        JasperReport report = compilar("bodega_movimientos");

        JasperPrint pdf = JasperFillManager.fillReport(
                report, parametrosMovimientos("PDF"), new JRMapCollectionDataSource(filasMovimientos()));
        assertPdf(JasperExportManager.exportReportToPdf(pdf));

        JasperPrint xlsx = JasperFillManager.fillReport(
                report, parametrosMovimientos("XLSX"), new JRMapCollectionDataSource(filasMovimientos()));
        assertXlsx(exportarXlsx(xlsx, "Movimientos de bodega"));
    }

    // ---------- despachos ----------

    private Map<String, Object> parametrosDespachos(String formato) {
        Map<String, Object> parametros = rango(new HashMap<>());
        parametros.put(JRParameter.IS_IGNORE_PAGINATION, "XLSX".equals(formato));
        parametros.put("ID_BODEGA", null);
        parametros.put("BODEGA", "Todas las bodegas");
        parametros.put("ESTADO", null);
        parametros.put("ESTADO_ETIQUETA", "Todos los estados");
        parametros.put("FORMATO", formato);
        return parametros;
    }

    private Map<String, Object> filaDespacho(long numero, String estado, String total) {
        Map<String, Object> fila = new HashMap<>();
        fila.put("numero", numero);
        fila.put("fecha", Timestamp.valueOf(LocalDateTime.of(2026, 10, 2, 10, 15)));
        fila.put("bodega", "Principal");
        fila.put("sucursal", "Norte");
        fila.put("recibe", "María López");
        fila.put("despacha", "Juan Pérez");
        fila.put("estado_etiqueta", estado);
        fila.put("estado_codigo", estado.toUpperCase());
        fila.put("lineas", 2L);
        fila.put("unidades", 24L);
        fila.put("total", new BigDecimal(total));
        return fila;
    }

    private List<Map<String, ?>> filasDespachos() {
        List<Map<String, ?>> filas = new ArrayList<>();
        filas.add(filaDespacho(1, "Realizado", "75.00"));
        filas.add(filaDespacho(2, "Pendiente", "30.00"));
        filas.add(filaDespacho(3, "Cancelado", "999.00"));
        return filas;
    }

    @Test
    void despachosCompilaYSeLlenaSinDatosYConFiltrosOpcionalesNulos() throws Exception {
        JasperReport report = compilar("bodega_despachos");
        assertTrue(tieneParametro(report, "ESTADO"));

        JasperPrint print = assertDoesNotThrow(() -> JasperFillManager.fillReport(
                report, parametrosDespachos("XLSX"), new JREmptyDataSource(0)));
        assertXlsx(exportarXlsx(print, "Despachos de bodega"));
    }

    @Test
    void despachosGeneraPdfYXlsxYElTotalExcluyeLosCancelados() throws Exception {
        JasperReport report = compilar("bodega_despachos");

        JasperPrint pdf = JasperFillManager.fillReport(
                report, parametrosDespachos("PDF"), new JRMapCollectionDataSource(filasDespachos()));
        assertPdf(JasperExportManager.exportReportToPdf(pdf));

        JasperPrint xlsx = JasperFillManager.fillReport(
                report, parametrosDespachos("XLSX"), new JRMapCollectionDataSource(filasDespachos()));
        byte[] contenido = exportarXlsx(xlsx, "Despachos de bodega");
        assertXlsx(contenido);
        // 75.00 (realizado) + 30.00 (pendiente) = 105.00; los 999.00 del cancelado no se suman
        assertTrue(contieneNumero(contenido, 105.0), "El total valorizado debe ser 105.00");
        assertFalse(contieneNumero(contenido, 1104.0));
    }

    private boolean contieneNumero(byte[] xlsx, double esperado) throws Exception {
        try (org.apache.poi.ss.usermodel.Workbook libro =
                new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(xlsx))) {
            for (org.apache.poi.ss.usermodel.Sheet hoja : libro) {
                for (org.apache.poi.ss.usermodel.Row fila : hoja) {
                    for (org.apache.poi.ss.usermodel.Cell celda : fila) {
                        if (celda.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                                && Math.abs(celda.getNumericCellValue() - esperado) < 0.001) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // ---------- comprobante ----------

    private Map<String, Object> filaComprobante(String codigo, String producto, int cantidad, String precio, int existencia,
            String estado) {
        Map<String, Object> fila = new HashMap<>();
        fila.put("id_despacho", 12L);
        fila.put("fecha_registro", Timestamp.valueOf(LocalDateTime.of(2026, 10, 2, 10, 15)));
        fila.put("fecha_resolucion", "REALIZADO".equals(estado) ? Timestamp.valueOf(LocalDateTime.of(2026, 10, 2, 11, 0)) : null);
        fila.put("estado", estado);
        fila.put("total", new BigDecimal("75.00"));
        fila.put("recibido_por", "María López");
        fila.put("observaciones", "Reposición semanal");
        fila.put("motivo_cancelacion", "CANCELADO".equals(estado) ? "Error de captura" : null);
        fila.put("bodega", "Principal");
        fila.put("bodega_ubicacion", "Zona 1, Jalapa");
        fila.put("sucursal_destino", "Norte");
        fila.put("sucursal_direccion", "3a. Calle 4-56");
        fila.put("despachado_por", "Juan Pérez");
        fila.put("resuelto_por", "REALIZADO".equals(estado) || "CANCELADO".equals(estado) ? "Ana Gómez" : null);
        fila.put("codigo", codigo);
        fila.put("producto", producto);
        fila.put("cantidad", cantidad);
        fila.put("precio_unitario", new BigDecimal(precio));
        fila.put("sub_total", new BigDecimal(precio).multiply(BigDecimal.valueOf(cantidad)));
        fila.put("existencia_bodega", existencia);
        return fila;
    }

    private List<Map<String, ?>> filasComprobante(String estado) {
        List<Map<String, ?>> filas = new ArrayList<>();
        filas.add(filaComprobante("A-1", "Cuaderno universitario", 4, "12.50", 50, estado));
        filas.add(filaComprobante("B-2", "Lápiz HB", 20, "1.25", 200, estado));
        return filas;
    }

    @Test
    void comprobanteCompilaYSeLlenaSinDatos() throws Exception {
        JasperReport report = compilar("despacho_bodega");
        assertTrue(tieneParametro(report, "ID_DESPACHO"));

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ID_DESPACHO", 12L);
        assertDoesNotThrow(() -> JasperFillManager.fillReport(report, parametros, new JREmptyDataSource(0)));
    }

    @Test
    void comprobanteGeneraPdfParaCadaEstadoDelDespacho() throws Exception {
        JasperReport report = compilar("despacho_bodega");

        for (String estado : new String[] { "PENDIENTE", "REALIZADO", "CANCELADO" }) {
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("ID_DESPACHO", 12L);
            JasperPrint print = JasperFillManager.fillReport(
                    report, parametros, new JRMapCollectionDataSource(filasComprobante(estado)));
            assertPdf(JasperExportManager.exportReportToPdf(print));
        }
    }
}
