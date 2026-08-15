package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;

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
        }
    }
}
