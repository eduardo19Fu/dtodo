package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/** Las dos pólizas se llenan con datos de ejemplo (sin base de datos): textos, totales y PDF de varias páginas. */
class PolizaVisualTest {

    private static JasperReport compilar(String plantilla) throws Exception {
        try (InputStream in = PolizaVisualTest.class.getResourceAsStream(plantilla)) {
            return JasperCompileManager.compileReport(in);
        }
    }

    private static Map<String, Object> parametros() {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("fechaInicio", Date.valueOf("2026-11-01"));
        parametros.put("fechaFin", Date.valueOf("2026-11-30"));
        parametros.put("sucursal", 1);
        parametros.put("usuario", 2);
        return parametros;
    }

    private static Map<String, Object> fila(int idUsuario, String vendedor, String producto, String cantidad, String pc, String pv) {
        Map<String, Object> fila = new HashMap<>();
        fila.put("id_usuario", idUsuario);
        fila.put("vendedor", vendedor);
        fila.put("sucursal", "Sucursal Central");
        fila.put("id_producto", 1);
        fila.put("nombre_producto", producto);
        fila.put("cantidad", new BigDecimal(cantidad));
        fila.put("pc_total", new BigDecimal(pc));
        fila.put("pv_total", new BigDecimal(pv));
        fila.put("ganancia", new BigDecimal(pv).subtract(new BigDecimal(pc)));
        return fila;
    }

    @SuppressWarnings("unchecked")
    private static JasperPrint llenar(JasperReport reporte, List<Map<String, Object>> filas) throws Exception {
        return JasperFillManager.fillReport(reporte, parametros(), new JRMapCollectionDataSource(new ArrayList<Map<String, ?>>(filas)));
    }

    private static void volcar(String nombre, JasperPrint impreso) throws Exception {
        String carpeta = System.getProperty("plantilla.salida");
        if (carpeta != null) {
            Files.write(new File(carpeta, nombre + ".pdf").toPath(), JasperExportManager.exportReportToPdf(impreso));
        }
    }

    @Test
    void laPolizaIndividualMuestraTotalesYGananciaNegativa() throws Exception {
        List<Map<String, Object>> filas = new ArrayList<>();
        filas.add(fila(2, "Ana Lopez", "CUADERNO UNIVERSITARIO 100 HOJAS", "30", "150.00", "240.00"));
        filas.add(fila(2, "Ana Lopez", "LAPIZ HB CAJA", "10", "80.00", "60.00"));

        JasperPrint impreso = llenar(compilar("/reports/poliza.jrxml"), filas);
        volcar("poliza", impreso);
        TextosImpresos textos = new TextosImpresos(impreso);

        assertTrue(textos.contiene("Resumen de ingresos"));
        assertTrue(textos.contiene("Ana Lopez · Sucursal Central"));
        assertTrue(textos.contiene("01/11/2026 al 30/11/2026"));
        assertTrue(textos.contiene("Q 300.00"), "P.V. total");
        assertTrue(textos.contiene("Q 230.00"), "P.C. total");
        assertTrue(textos.contiene("Q 70.00"), "ganancia total");
        assertTrue(textos.contiene("-Q 20.00"), "la ganancia negativa se muestra");
    }

    @Test
    void laPolizaGeneralAgrupaPorVendedorConSubtotales() throws Exception {
        List<Map<String, Object>> filas = new ArrayList<>();
        filas.add(fila(2, "Ana Lopez", "CUADERNO", "30", "150.00", "240.00"));
        filas.add(fila(2, "Ana Lopez", "LAPIZ", "10", "80.00", "100.00"));
        filas.add(fila(3, "Luis Perez", "BORRADOR", "5", "10.00", "25.00"));

        JasperPrint impreso = llenar(compilar("/reports/poliza_general.jrxml"), filas);
        volcar("poliza_general", impreso);
        TextosImpresos textos = new TextosImpresos(impreso);

        assertTrue(textos.contiene("Póliza general de ventas"));
        assertTrue(textos.contiene("Vendedor: Ana Lopez"));
        assertTrue(textos.contiene("Vendedor: Luis Perez"));
        assertTrue(textos.contiene("Subtotal del vendedor:"));
        assertTrue(textos.contiene("Q 340.00"), "subtotal de ventas de Ana");
        assertTrue(textos.contiene("Q 365.00"), "total general de ventas");
    }

    @Test
    void sinVentasSalenConSuAviso() throws Exception {
        TextosImpresos individual = new TextosImpresos(llenar(compilar("/reports/poliza.jrxml"), new ArrayList<>()));
        TextosImpresos general = new TextosImpresos(llenar(compilar("/reports/poliza_general.jrxml"), new ArrayList<>()));

        assertTrue(individual.contiene("No hay ventas del vendedor en el período seleccionado."));
        assertTrue(general.contiene("No hay ventas en el período seleccionado."));
    }

    @Test
    void conMuchasFilasPaginaYExportaAPdf() throws Exception {
        List<Map<String, Object>> filas = new ArrayList<>();
        for (int i = 0; i < 90; i++) {
            filas.add(fila(2 + i / 30, "Vendedor " + (i / 30), "PRODUCTO NUMERO " + i, "3", "10.00", "15.00"));
        }
        byte[] pdf = JasperExportManager.exportReportToPdf(llenar(compilar("/reports/poliza_general.jrxml"), filas));

        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }
}
