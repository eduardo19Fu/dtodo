package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
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

class ValorizacionInventarioTemplateTest {

    @Test
    void compilaYLlenaPlantillaSinDatos() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/valorizacion_inventario.jrxml")) {
            assertNotNull(template);
            JasperReport report = JasperCompileManager.compileReport(template);
            assertTrue(Arrays.stream(report.getParameters())
                    .anyMatch(parameter -> "FECHA_CORTE_EXCLUSIVA".equals(parameter.getName())));
            assertTrue(Arrays.stream(report.getFields())
                    .anyMatch(field -> "valor_costo".equals(field.getName())));
            JasperPrint print = assertDoesNotThrow(() -> JasperFillManager.fillReport(
                    report, parametros(), new JREmptyDataSource(0)));
            assertXlsx(exportarXlsx(print));
        }
    }

    @Test
    void generaPdfDeMuestraConValorizacion() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/valorizacion_inventario.jrxml")) {
            assertNotNull(template);
            JasperReport report = JasperCompileManager.compileReport(template);
            Map<String, Object> parameters = parametros();
            parameters.put(JRParameter.IS_IGNORE_PAGINATION, false);
            parameters.put("FORMATO", "PDF");
            JasperPrint print = JasperFillManager.fillReport(
                    report, parameters, new JRMapCollectionDataSource(filas()));
            byte[] pdf = JasperExportManager.exportReportToPdf(print);
            assertTrue(pdf.length > 1000);
            assertTrue(pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F');
            guardarMuestra("reporte.valorizacion.pdf.muestra.path", pdf);
        }
    }

    @Test
    void generaXlsxDeMuestraConValorizacion() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/valorizacion_inventario.jrxml")) {
            assertNotNull(template);
            JasperReport report = JasperCompileManager.compileReport(template);
            JasperPrint print = JasperFillManager.fillReport(
                    report, parametros(), new JRMapCollectionDataSource(filas()));
            byte[] xlsx = exportarXlsx(print);
            assertXlsx(xlsx);
            guardarMuestra("reporte.valorizacion.xlsx.muestra.path", xlsx);
        }
    }

    private Map<String, Object> parametros() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(JRParameter.IS_IGNORE_PAGINATION, true);
        parameters.put("ID_SUCURSAL", 1);
        parameters.put("FECHA_CORTE_EXCLUSIVA", Timestamp.valueOf("2026-09-23 00:00:00"));
        parameters.put("FECHA_CORTE", "22/09/2026");
        parameters.put("SUCURSAL", "Sucursal Central");
        parameters.put("FORMATO", "XLSX");
        return parameters;
    }

    private List<Map<String, ?>> filas() {
        List<Map<String, ?>> rows = new ArrayList<>();
        rows.add(fila("75010001", "Cuaderno universitario", "Librería", 35, "15.00", "525.00", "25.00", "875.00"));
        rows.add(fila("75010002", "Marcador permanente azul", "Oficina", 18, "7.50", "135.00", "12.00", "216.00"));
        rows.add(fila("75010003", "Detergente multiusos", "Limpieza", 12, "21.00", "252.00", "32.00", "384.00"));
        return rows;
    }

    private Map<String, Object> fila(
            String codigo,
            String producto,
            String categoria,
            int stock,
            String precioCompra,
            String valorCosto,
            String precioVenta,
            String valorVenta) {
        Map<String, Object> row = new HashMap<>();
        row.put("codigo", codigo);
        row.put("producto", producto);
        row.put("categoria", categoria);
        row.put("stock_corte", stock);
        row.put("precio_compra", new BigDecimal(precioCompra));
        row.put("valor_costo", new BigDecimal(valorCosto));
        row.put("precio_venta", new BigDecimal(precioVenta));
        row.put("valor_venta", new BigDecimal(valorVenta));
        return row;
    }

    private byte[] exportarXlsx(JasperPrint print) throws Exception {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            JRXlsxExporter exporter = new JRXlsxExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(output));
            SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
            configuration.setDetectCellType(true);
            configuration.setRemoveEmptySpaceBetweenRows(true);
            configuration.setWhitePageBackground(false);
            configuration.setOnePagePerSheet(false);
            configuration.setSheetNames(new String[] { "Valorización inventario" });
            exporter.setConfiguration(configuration);
            exporter.exportReport();
            return output.toByteArray();
        }
    }

    private void guardarMuestra(String propiedad, byte[] contenido) throws Exception {
        String ruta = System.getProperty(propiedad);
        if (ruta != null && !ruta.isBlank()) {
            Path destino = Path.of(ruta);
            Files.createDirectories(destino.getParent());
            Files.write(destino, contenido);
        }
    }

    private void assertXlsx(byte[] xlsx) {
        assertTrue(xlsx.length > 1000);
        assertTrue(xlsx[0] == 'P' && xlsx[1] == 'K');
    }
}
