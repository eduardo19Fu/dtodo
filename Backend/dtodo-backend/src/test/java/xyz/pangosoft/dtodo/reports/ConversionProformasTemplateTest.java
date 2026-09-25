package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
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

class ConversionProformasTemplateTest {

    @Test
    void compilaYLlenaPlantillaSinDatos() throws Exception {
        JasperReport report = compilar();
        JasperPrint print = JasperFillManager.fillReport(report, parametros("XLSX"), new JREmptyDataSource(0));
        assertXlsx(exportarXlsx(print));
    }

    @Test
    void generaPdfDeMuestra() throws Exception {
        JasperPrint print = JasperFillManager.fillReport(
                compilar(), parametros("PDF"), new JRMapCollectionDataSource(List.of(fila())));
        byte[] pdf = JasperExportManager.exportReportToPdf(print);
        assertTrue(pdf.length > 1000 && pdf[0] == '%' && pdf[1] == 'P');
        guardar("reporte.conversion-proformas.pdf.muestra.path", pdf);
    }

    @Test
    void generaXlsxDeMuestra() throws Exception {
        JasperPrint print = JasperFillManager.fillReport(
                compilar(), parametros("XLSX"), new JRMapCollectionDataSource(List.of(fila())));
        byte[] xlsx = exportarXlsx(print);
        assertXlsx(xlsx);
        guardar("reporte.conversion-proformas.xlsx.muestra.path", xlsx);
    }

    private JasperReport compilar() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/conversion_proformas.jrxml")) {
            assertNotNull(template);
            return JasperCompileManager.compileReport(template);
        }
    }

    private Map<String, Object> parametros(String formato) {
        Map<String, Object> values = new HashMap<>();
        values.put(JRParameter.IS_IGNORE_PAGINATION, "XLSX".equals(formato));
        values.put("ID_SUCURSAL", 1);
        values.put("ID_USUARIO", 7);
        values.put("FECHA_INICIO", Timestamp.valueOf("2026-09-01 00:00:00"));
        values.put("FECHA_FIN_EXCLUSIVA", Timestamp.valueOf("2026-10-01 00:00:00"));
        values.put("RANGO", "Del 01/09/2026 al 30/09/2026");
        values.put("SUCURSAL", "Sucursal Central");
        values.put("USUARIO", "Ana López");
        values.put("FORMATO", formato);
        return values;
    }

    private Map<String, Object> fila() {
        Map<String, Object> row = new HashMap<>();
        row.put("usuario", "Ana López");
        row.put("proformas_emitidas", 24L);
        row.put("proformas_convertidas", 15L);
        row.put("tasa_conversion", new BigDecimal("0.625"));
        row.put("valor_proformado", new BigDecimal("18450.00"));
        row.put("valor_convertido", new BigDecimal("12680.00"));
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
            configuration.setSheetNames(new String[] { "Conversión de proformas" });
            exporter.setConfiguration(configuration);
            exporter.exportReport();
            return output.toByteArray();
        }
    }

    private void guardar(String property, byte[] content) throws Exception {
        String value = System.getProperty(property);
        if (value != null && !value.isBlank()) {
            Path path = Path.of(value);
            Files.createDirectories(path.getParent());
            Files.write(path, content);
        }
    }

    private void assertXlsx(byte[] xlsx) {
        assertTrue(xlsx.length > 1000 && xlsx[0] == 'P' && xlsx[1] == 'K');
    }
}
