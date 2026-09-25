package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
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
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;

class ComprasProveedorProductoTemplateTest {

    @Test
    void compilaYLlenaPlantillaSinDatos() throws Exception {
        try (InputStream template = getClass().getResourceAsStream(
                "/reports/compras_proveedor_producto.jrxml")) {
            assertNotNull(template);
            JasperReport report = JasperCompileManager.compileReport(template);
            assertTrue(Arrays.stream(report.getParameters())
                    .anyMatch(parameter -> "ID_PROVEEDOR".equals(parameter.getName())));
            assertTrue(Arrays.stream(report.getFields())
                    .anyMatch(field -> "costo_promedio".equals(field.getName())));
            JasperPrint print = assertDoesNotThrow(() -> JasperFillManager.fillReport(
                    report, parametros(), new JREmptyDataSource(0)));
            assertXlsx(exportarXlsx(print));
        }
    }

    @Test
    void generaXlsxDeMuestraConTotales() throws Exception {
        try (InputStream template = getClass().getResourceAsStream(
                "/reports/compras_proveedor_producto.jrxml")) {
            assertNotNull(template);
            JasperReport report = JasperCompileManager.compileReport(template);
            JasperPrint print = JasperFillManager.fillReport(
                    report, parametros(), new JRMapCollectionDataSource(filas()));
            byte[] xlsx = exportarXlsx(print);
            assertXlsx(xlsx);
            String ruta = System.getProperty("reporte.compras.proveedor.muestra.path");
            if (ruta != null && !ruta.isBlank()) {
                Path destino = Path.of(ruta);
                Files.createDirectories(destino.getParent());
                Files.write(destino, xlsx);
            }
        }
    }

    @Test
    void agrupaComprasActivasYRespetaFiltros() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:compras_proveedor_reporte;MODE=MySQL;DB_CLOSE_DELAY=-1");
             Statement statement = connection.createStatement();
             InputStream template = getClass().getResourceAsStream(
                     "/reports/compras_proveedor_producto.jrxml")) {
            assertNotNull(template);
            statement.execute("CREATE TABLE proveedores (id_proveedor INT PRIMARY KEY, nombre VARCHAR(100))");
            statement.execute("CREATE TABLE productos (id_producto INT PRIMARY KEY, cod_producto VARCHAR(30), nombre VARCHAR(100))");
            statement.execute("CREATE TABLE compras (id_compra BIGINT PRIMARY KEY, fecha_compra DATE, estado VARCHAR(20), "
                    + "id_sucursal INT, id_proveedor INT)");
            statement.execute("CREATE TABLE compras_detalle (id_compra BIGINT, id_producto INT, cantidad INT, "
                    + "sub_total DECIMAL(12,2))");
            statement.execute("INSERT INTO proveedores VALUES (1, 'Proveedor Uno'), (2, 'Proveedor Dos')");
            statement.execute("INSERT INTO productos VALUES (10, 'A10', 'Cuaderno'), (20, 'B20', 'Lápiz')");
            statement.execute("INSERT INTO compras VALUES (1, DATE '2026-09-10', 'ACTIVA', 1, 1), "
                    + "(2, DATE '2026-09-11', 'ACTIVA', 1, 1), "
                    + "(3, DATE '2026-09-12', 'ANULADA', 1, 1), "
                    + "(4, DATE '2026-09-13', 'ACTIVA', 2, 1), "
                    + "(5, DATE '2026-09-14', 'ACTIVA', 1, 2)");
            statement.execute("INSERT INTO compras_detalle VALUES (1, 10, 2, 20.00), (2, 10, 3, 36.00), "
                    + "(3, 10, 100, 1000.00), (4, 10, 100, 1000.00), (5, 20, 4, 12.00)");

            JasperReport report = JasperCompileManager.compileReport(template);
            Map<String, Object> parameters = parametros();
            parameters.put("ID_PROVEEDOR", 1);
            JasperPrint print = JasperFillManager.fillReport(report, parameters, connection);
            List<String> textos = new ArrayList<>();
            print.getPages().forEach(page -> page.getElements()
                    .forEach(element -> recogerTexto(element, textos)));

            assertEquals(1, print.getPages().size());
            assertTrue(textos.contains("Cuaderno"));
            assertTrue(textos.contains("2"));
            assertTrue(textos.contains("5"));
            assertTrue(textos.contains("Q 11.20"));
            assertTrue(textos.contains("Q 56.00"));
            assertTrue(!textos.contains("Lápiz"));
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
        parameters.put("ID_SUCURSAL", 1);
        parameters.put("ID_PROVEEDOR", null);
        parameters.put("FECHA_INICIO", Date.valueOf(LocalDate.of(2026, 9, 1)));
        parameters.put("FECHA_FIN_EXCLUSIVA", Date.valueOf(LocalDate.of(2026, 10, 1)));
        parameters.put("RANGO", "Del 01/09/2026 al 30/09/2026");
        parameters.put("SUCURSAL", "Sucursal Central");
        parameters.put("FILTRO_PROVEEDOR", "Todos los proveedores");
        return parameters;
    }

    private List<Map<String, ?>> filas() {
        List<Map<String, ?>> rows = new ArrayList<>();
        rows.add(fila(1, "Distribuidora El Sol", "75010001", "Cuaderno universitario",
                3L, "40", "12.50", "500.00"));
        rows.add(fila(1, "Distribuidora El Sol", "75010002", "Marcador permanente azul",
                2L, "25", "7.80", "195.00"));
        rows.add(fila(2, "Suministros del Norte", "75010003", "Detergente multiusos",
                1L, "12", "21.00", "252.00"));
        return rows;
    }

    private Map<String, Object> fila(
            int idProveedor, String proveedor, String codigo, String producto, long compras,
            String unidades, String costoPromedio, String importe) {
        Map<String, Object> row = new HashMap<>();
        row.put("id_proveedor", idProveedor);
        row.put("proveedor", proveedor);
        row.put("codigo", codigo);
        row.put("producto", producto);
        row.put("compras", compras);
        row.put("unidades", new BigDecimal(unidades));
        row.put("costo_promedio", new BigDecimal(costoPromedio));
        row.put("importe", new BigDecimal(importe));
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
            configuration.setSheetNames(new String[] { "Compras por proveedor" });
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
