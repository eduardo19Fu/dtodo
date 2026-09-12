package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.Test;

class PolizaGeneralTemplateTest {

    @Test
    void compilaYFiltraPorSucursalYRangoSinLimitarUsuario() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/poliza_general.jrxml")) {
            assertNotNull(template, "La plantilla de póliza general debe existir");
            JasperReport report = JasperCompileManager.compileReport(template);
            java.util.List<String> parametros = Arrays.stream(report.getParameters())
                    .map(JRParameter::getName)
                    .collect(java.util.stream.Collectors.toList());

            assertTrue(parametros.contains("fechaInicio"));
            assertTrue(parametros.contains("fechaFin"));
            assertTrue(parametros.contains("sucursal"));
            assertTrue(report.getQuery().getText().contains("f.id_sucursal = $P{sucursal}"));
            assertTrue(!report.getQuery().getText().contains("f.id_usuario = $P{usuario}"));
            assertTrue(Arrays.stream(report.getGroups()).anyMatch(group -> "VendedorGroup".equals(group.getName())));

            Map<String, Object> params = new HashMap<>();
            params.put("fechaInicio", new Date());
            params.put("fechaFin", new Date());
            params.put("sucursal", 1);
            assertDoesNotThrow(() -> JasperFillManager.fillReport(report, params, new JREmptyDataSource()));
        }
    }
}
