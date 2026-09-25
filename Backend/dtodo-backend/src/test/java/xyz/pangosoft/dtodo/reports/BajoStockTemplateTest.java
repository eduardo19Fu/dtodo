package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
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

class BajoStockTemplateTest {

    @Test
    void compilaYLlenaPlantillaSinDatos() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/bajo_stock.jrxml")) {
            assertNotNull(template);
            JasperReport report = JasperCompileManager.compileReport(template);

            assertTrue(Arrays.stream(report.getParameters())
                    .anyMatch(parameter -> "ID_CATEGORIA".equals(parameter.getName())));
            assertTrue(Arrays.stream(report.getFields())
                    .anyMatch(field -> "inversion_estimada".equals(field.getName())));

            JasperPrint print = assertDoesNotThrow(() -> JasperFillManager.fillReport(
                    report, parametros(), new JREmptyDataSource(0)));
            assertXlsx(exportarXlsx(print));
        }
    }

    @Test
    void generaPdfDeMuestraConFilasYTotales() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/bajo_stock.jrxml")) {
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
            guardarMuestra("reporte.bajo.stock.pdf.muestra.path", pdf);
        }
    }

    @Test
    void generaXlsxDeMuestraConFilasYTotales() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/bajo_stock.jrxml")) {
            assertNotNull(template);
            JasperReport report = JasperCompileManager.compileReport(template);
            JasperPrint print = JasperFillManager.fillReport(
                    report, parametros(), new JRMapCollectionDataSource(filas()));
            byte[] xlsx = exportarXlsx(print);

            assertXlsx(xlsx);
            guardarMuestra("reporte.bajo.stock.xlsx.muestra.path", xlsx);
        }
    }

    private Map<String, Object> parametros() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(JRParameter.IS_IGNORE_PAGINATION, true);
        parameters.put("ID_SUCURSAL", 1);
        parameters.put("SUCURSAL", "Sucursal Central");
        parameters.put("ID_CATEGORIA", null);
        parameters.put("CATEGORIA", "Todas las categorías");
        parameters.put("FORMATO", "XLSX");
        return parameters;
    }

    private List<Map<String, ?>> filas() {
        List<Map<String, ?>> rows = new ArrayList<>();
        rows.add(fila("75010001", "Cuaderno universitario", "Librería", 4, 15, 11, "15.00", "165.00"));
        rows.add(fila("75010002", "Marcador permanente azul", "Oficina", 8, 12, 4, "7.50", "30.00"));
        rows.add(fila("75010003", "Detergente multiusos", "Limpieza", 2, 10, 8, "21.00", "168.00"));
        return rows;
    }

    private Map<String, Object> fila(
            String codigo,
            String producto,
            String categoria,
            int stockActual,
            int stockMinimo,
            int faltante,
            String precioCompra,
            String inversionEstimada) {
        Map<String, Object> row = new HashMap<>();
        row.put("codigo", codigo);
        row.put("producto", producto);
        row.put("categoria", categoria);
        row.put("stock_actual", stockActual);
        row.put("stock_minimo", stockMinimo);
        row.put("faltante", faltante);
        row.put("precio_compra", new BigDecimal(precioCompra));
        row.put("inversion_estimada", new BigDecimal(inversionEstimada));
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
            configuration.setSheetNames(new String[] { "Productos bajo stock" });
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
