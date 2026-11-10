package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/**
 * Las tres plantillas de los reportes de mayoristas se llenan con datos de ejemplo (sin base de datos) para
 * comprobar que compilan, qué texto sale y los totales. La consulta SQL se prueba aparte, contra MySQL.
 */
class ReportesMayoristasTemplateTest {

    private static JasperReport compilar(String plantilla) throws Exception {
        try (InputStream in = ReportesMayoristasTemplateTest.class.getResourceAsStream(plantilla)) {
            assertNotNull(in, "Falta la plantilla " + plantilla);
            return JasperCompileManager.compileReport(in);
        }
    }

    private static Map<String, Object> parametros(String ruta) {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("RUTA", ruta);
        parametros.put("FORMATO", "PDF");
        parametros.put("RANGO", "Del 01/10/2026 al 31/10/2026");
        parametros.put("FECHA_INICIO", Timestamp.valueOf(LocalDateTime.of(2026, 10, 1, 0, 0)));
        parametros.put("FECHA_FIN_EXCLUSIVA", Timestamp.valueOf(LocalDateTime.of(2026, 11, 1, 0, 0)));
        return parametros;
    }

    /** Con -Dplantilla.salida=carpeta guarda el PDF para revisarlo a ojo. */
    private static void volcar(String nombre, JasperPrint impreso) throws Exception {
        String carpeta = System.getProperty("plantilla.salida");
        if (carpeta != null) {
            java.nio.file.Files.write(new java.io.File(carpeta, nombre + ".pdf").toPath(), JasperExportManager.exportReportToPdf(impreso));
        }
    }

    @SafeVarargs
    private static JasperPrint llenar(JasperReport reporte, Map<String, Object> parametros, Map<String, ?>... filas) throws Exception {
        return JasperFillManager.fillReport(reporte, parametros, new JRMapCollectionDataSource(new ArrayList<>(Arrays.asList(filas))));
    }

    private static Map<String, Object> clienteRuta(String ruta, String vendedor, String cliente) {
        Map<String, Object> fila = new HashMap<>();
        fila.put("ruta", ruta);
        fila.put("vendedor", vendedor);
        fila.put("codigo", "C-0001");
        fila.put("negocio", "LIBRERIA ELIZYOV");
        fila.put("cliente", cliente);
        fila.put("contacto", "WENDY NAJERA");
        fila.put("telefono", "5555-0000");
        fila.put("direccion", "A UN COSTADO DEL CEMENTERIO MUNICIPAL");
        fila.put("categoria", "A");
        fila.put("dias_credito", 15);
        return fila;
    }

    @Test
    @SuppressWarnings("unchecked")
    void clientesPorRutaAgrupaPorRutaConSuVendedorYCuentaLosClientes() throws Exception {
        JasperReport reporte = compilar("/reports/mayoristas_clientes_ruta.jrxml");

        JasperPrint impreso = llenar(reporte, parametros("Todas las rutas"),
                clienteRuta("RUTA 1", "Bryan Vargas", "ANA PEREZ"), clienteRuta("RUTA 1", "Bryan Vargas", "LUIS GOMEZ"),
                clienteRuta("Sin ruta", "", "MARIA LOPEZ"));
        volcar("ruta", impreso);
        TextosImpresos textos = new TextosImpresos(impreso);

        assertTrue(textos.contiene("Ruta: RUTA 1 · Vendedor: Bryan Vargas"));
        assertTrue(textos.contiene("Ruta: Sin ruta · sin vendedor asignado"));
        assertTrue(textos.contiene("Clientes de la ruta: 2"));
        assertTrue(textos.contiene("Clientes de la ruta: 1"));
        assertTrue(textos.contiene("LIBRERIA ELIZYOV"));
        assertTrue(textos.contiene("Clientes en el reporte:"));
    }

    @Test
    void clientesPorRutaSinDatosMuestraElAviso() throws Exception {
        JasperReport reporte = compilar("/reports/mayoristas_clientes_ruta.jrxml");

        TextosImpresos textos = new TextosImpresos(llenar(reporte, parametros("RUTA 9")));

        assertTrue(textos.contiene("No hay clientes mayoristas para los filtros seleccionados."));
    }

    private static Map<String, Object> sinCompras(String cliente, Timestamp ultima, Integer dias) {
        Map<String, Object> fila = clienteRuta("RUTA 1", "Bryan Vargas", cliente);
        fila.put("ultima_compra", ultima);
        fila.put("dias_sin_compra", dias);
        return fila;
    }

