package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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

class KardexProductoTemplateTest {

    @Test
    void compilaYLlenaPlantillaSinDatos() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/kardex_producto.jrxml")) {
            assertNotNull(template);
            JasperReport report = JasperCompileManager.compileReport(template);

            assertTrue(Arrays.stream(report.getParameters())
                    .anyMatch(parameter -> "ID_PRODUCTO".equals(parameter.getName())));
            assertTrue(Arrays.stream(report.getFields())
                    .anyMatch(field -> "saldo".equals(field.getName())));

            JasperPrint print = assertDoesNotThrow(() -> JasperFillManager.fillReport(
                    report, parametros(), new JREmptyDataSource(0)));
            assertXlsx(exportarXlsx(print));
        }
    }

    @Test
    void generaPdfDeMuestraConMovimientosYSaldos() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/kardex_producto.jrxml")) {
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
            guardarMuestra("reporte.kardex.producto.pdf.muestra.path", pdf);
        }
    }

    @Test
    void generaXlsxDeMuestraConMovimientosYSaldos() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/kardex_producto.jrxml")) {
            assertNotNull(template);
            JasperReport report = JasperCompileManager.compileReport(template);
            JasperPrint print = JasperFillManager.fillReport(
                    report, parametros(), new JRMapCollectionDataSource(filas()));
            byte[] xlsx = exportarXlsx(print);

            assertXlsx(xlsx);
            guardarMuestra("reporte.kardex.producto.xlsx.muestra.path", xlsx);
        }
    }

    private Map<String, Object> parametros() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(JRParameter.IS_IGNORE_PAGINATION, true);
        parameters.put("ID_SUCURSAL", 1);
        parameters.put("ID_PRODUCTO", 15);
        parameters.put("FECHA_INICIO", Timestamp.valueOf("2026-09-01 00:00:00"));
        parameters.put("FECHA_FIN_EXCLUSIVA", Timestamp.valueOf("2026-09-20 00:00:00"));
        parameters.put("RANGO", "Del 01/09/2026 al 19/09/2026");
        parameters.put("SUCURSAL", "Sucursal Central");
        parameters.put("PRODUCTO", "75010001 · Cuaderno universitario");
        parameters.put("FORMATO", "XLSX");
        return parameters;
    }

    private List<Map<String, ?>> filas() {
        List<Map<String, ?>> rows = new ArrayList<>();
        rows.add(fila(1001L, "2026-09-01T08:30:00", "Compra", "María López", 20, 15, 0, 35));
        rows.add(fila(1002L, "2026-09-03T11:15:00", "Venta", "Carlos Pérez", 35, 0, 4, 31));
        rows.add(fila(1003L, "2026-09-05T16:20:00", "Entrada", "Ana Ruiz", 31, 6, 0, 37));
        rows.add(fila(1004L, "2026-09-08T09:45:00", "Salida", "Ana Ruiz", 37, 0, 2, 35));
        return rows;
    }

    private Map<String, Object> fila(
            Long numero,
            String fecha,
            String movimiento,
            String usuario,
            int stockInicial,
            int entrada,
            int salida,
            int saldo) {
        Map<String, Object> row = new HashMap<>();
        row.put("numero", numero);
        row.put("fecha", Timestamp.valueOf(LocalDateTime.parse(fecha)));
        row.put("movimiento", movimiento);
        row.put("usuario", usuario);
        row.put("stock_inicial", stockInicial);
        row.put("entrada", entrada);
        row.put("salida", salida);
        row.put("saldo", saldo);
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
            configuration.setSheetNames(new String[] { "Kardex de producto" });
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
