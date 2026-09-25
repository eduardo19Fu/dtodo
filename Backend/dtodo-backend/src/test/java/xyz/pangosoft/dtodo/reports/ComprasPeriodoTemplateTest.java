package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
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

class ComprasPeriodoTemplateTest {

    @Test
    void compilaYLlenaPlantillaSinDatos() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/compras_periodo.jrxml")) {
            assertNotNull(template);
            JasperReport report = JasperCompileManager.compileReport(template);

            assertTrue(Arrays.stream(report.getParameters())
                    .anyMatch(parameter -> "ID_PROVEEDOR".equals(parameter.getName())));
            assertTrue(Arrays.stream(report.getFields())
                    .anyMatch(field -> "proveedor".equals(field.getName())));

            JasperPrint print = assertDoesNotThrow(() -> JasperFillManager.fillReport(
                    report, parametros(), new JREmptyDataSource(0)));
            byte[] pdf = JasperExportManager.exportReportToPdf(print);
            assertTrue(pdf.length > 1000);
        }
    }

    @Test
    void generaPdfDeMuestraConFilasYTotales() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/compras_periodo.jrxml")) {
            assertNotNull(template);
            JasperReport report = JasperCompileManager.compileReport(template);
            JasperPrint print = JasperFillManager.fillReport(
                    report, parametros(), new JRMapCollectionDataSource(filas()));
            byte[] pdf = JasperExportManager.exportReportToPdf(print);

            assertTrue(pdf.length > 1000);
            assertTrue(pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F');

            String rutaMuestra = System.getProperty("reporte.compras.muestra.path");
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
        parameters.put("FECHA_INICIO", Date.valueOf(LocalDate.of(2026, 9, 1)));
        parameters.put("FECHA_FIN_EXCLUSIVA", Date.valueOf(LocalDate.of(2026, 9, 20)));
        parameters.put("RANGO", "Del 01/09/2026 al 19/09/2026");
        parameters.put("SUCURSAL", "Sucursal Central");
        parameters.put("ID_PROVEEDOR", null);
        parameters.put("PROVEEDOR", "Todos los proveedores");
        parameters.put("ESTADO", null);
        parameters.put("FILTRO_ESTADO", "Todos los estados");
        return parameters;
    }

    private List<Map<String, ?>> filas() {
        List<Map<String, ?>> rows = new ArrayList<>();
        rows.add(fila("0000085", "2026-09-18", "FAC-5894", "FACTURA",
                "Distribuidora El Sol", "Andrea López", "ACTIVA", "4250.75"));
        rows.add(fila("0000084", "2026-09-15", "REC-1208", "RECIBO",
                "Comercial Los Pinos", "Carlos Méndez", "ACTIVA", "1845.00"));
        rows.add(fila("0000083", "2026-09-10", "FAC-5831", "FACTURA",
                "Suministros del Norte", "Andrea López", "ANULADA", "932.50"));
        return rows;
    }

    private Map<String, Object> fila(
            String numero,
            String fecha,
            String comprobante,
            String tipo,
            String proveedor,
            String responsable,
            String estado,
            String total) {
        Map<String, Object> row = new HashMap<>();
        row.put("numero", numero);
        row.put("fecha_compra", Date.valueOf(fecha));
        row.put("no_comprobante", comprobante);
        row.put("tipo_comprobante", tipo);
        row.put("proveedor", proveedor);
        row.put("responsable", responsable);
        row.put("estado", estado);
        row.put("total", new BigDecimal(total));
        return row;
    }
}
