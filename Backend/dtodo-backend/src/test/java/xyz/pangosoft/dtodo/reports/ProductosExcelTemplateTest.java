package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
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

    private List<Map<String, ?>> filas() {
        Map<String, Object> fila = new HashMap<>();
        fila.put("codigo", "75010001");
        fila.put("nombre", "Cuaderno universitario");
        fila.put("precio_compra", new BigDecimal("8.50"));
        fila.put("precio_venta", new BigDecimal("12.00"));
        fila.put("porcentaje_ganancia", 41.18f);
        fila.put("stock", 4);
        fila.put("marca", "Maped");
        fila.put("tipo", "Librería");
        List<Map<String, ?>> filas = new ArrayList<>();
        filas.add(fila);
        return filas;
    }

    @Test
    void elPrecioDeCompraYElMargenSoloSeImprimenParaElAdministrador() throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/productos_excel.jrxml")) {
            JasperReport report = JasperCompileManager.compileReport(template);
            Map<String, Object> comoOtroRol = new HashMap<>();
            comoOtroRol.put(JRParameter.IS_IGNORE_PAGINATION, true);
            comoOtroRol.put("idSucursal", 1);
            Map<String, Object> comoAdmin = new HashMap<>(comoOtroRol);
            comoAdmin.put("VER_COSTOS", true);

            TextosImpresos sinCostos = new TextosImpresos(JasperFillManager.fillReport(
                    report, comoOtroRol, new JRMapCollectionDataSource(filas())));
            TextosImpresos conCostos = new TextosImpresos(JasperFillManager.fillReport(
                    report, comoAdmin, new JRMapCollectionDataSource(filas())));

            assertTrue(sinCostos.contiene("Precio Venta"));
            assertTrue(sinCostos.contiene("12.00"));
            assertFalse(sinCostos.contiene("Precio Compra"));
            assertFalse(sinCostos.contiene("% Ganancia"));
            assertFalse(sinCostos.contiene("8.50"));
            assertFalse(sinCostos.contiene("41.18"));
            assertTrue(conCostos.contiene("Precio Compra"));
            assertTrue(conCostos.contiene("% Ganancia"));
            assertTrue(conCostos.contiene("8.50"));
        }
    }
}
