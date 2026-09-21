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

class VentasClienteTemplateTest {

    @Test
    void compilaYLlenaPlantillaSinDatos() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/ventas_cliente.jrxml")) {
            assertNotNull(template);
            JasperReport report = JasperCompileManager.compileReport(template);

            assertTrue(Arrays.stream(report.getParameters())
                    .anyMatch(parameter -> "ID_CLIENTE".equals(parameter.getName())));
            assertTrue(Arrays.stream(report.getFields())
                    .anyMatch(field -> "venta_neta".equals(field.getName())));

            JasperPrint print = assertDoesNotThrow(() -> JasperFillManager.fillReport(
                    report, parametros(), new JREmptyDataSource(0)));
            byte[] xlsx = exportarXlsx(print);
            assertXlsx(xlsx);
        }
    }

    @Test
    void generaXlsxDeMuestraConFilasYTotales() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/ventas_cliente.jrxml")) {
            assertNotNull(template);
            JasperReport report = JasperCompileManager.compileReport(template);
            JasperPrint print = JasperFillManager.fillReport(
                    report, parametros(), new JRMapCollectionDataSource(filas()));
            byte[] xlsx = exportarXlsx(print);

            assertXlsx(xlsx);
            String rutaMuestra = System.getProperty("reporte.ventas.cliente.muestra.path");
            if (rutaMuestra != null && !rutaMuestra.isBlank()) {
                Path destino = Path.of(rutaMuestra);
                Files.createDirectories(destino.getParent());
                Files.write(destino, xlsx);
            }
        }
    }

    @Test
    void generaPdfDeMuestraConFilasYTotales() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/ventas_cliente.jrxml")) {
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
            String rutaMuestra = System.getProperty("reporte.ventas.cliente.pdf.muestra.path");
            if (rutaMuestra != null && !rutaMuestra.isBlank()) {
                Path destino = Path.of(rutaMuestra);
                Files.createDirectories(destino.getParent());
                Files.write(destino, pdf);
            }
        }
    }

    private Map<String, Object> parametros() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(JRParameter.IS_IGNORE_PAGINATION, true);
        parameters.put("ID_SUCURSAL", 1);
        parameters.put("FECHA_INICIO", Timestamp.valueOf("2026-09-01 00:00:00"));
        parameters.put("FECHA_FIN_EXCLUSIVA", Timestamp.valueOf("2026-09-20 00:00:00"));
        parameters.put("RANGO", "Del 01/09/2026 al 19/09/2026");
        parameters.put("SUCURSAL", "Sucursal Central");
        parameters.put("ID_CLIENTE", null);
        parameters.put("CLIENTE", "Todos los clientes");
        parameters.put("FORMATO", "XLSX");
        return parameters;
    }

    private List<Map<String, ?>> filas() {
        List<Map<String, ?>> rows = new ArrayList<>();
        rows.add(fila("Librería El Faro", "5487962-1", "5555-1200", 8L, "76",
                "8250.00", "350.00", "7900.00", "2026-09-18T15:30:00"));
        rows.add(fila("Colegio Monte Verde", "7845123-9", "5555-2431", 5L, "49",
                "4680.00", "180.00", "4500.00", "2026-09-16T10:15:00"));
        rows.add(fila("Consumidor final", "CF", "", 12L, "38",
                "2980.00", "80.00", "2900.00", "2026-09-19T12:45:00"));
        return rows;
    }

    private Map<String, Object> fila(
            String cliente,
            String nit,
            String telefono,
            Long facturas,
            String unidades,
            String bruta,
            String descuento,
            String neta,
            String ultimaCompra) {
        Map<String, Object> row = new HashMap<>();
        row.put("cliente", cliente);
        row.put("nit", nit);
        row.put("telefono", telefono);
        row.put("facturas", facturas);
        row.put("unidades", new BigDecimal(unidades));
        row.put("venta_bruta", new BigDecimal(bruta));
        row.put("descuento", new BigDecimal(descuento));
        row.put("venta_neta", new BigDecimal(neta));
        row.put("ultima_compra", Timestamp.valueOf(LocalDateTime.parse(ultimaCompra)));
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
            configuration.setSheetNames(new String[] { "Ventas por cliente" });
            exporter.setConfiguration(configuration);
            exporter.exportReport();
            return output.toByteArray();
        }
    }

    private void assertXlsx(byte[] xlsx) {
        assertTrue(xlsx.length > 1000);
        assertTrue(xlsx[0] == 'P' && xlsx[1] == 'K');
    }
}
