package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
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

class PendientesDespachoTemplateTest {

    @Test
    void compilaYLlenaPlantillaSinDatos() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/pendientes_despacho.jrxml")) {
            assertNotNull(template);
            JasperReport report = JasperCompileManager.compileReport(template);
            assertTrue(Arrays.stream(report.getParameters())
                    .anyMatch(parameter -> "ID_CLIENTE".equals(parameter.getName())));
            assertTrue(Arrays.stream(report.getFields())
                    .anyMatch(field -> "pendiente".equals(field.getName())));
            JasperPrint print = assertDoesNotThrow(() -> JasperFillManager.fillReport(
                    report, parametros(), new JREmptyDataSource(0)));
            assertXlsx(exportarXlsx(print));
        }
    }

    @Test
    void generaPdfDeMuestraConPendientes() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/pendientes_despacho.jrxml")) {
            assertNotNull(template);
            JasperReport report = JasperCompileManager.compileReport(template);
            Map<String, Object> parameters = parametros();
            parameters.put(JRParameter.IS_IGNORE_PAGINATION, false);
            JasperPrint print = JasperFillManager.fillReport(
                    report, parameters, new JRMapCollectionDataSource(filas()));
            byte[] pdf = JasperExportManager.exportReportToPdf(print);
            assertTrue(pdf.length > 1000);
            assertTrue(pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F');
            guardarMuestra("reporte.pendientes.pdf.muestra.path", pdf);
        }
    }

    @Test
    void generaXlsxDeMuestraConPendientes() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/pendientes_despacho.jrxml")) {
            assertNotNull(template);
            JasperReport report = JasperCompileManager.compileReport(template);
            JasperPrint print = JasperFillManager.fillReport(
                    report, parametros(), new JRMapCollectionDataSource(filas()));
            byte[] xlsx = exportarXlsx(print);
            assertXlsx(xlsx);
            guardarMuestra("reporte.pendientes.xlsx.muestra.path", xlsx);
        }
    }

    private Map<String, Object> parametros() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(JRParameter.IS_IGNORE_PAGINATION, true);
        parameters.put("ID_SUCURSAL", 1);
        parameters.put("ID_CLIENTE", null);
        parameters.put("FECHA_INICIO", Timestamp.valueOf(LocalDate.of(2026, 9, 1).atStartOfDay()));
        parameters.put("FECHA_FIN_EXCLUSIVA", Timestamp.valueOf(LocalDate.of(2026, 9, 24).atStartOfDay()));
        parameters.put("RANGO", "Del 01/09/2026 al 23/09/2026");
        parameters.put("SUCURSAL", "Sucursal Central");
        parameters.put("FILTRO_CLIENTE", "Todos los clientes");
        return parameters;
    }

    private List<Map<String, ?>> filas() {
        List<Map<String, ?>> rows = new ArrayList<>();
        rows.add(fila(151L, "0000151", "2026-09-10 10:15:00", "2026-09-20",
                "Comercial Los Pinos", "548796-2", "75010001", "Cuaderno universitario", 20, 8, 12));
        rows.add(fila(151L, "0000151", "2026-09-10 10:15:00", "2026-09-20",
                "Comercial Los Pinos", "548796-2", "75010002", "Marcador permanente azul", 15, 10, 5));
        rows.add(fila(158L, "0000158", "2026-09-18 14:40:00", "2026-09-25",
                "Distribuidora El Sol", "CF", "75010003", "Detergente multiusos", 24, 0, 24));
        return rows;
    }

    private Map<String, Object> fila(
            long idNota,
            String numero,
            String fechaCreacion,
            String fechaEntrega,
            String cliente,
            String nit,
            String codigo,
            String producto,
            int solicitado,
            int despachado,
            int pendiente) {
        Map<String, Object> row = new HashMap<>();
        row.put("id_nota_credito", idNota);
        row.put("numero", numero);
        row.put("fecha_creacion", Timestamp.valueOf(fechaCreacion));
        row.put("fecha_entrega_estimada", Date.valueOf(fechaEntrega));
        row.put("cliente", cliente);
        row.put("nit", nit);
        row.put("codigo", codigo);
        row.put("producto", producto);
        row.put("solicitado", solicitado);
        row.put("despachado", BigDecimal.valueOf(despachado));
        row.put("pendiente", BigDecimal.valueOf(pendiente));
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
            configuration.setSheetNames(new String[] { "Pendientes despacho" });
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
