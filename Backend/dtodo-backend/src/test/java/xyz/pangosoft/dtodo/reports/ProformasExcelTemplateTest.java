package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Arrays;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.Test;

class ProformasExcelTemplateTest {

    @Test
    void compilaPlantillaDeProformas() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/proformas_excel.jrxml")) {
            assertNotNull(template, "La plantilla del reporte de proformas debe existir");
            JasperReport report = JasperCompileManager.compileReport(template);
            assertNotNull(report);
            assertTrue(Arrays.stream(report.getParameters())
                    .anyMatch(parameter -> "ID_USUARIO".equals(parameter.getName())));
        }
    }
}
