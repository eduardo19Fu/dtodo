package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Arrays;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.Test;

class RptNotaCreditoTemplateTest {

    @Test
    void compilaPlantillaPdfDeNotaCreditoConLosCamposDeSucursal() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/rpt_nota_credito.jrxml")) {
            assertNotNull(template, "La plantilla PDF de nota de crédito debe existir");

            JasperReport report = JasperCompileManager.compileReport(template);

            assertNotNull(report);
            assertTrue(Arrays.stream(report.getParameters())
                    .anyMatch(parameter -> "idNotaCredito".equals(parameter.getName())));
            assertTrue(Arrays.stream(report.getFields())
                    .anyMatch(field -> "sucursal_direccion".equals(field.getName())));
            assertTrue(Arrays.stream(report.getFields())
                    .anyMatch(field -> "sucursal_telefono".equals(field.getName())));
        }
    }
}