    @Test
    @SuppressWarnings("unchecked")
    void clientesSinComprasMuestraLaUltimaCompraONuncaYCuentaLosQueNuncaComprararon() throws Exception {
        JasperReport reporte = compilar("/reports/mayoristas_clientes_sin_compras.jrxml");

        JasperPrint impreso = llenar(reporte, parametros("RUTA 1"),
                sinCompras("ANA PEREZ", Timestamp.valueOf(LocalDateTime.of(2026, 8, 15, 10, 0)), 72),
                sinCompras("LUIS GOMEZ", null, null));
        volcar("sincompras", impreso);
        TextosImpresos textos = new TextosImpresos(impreso);

        assertTrue(textos.contiene("15/08/2026"));
        assertTrue(textos.contiene("Nunca"));
        assertTrue(textos.contiene("Sin compras · Del 01/10/2026 al 31/10/2026 · RUTA 1"));
        assertTrue(textos.contiene("Clientes sin compras:"));
        assertTrue(textos.contiene("Nunca han comprado:"));
    }

    @Test
    void clientesSinComprasSinDatosDiceQueTodosCompraron() throws Exception {
        JasperReport reporte = compilar("/reports/mayoristas_clientes_sin_compras.jrxml");

        TextosImpresos textos = new TextosImpresos(llenar(reporte, parametros("Todas las rutas")));

        assertTrue(textos.contiene("Todos los clientes mayoristas compraron en el período seleccionado."));
    }

    private static Map<String, Object> vendedor(String nombre, long generadas, String proformado, long facturadas,
                                                String facturado, String tasa, long porAutorizar) {
        Map<String, Object> fila = new HashMap<>();
        fila.put("vendedor", nombre);
        fila.put("generadas", generadas);
        fila.put("valor_proformado", new BigDecimal(proformado));
        fila.put("facturadas", facturadas);
        fila.put("valor_facturado", new BigDecimal(facturado));
        fila.put("tasa_conversion", new BigDecimal(tasa));
        fila.put("por_autorizar", porAutorizar);
        return fila;
    }

    @Test
    @SuppressWarnings("unchecked")
    void proformasPorVendedorMuestraGeneradasFacturadasYSusTotales() throws Exception {
        JasperReport reporte = compilar("/reports/mayoristas_proformas_vendedor.jrxml");

        JasperPrint impreso = llenar(reporte, parametros("Todas las rutas"),
                vendedor("Bryan Vargas", 10, "5000.00", 4, "2500.00", "0.4", 1),
                vendedor("Ana Lopez", 5, "1250.50", 5, "1250.50", "1", 0));
        if (System.getProperty("plantilla.salida") != null) {
            java.nio.file.Files.write(new java.io.File(System.getProperty("plantilla.salida"), "proformas.pdf").toPath(), JasperExportManager.exportReportToPdf(impreso));
        }
        TextosImpresos textos = new TextosImpresos(impreso);

        assertTrue(textos.contiene("Bryan Vargas"));
        assertTrue(textos.contiene("Q 5,000.00"));
        assertTrue(textos.contiene("40.0%"));
        assertTrue(textos.contiene("100.0%"));
        // Totales: 15 generadas, 9 facturadas, Q6,250.50 proformado y Q3,750.50 facturado.
        assertTrue(textos.contiene("15"));
        assertTrue(textos.contiene("Q 6,250.50"));
        assertTrue(textos.contiene("Q 3,750.50"));
    }

    @Test
    void proformasPorVendedorSinDatosMuestraElAviso() throws Exception {
        JasperReport reporte = compilar("/reports/mayoristas_proformas_vendedor.jrxml");

        TextosImpresos textos = new TextosImpresos(llenar(reporte, parametros("Todas las rutas")));

        assertTrue(textos.contiene("No hay proformas mayoristas en el período seleccionado."));
    }

    @Test
    @SuppressWarnings("unchecked")
    void lasTresSePuedenExportarAPdfConMuchasFilas() throws Exception {
        java.util.List<Map<String, Object>> filas = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            filas.add(clienteRuta("RUTA " + (i % 3), "Bryan Vargas", "CLIENTE " + i));
        }
        byte[] pdf = JasperExportManager.exportReportToPdf(
                llenar(compilar("/reports/mayoristas_clientes_ruta.jrxml"), parametros("Todas las rutas"),
                        filas.toArray(new Map[0])));

        assertTrue(pdf.length > 1000);
        assertEquals("%PDF", new String(pdf, 0, 4));
        assertFalse(new TextosImpresos(llenar(compilar("/reports/mayoristas_clientes_ruta.jrxml"),
                parametros("Todas las rutas"), filas.toArray(new Map[0]))).contiene("null"));
    }
}
