package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;

import net.sf.jasperreports.engine.JasperCompileManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FechaHoraGeneracionTemplateTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "factura", "factura_2", "nota_credito", "notas_despachadas",
            "orden_despacho", "productosBodega", "productosDisponibles", "productos_excel",
            "proforma", "proformas_excel", "reporte_notas", "rpt_ventas_diarias",
            "simple", "ventas_mensuales"
    })
    void compilaPlantillaConFechaHoraDeGeneracion(String nombre) throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/" + nombre + ".jrxml")) {
            assertNotNull(template, "La plantilla debe existir: " + nombre);
            assertNotNull(JasperCompileManager.compileReport(template));
        }
    }
}
