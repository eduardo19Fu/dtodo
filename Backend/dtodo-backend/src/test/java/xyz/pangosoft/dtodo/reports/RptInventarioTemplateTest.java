package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Arrays;

import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
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
}
