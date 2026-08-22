package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Arrays;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.Test;

class ProformaPdfTemplateTest {

    @Test
    void compilaPlantillaPdfDeProforma() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/proforma.jrxml")) {
            assertNotNull(template, "La plantilla PDF de proforma debe existir");

            JasperReport report = JasperCompileManager.compileReport(template);

            assertNotNull(report);
            assertTrue(Arrays.stream(report.getParameters())
                    .anyMatch(parameter -> "proformaId".equals(parameter.getName())));
            assertTrue(Arrays.stream(report.getFields())
                    .anyMatch(field -> "precio_venta".equals(field.getName())
                            && "java.math.BigDecimal".equals(field.getValueClassName())));
        }
    }
}
