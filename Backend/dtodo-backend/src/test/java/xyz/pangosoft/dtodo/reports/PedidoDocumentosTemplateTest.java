package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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

/** Hoja de picking, lista de empaque y nota de envío: compilan y muestran lo que cada una debe mostrar. */
class PedidoDocumentosTemplateTest {

    private static JasperReport compilar(String plantilla) throws Exception {
        try (InputStream in = PedidoDocumentosTemplateTest.class.getResourceAsStream(plantilla)) {
            assertNotNull(in, "Falta la plantilla " + plantilla);
            return JasperCompileManager.compileReport(in);
        }
    }

    @SafeVarargs
    private static JasperPrint llenar(JasperReport reporte, Map<String, Object> parametros, Map<String, ?>... filas) throws Exception {
        return JasperFillManager.fillReport(reporte, parametros, new JRMapCollectionDataSource(new ArrayList<>(Arrays.asList(filas))));
    }

    private static Map<String, Object> linea(String codigo, String producto, int cantidad, String observacion) {
        Map<String, Object> fila = new HashMap<>();
        fila.put("no_proforma", "123456789P");
        fila.put("fecha", "27/10/2026");
        fila.put("entrega", "30/10/2026");
        fila.put("observaciones", "Empacar en bolsa individual por niño");
        fila.put("cliente", "ANA PEREZ");
        fila.put("negocio", "LIBRERIA ELIZYOV");
        fila.put("direccion", "A UN COSTADO DEL CEMENTERIO MUNICIPAL");
        fila.put("contacto", "WENDY NAJERA");
        fila.put("telefono_contacto", "5555-0000");
        fila.put("ruta", "RUTA 1");
        fila.put("vendedor", "Bryan Vargas");
        fila.put("codigo", codigo);
        fila.put("producto", producto);
        fila.put("cantidad", cantidad);
        fila.put("observacion", observacion);
        return fila;
    }

    private static Map<String, Object> parametros(String documento) {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ID_PROFORMA", 7L);
        if (documento != null) {
            parametros.put("DOCUMENTO", documento);
        }
        return parametros;
    }

    @Test
    @SuppressWarnings("unchecked")
    void elPickingMuestraLasLineasConSuObservacionPeroNoLaObservacionGeneral() throws Exception {
        JasperReport reporte = compilar("/reports/pedido_bodega.jrxml");

        TextosImpresos textos = new TextosImpresos(llenar(reporte, parametros("PICKING"),
                linea("A-1", "CUADERNO", 10, "10 rojos, 6 azules y 2 negros"), linea("B-2", "REGLA", 5, null)));

        assertTrue(textos.contiene("Hoja de picking"));
        assertFalse(textos.contiene("Lista de empaque"));
        assertTrue(textos.contiene("Pedido 123456789P"));
        assertTrue(textos.contiene("CUADERNO"));
        assertTrue(textos.contiene("10 rojos, 6 azules y 2 negros"));
        assertTrue(textos.contiene("LIBRERIA ELIZYOV (ANA PEREZ)"));
        assertTrue(textos.contiene("RUTA 1"));
        assertTrue(textos.contiene("Total de unidades:"));
        assertTrue(textos.contiene("15"));
        assertTrue(textos.contiene("Preparó:"));
        assertFalse(textos.contiene("Observaciones del pedido"));
        assertFalse(textos.contiene("Q "));
    }

