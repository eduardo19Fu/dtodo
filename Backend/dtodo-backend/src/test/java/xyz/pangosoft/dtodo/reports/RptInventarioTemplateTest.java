package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JREmptyDataSource;
import org.junit.jupiter.api.Test;

class RptInventarioTemplateTest {

    @Test
    void compilaPlantillaDeInventarioConElParametroIdSucursal() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/rpt_inventario.jrxml")) {
            assertNotNull(template, "La plantilla del reporte de inventario debe existir");
            JasperReport report = JasperCompileManager.compileReport(template);
            assertNotNull(report);

            boolean tieneParametroIdSucursal = Arrays.stream(report.getParameters())
                    .map(JRParameter::getName)
                    .anyMatch("idSucursal"::equals);
            assertTrue(tieneParametroIdSucursal, "El reporte de inventario debe aceptar el parametro idSucursal");
        }
    }

    @Test
    void llenaElEncabezadoDelReporteSinFallarPorLaImagenDelLogo() throws Exception {
        // Solo compilar no detecta rutas de imagen rotas -- JRFillImage recién resuelve el
        // recurso al llenar el reporte. Aquí se reprodujo "Byte data not found at:
        // static/images/reports-image/dimsa-logo.jpeg" (logo de una marca anterior al rebrand
        // a D'Todo, ruta que ya no existe) al generar el PDF de inventario.
        try (InputStream template = getClass().getResourceAsStream("/reports/rpt_inventario.jrxml")) {
            JasperReport report = JasperCompileManager.compileReport(template);

            Map<String, Object> params = new HashMap<>();
            params.put("fechaIni", new Date());
            params.put("fechaFin", new Date());
            params.put("idSucursal", 1);

            assertDoesNotThrow(() -> JasperFillManager.fillReport(report, params, new JREmptyDataSource()));
        }
    }
}
