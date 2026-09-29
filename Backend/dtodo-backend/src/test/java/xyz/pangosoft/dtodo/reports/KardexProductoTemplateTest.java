package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRPrintElement;
import net.sf.jasperreports.engine.JRPrintFrame;
import net.sf.jasperreports.engine.JRPrintText;
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
            assertTrue(Arrays.stream(report.getFields())
                    .anyMatch(field -> "documento".equals(field.getName())));

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

    @Test
    void muestraDocumentoDeVentaYNoInventaReferenciaParaMovimientosAnteriores() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:kardex_documentos;MODE=MySQL;DB_CLOSE_DELAY=-1");
             Statement statement = connection.createStatement();
             InputStream template = getClass().getResourceAsStream("/reports/kardex_producto.jrxml")) {
            assertNotNull(template);
            statement.execute("CREATE TABLE usuarios (id_usuario INT PRIMARY KEY, usuario VARCHAR(50))");
            statement.execute("CREATE TABLE facturas (id_factura BIGINT PRIMARY KEY, no_factura BIGINT, serie VARCHAR(20))");
            statement.execute("CREATE TABLE compras (id_compra BIGINT PRIMARY KEY, no_comprobante VARCHAR(50))");
            statement.execute("CREATE TABLE notas_credito (id_nota_credito BIGINT PRIMARY KEY)");
            statement.execute("CREATE TABLE movimientos_producto (id_movimiento BIGINT PRIMARY KEY, "
                    + "fecha_movimiento TIMESTAMP, tipo_movimiento VARCHAR(40), id_usuario INT, "
                    + "stock_inicial INT, cantidad INT, id_sucursal INT, id_producto INT, "
                    + "tipo_documento_origen VARCHAR(30), id_documento_origen BIGINT)");
            statement.execute("INSERT INTO usuarios VALUES (1, 'caja1')");
            statement.execute("INSERT INTO facturas VALUES (10, 456, 'A')");
            statement.execute("INSERT INTO compras VALUES (20, 'FAC-52')");
            statement.execute("INSERT INTO notas_credito VALUES (30)");
            statement.execute("INSERT INTO movimientos_producto VALUES "
                    + "(1, TIMESTAMP '2026-09-09 22:04:02', 'VENTA', 1, 10, 2, 1, 15, 'FACTURA', 10), "
                    + "(2, TIMESTAMP '2026-09-09 22:04:08', 'VENTA', 1, 8, 1, 1, 15, NULL, NULL), "
                    + "(3, TIMESTAMP '2026-09-10 09:00:00', 'COMPRA', 1, 7, 4, 1, 15, 'COMPRA', 20), "
                    + "(4, TIMESTAMP '2026-09-11 10:00:00', 'ENTREGA_PRODUCTO_NOTA', 1, 11, 1, 1, 15, 'NOTA_CREDITO', 30), "
                    + "(5, TIMESTAMP '2026-09-12 11:00:00', 'ENTRADA', 1, 10, 2, 1, 15, NULL, NULL)");

            JasperReport report = JasperCompileManager.compileReport(template);
            List<String> textos = new ArrayList<>();
            JasperPrint print = JasperFillManager.fillReport(report, parametros(), connection);
            print.getPages().forEach(page -> page.getElements()
                    .forEach(element -> recogerTexto(element, textos)));

            assertTrue(textos.contains("Documento: Factura A-456"));
            assertTrue(textos.contains("Documento: No registrado"));
            assertTrue(textos.contains("Documento: Compra FAC-52"));
            assertTrue(textos.contains("Documento: Nota de crédito #30"));
            assertTrue(textos.contains("Documento: Ajuste manual #5"));
            assertTrue(textos.contains("09/09/2026 22:04:02"));
        }
    }

    private void recogerTexto(JRPrintElement element, List<String> textos) {
        if (element instanceof JRPrintText) {
            textos.add(((JRPrintText) element).getFullText());
        } else if (element instanceof JRPrintFrame) {
            ((JRPrintFrame) element).getElements().forEach(hijo -> recogerTexto(hijo, textos));
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
        rows.add(fila(1001L, "2026-09-01T08:30:00", "Compra", "Compra FAC-51", "María López", 20, 15, 0, 35));
        rows.add(fila(1002L, "2026-09-03T11:15:00", "Venta", "Factura A-100", "Carlos Pérez", 35, 0, 4, 31));
        rows.add(fila(1003L, "2026-09-05T16:20:00", "Entrada", "Ajuste manual #1003", "Ana Ruiz", 31, 6, 0, 37));
        rows.add(fila(1004L, "2026-09-08T09:45:00", "Salida", "Ajuste manual #1004", "Ana Ruiz", 37, 0, 2, 35));
        return rows;
    }

    private Map<String, Object> fila(
            Long numero,
            String fecha,
            String movimiento,
            String documento,
            String usuario,
            int stockInicial,
            int entrada,
            int salida,
            int saldo) {
        Map<String, Object> row = new HashMap<>();
        row.put("numero", numero);
        row.put("fecha", Timestamp.valueOf(LocalDateTime.parse(fecha)));
        row.put("movimiento", movimiento);
        row.put("documento", documento);
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