    @Test
    @SuppressWarnings("unchecked")
    void elEmpaqueSiMuestraLaObservacionGeneralYPideBultos() throws Exception {
        JasperReport reporte = compilar("/reports/pedido_bodega.jrxml");

        TextosImpresos textos = new TextosImpresos(llenar(reporte, parametros("EMPAQUE"),
                linea("A-1", "CUADERNO", 10, null)));

        assertTrue(textos.contiene("Lista de empaque"));
        assertTrue(textos.contiene("Observaciones del pedido: Empacar en bolsa individual por niño"));
        assertTrue(textos.contiene("Empacó:"));
        assertTrue(textos.contiene("Bultos:"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sinObservacionGeneralElEmpaqueNoImprimeElRotulo() throws Exception {
        JasperReport reporte = compilar("/reports/pedido_bodega.jrxml");
        Map<String, Object> fila = linea("A-1", "CUADERNO", 10, null);
        fila.put("observaciones", "  ");

        TextosImpresos textos = new TextosImpresos(llenar(reporte, parametros("EMPAQUE"), fila));

        assertFalse(textos.contiene("Observaciones del pedido"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sinElParametroSeImprimeElPickingYUnPedidoGrandeSeExportaAPdf() throws Exception {
        JasperReport reporte = compilar("/reports/pedido_bodega.jrxml");
        List<Map<String, Object>> filas = new ArrayList<>();
        for (int i = 1; i <= 70; i++) {
            filas.add(linea("C-" + i, "PRODUCTO " + i, i, i % 4 == 0 ? "observación larga de la línea " + i + " con varios detalles para el empaque" : null));
        }

        JasperPrint print = llenar(reporte, parametros(null), filas.toArray(new Map[0]));
        byte[] pdf = JasperExportManager.exportReportToPdf(print);

        assertTrue(new TextosImpresos(print).contiene("Hoja de picking"));
        assertTrue(pdf.length > 1000);
        assertEquals("%PDF", new String(pdf, 0, 4));
        assertTrue(print.getPages().size() > 1);
    }

    @Test
    void unPedidoSinLineasMuestraElAviso() throws Exception {
        TextosImpresos textos = new TextosImpresos(llenar(compilar("/reports/pedido_bodega.jrxml"), parametros("PICKING")));

        assertTrue(textos.contiene("El pedido no tiene productos."));
    }

    private static Map<String, Object> pedidoDeRuta(String numero, String cliente, long unidades, String estado) {
        Map<String, Object> fila = new HashMap<>();
        fila.put("no_proforma", numero);
        fila.put("negocio", "LIBRERIA ELIZYOV");
        fila.put("cliente", cliente);
        fila.put("direccion", "A UN COSTADO DEL CEMENTERIO MUNICIPAL");
        fila.put("contacto", "WENDY NAJERA");
        fila.put("telefono", "5555-0000");
        fila.put("unidades", unidades);
        fila.put("entrega", "30/10/2026");
        fila.put("estado", estado);
        return fila;
    }

    private static Map<String, Object> parametrosNota() {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ID_RUTA", 1);
        parametros.put("RUTA", "RUTA 1");
        parametros.put("FECHA", "28/10/2026");
        parametros.put("FECHA_INICIO", Timestamp.valueOf(LocalDateTime.of(2026, 10, 28, 0, 0)));
        parametros.put("FECHA_FIN_EXCLUSIVA", Timestamp.valueOf(LocalDateTime.of(2026, 10, 29, 0, 0)));
        return parametros;
    }

    @Test
    @SuppressWarnings("unchecked")
    void laNotaDeEnvioListaLosPedidosDeLaRutaConSusTotalesYSinFirmaDeRecibido() throws Exception {
        TextosImpresos textos = new TextosImpresos(llenar(compilar("/reports/nota_envio.jrxml"), parametrosNota(),
                pedidoDeRuta("111P", "ANA PEREZ", 15, "EMPACADA"), pedidoDeRuta("222P", "LUIS GOMEZ", 30, "EN_RUTA")));

        assertTrue(textos.contiene("Nota de envío · carga a ruta"));
        assertTrue(textos.contiene("RUTA 1 · 28/10/2026"));
        assertTrue(textos.contiene("111P"));
        assertTrue(textos.contiene("Por cargar"));
        assertTrue(textos.contiene("En ruta"));
        assertTrue(textos.contiene("Total de unidades:"));
        assertTrue(textos.contiene("45"));
        assertTrue(textos.contiene("Cargó:"));
        assertTrue(textos.contiene("Piloto:"));
        assertFalse(textos.contiene("Recibió"));
        assertFalse(textos.contiene("Q "));
    }

    @Test
    void laNotaDeEnvioSinPedidosMuestraElAviso() throws Exception {
        TextosImpresos textos = new TextosImpresos(llenar(compilar("/reports/nota_envio.jrxml"), parametrosNota()));

        assertTrue(textos.contiene("No hay pedidos empacados ni en ruta para esa ruta y fecha."));
    }
}
