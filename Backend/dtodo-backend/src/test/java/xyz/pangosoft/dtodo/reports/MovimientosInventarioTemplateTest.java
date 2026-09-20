package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

class MovimientosInventarioTemplateTest {

    @Test
    void compilaYLlenaPlantillaSinDatos() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/movimientos_inventario.jrxml")) {
            assertNotNull(template);
            JasperReport report = JasperCompileManager.compileReport(template);

            assertTrue(Arrays.stream(report.getParameters())
                    .anyMatch(parameter -> "ID_SUCURSAL".equals(parameter.getName())));
            assertTrue(Arrays.stream(report.getFields())
                    .anyMatch(field -> "saldo_final".equals(field.getName())));

            JasperPrint print = assertDoesNotThrow(() -> JasperFillManager.fillReport(
                    report, parametros(), new JREmptyDataSource(0)));
            byte[] pdf = JasperExportManager.exportReportToPdf(print);
            assertTrue(pdf.length > 1000);
        }
    }

    @Test
    void generaPdfDeMuestraConFilasYTotales() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/movimientos_inventario.jrxml")) {
            assertNotNull(template);
            JasperReport report = JasperCompileManager.compileReport(template);
            JasperPrint print = JasperFillManager.fillReport(
                    report, parametros(), new JRMapCollectionDataSource(filas()));
            byte[] pdf = JasperExportManager.exportReportToPdf(print);

            assertTrue(pdf.length > 1000);
            assertTrue(pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F');

            String rutaMuestra = System.getProperty("reporte.inventario.muestra.path");
            if (rutaMuestra != null && !rutaMuestra.isBlank()) {
                Path destino = Path.of(rutaMuestra);
                Files.createDirectories(destino.getParent());
                Files.write(destino, pdf);
            }
        }
    }

    private Map<String, Object> parametros() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ID_SUCURSAL", 1);
        parameters.put("FECHA_INICIO", Timestamp.valueOf(LocalDate.of(2026, 9, 1).atStartOfDay()));
        parameters.put("FECHA_FIN_EXCLUSIVA", Timestamp.valueOf(LocalDate.of(2026, 9, 20).atStartOfDay()));
        parameters.put("RANGO", "Del 01/09/2026 al 19/09/2026");
        parameters.put("SUCURSAL", "Sucursal Central");
        return parameters;
    }

    private List<Map<String, ?>> filas() {
        List<Map<String, ?>> rows = new ArrayList<>();
        rows.add(fila("75010001", "Cuaderno universitario", "Librería", 120L, 35L, 18L, 137L));
        rows.add(fila("75010002", "Marcador permanente azul", "Oficina", 48L, 24L, 12L, 60L));
        rows.add(fila("75010003", "Detergente multiusos", "Limpieza", 75L, 40L, 27L, 88L));
        return rows;
    }

    private Map<String, Object> fila(
            String codigo,
            String producto,
            String categoria,
            Long inicial,
            Long entradas,
            Long salidas,
            Long saldo) {
        Map<String, Object> row = new HashMap<>();
        row.put("codigo", codigo);
        row.put("producto", producto);
        row.put("categoria", categoria);
        row.put("stock_inicial", inicial);
        row.put("entradas", entradas);
        row.put("salidas", salidas);
        row.put("saldo_final", saldo);
        return row;
    }
}
