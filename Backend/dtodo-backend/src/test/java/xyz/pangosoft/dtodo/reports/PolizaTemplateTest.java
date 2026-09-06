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

class PolizaTemplateTest {

    @Test
    void compilaPlantillaDePolizaConLosParametrosQueEnviaElBackend() throws Exception {
        // La plantilla apuntaba a tablas de un esquema anterior (tbl_documento, tbl_usuario,
        // tbl_producto, tbl_detalle_documento) y declaraba el parametro de fecha como "fechaIni"
        // mientras el backend siempre envio la clave "fecha" -- se corrigio para usar las tablas
        // actuales (facturas, facturas_detalle, productos, usuarios) y el mismo nombre de parametro.
        try (InputStream template = getClass().getResourceAsStream("/reports/poliza.jrxml")) {
            assertNotNull(template, "La plantilla de poliza debe existir");
            JasperReport report = JasperCompileManager.compileReport(template);
            assertNotNull(report);

            java.util.List<String> nombresParametros = Arrays.stream(report.getParameters())
                    .map(JRParameter::getName)
                    .collect(java.util.stream.Collectors.toList());
            assertTrue(nombresParametros.contains("fecha"), "El reporte debe aceptar el parametro 'fecha'");
            assertTrue(nombresParametros.contains("usuario"), "El reporte debe aceptar el parametro 'usuario'");
        }
    }

    @Test
    void llenaElEncabezadoDePolizaSinFallarPorLaImagenDelLogo() throws Exception {
        // Igual que con rpt_inventario: compilar no detecta rutas de imagen rotas, solo llenar
        // el reporte lo hace. La plantilla original cargaba el logo desde una ruta de archivo
        // relativa al working directory del proceso, que nunca existe en el servidor.
        try (InputStream template = getClass().getResourceAsStream("/reports/poliza.jrxml")) {
            JasperReport report = JasperCompileManager.compileReport(template);

            Map<String, Object> params = new HashMap<>();
            params.put("fecha", new Date());
            params.put("usuario", 1);

            assertDoesNotThrow(() -> JasperFillManager.fillReport(report, params, new JREmptyDataSource()));
        }
    }
}
