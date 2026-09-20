package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.math.BigDecimal;
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
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

class ResumenNotasCreditoTemplateTest {

    @Test
    void compilaYLlenaPlantillaSinConexion() throws Exception {
        try (InputStream template = getClass().getResourceAsStream(
                "/reports/resumen_notas_credito.jrxml")) {
            assertNotNull(template);
            JasperReport report = JasperCompileManager.compileReport(template);

            assertTrue(Arrays.stream(report.getParameters())
                    .anyMatch(parameter -> "ID_SUCURSAL".equals(parameter.getName())));
            assertTrue(Arrays.stream(report.getFields())
                    .anyMatch(field -> "documento_origen".equals(field.getName())));

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("ID_SUCURSAL", 1);
            parameters.put("FECHA_INICIO", Timestamp.valueOf(LocalDate.of(2026, 9, 1).atStartOfDay()));
            parameters.put("FECHA_FIN_EXCLUSIVA", Timestamp.valueOf(LocalDate.of(2026, 9, 20).atStartOfDay()));
            parameters.put("RANGO", "Del 01/09/2026 al 19/09/2026");
            parameters.put("SUCURSAL", "Sucursal Central");
            parameters.put("ESTADO", null);
            parameters.put("FILTRO_ESTADO", "Todos los estados");

            JasperPrint print = assertDoesNotThrow(() -> JasperFillManager.fillReport(
                    report, parameters, new JREmptyDataSource(0)));
            byte[] pdf = JasperExportManager.exportReportToPdf(print);
            assertTrue(pdf.length > 1000);

            String rutaSinDatos = System.getProperty("reporte.sin.datos.path");
            if (rutaSinDatos != null && !rutaSinDatos.isBlank()) {
                Path destino = Path.of(rutaSinDatos);
                Files.createDirectories(destino.getParent());
                Files.write(destino, pdf);
            }
        }
    }

    @Test
    void generaPdfDeMuestraConFilasYTotales() throws Exception {
        try (InputStream template = getClass().getResourceAsStream(
                "/reports/resumen_notas_credito.jrxml")) {
            assertNotNull(template);
            JasperReport report = JasperCompileManager.compileReport(template);
            JasperPrint print = JasperFillManager.fillReport(
                    report, parametros(), new JRMapCollectionDataSource(filas()));
            byte[] pdf = JasperExportManager.exportReportToPdf(print);

            assertTrue(pdf.length > 1000);
            assertTrue(pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F');

            String rutaMuestra = System.getProperty("reporte.muestra.path");
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
        parameters.put("ESTADO", null);
        parameters.put("FILTRO_ESTADO", "Todos los estados");
        return parameters;
    }

    private List<Map<String, ?>> filas() {
        List<Map<String, ?>> rows = new ArrayList<>();
        rows.add(fila("0000142", "2026-09-18 10:15:00", "Comercial Los Pinos", "548796-2",
                "A12 987654", "Andrea López", "ENTREGADO", "1250.75"));
        rows.add(fila("0000141", "2026-09-16 14:40:00", "Distribuidora El Sol", "CF",
                "Proforma PF-00328", "Carlos Méndez", "ENTREGA_PENDIENTE", "845.00"));
        rows.add(fila("0000140", "2026-09-12 09:05:00", "Cliente de Mostrador", "CF",
                "B07 445566", "Andrea López", "PAGADO", "329.50"));
        return rows;
    }

    private Map<String, Object> fila(
            String numero,
            String fecha,
            String cliente,
            String nit,
            String documento,
            String vendedor,
            String estado,
            String total) {
        Map<String, Object> row = new HashMap<>();
        row.put("numero", numero);
        row.put("fecha_creacion", Timestamp.valueOf(fecha));
        row.put("cliente", cliente);
        row.put("nit", nit);
        row.put("documento_origen", documento);
        row.put("vendedor", vendedor);
        row.put("estado", estado);
        row.put("total", new BigDecimal(total));
        return row;
    }
}
