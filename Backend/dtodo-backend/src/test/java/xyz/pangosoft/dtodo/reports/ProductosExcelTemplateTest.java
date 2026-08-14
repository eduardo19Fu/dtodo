package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.Test;

class ProductosExcelTemplateTest {

    @Test
    void compilaPlantillaDeProductos() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/productos_excel.jrxml")) {
            assertNotNull(template, "La plantilla del reporte de productos debe existir");
            JasperReport report = JasperCompileManager.compileReport(template);
            assertNotNull(report);
        }
    }
}
