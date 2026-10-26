package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/**
 * La proforma mayorista tiene dos copias: la del cliente y la de la tienda, que agrega la observación general del
 * pedido. Se llena con datos de ejemplo (sin base de datos) para comprobar qué texto sale en cada una.
 */
class ProformaMayoristaPdfTemplateTest {

    private static JasperReport report;

    @BeforeAll
    static void compilar() throws Exception {
        try (InputStream template = ProformaMayoristaPdfTemplateTest.class.getResourceAsStream("/reports/proforma_mayorista.jrxml")) {
            assertNotNull(template, "La plantilla de la proforma mayorista debe existir");
            report = JasperCompileManager.compileReport(template);
        }
    }

    private static Map<String, Object> fila(String producto, String observacion) {
        Map<String, Object> fila = new HashMap<>();
        fila.put("nombre", "ANA PEREZ");
        fila.put("nombre_negocio", "LIBRERIA ELIZYOV");
        fila.put("direccion", "A UN COSTADO DEL CEMENTERIO MUNICIPAL, SAN CARLOS ALZATATE");
        fila.put("nit", "4514627-6");
        fila.put("contacto", "WENDY NAJERA");
        fila.put("telefono_contacto", "5555-0000");
        fila.put("dias_credito", 15);
        fila.put("ruta", "RUTA 1");
        fila.put("date_format(pr.fecha_emision,'%d/%m/%Y')", "27/10/2026");
        fila.put("no_proforma", "123456789P");
        fila.put("total", new BigDecimal("850.00"));
        fila.put("observaciones", "Empacar en bolsa individual por niño");
        fila.put("precio_venta", new BigDecimal("12.50"));
        fila.put("producto", producto);
        fila.put("cantidad", 24);
        fila.put("descuento", 10f);
        fila.put("sub_total_descuento", new BigDecimal("270.00"));
        fila.put("observacion", observacion);
        fila.put("vendedor", "Bryan Vargas");
        fila.put("total_letras", "OCHOCIENTOS CINCUENTA QUETZALES");
        fila.put("sucursal_direccion", "3a Avenida, Jalapa");
        fila.put("sucursal_telefono", "7922-0000");
        return fila;
    }

    private static JasperPrint llenar(String copia, Map<String, Object>... filas) throws Exception {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("proformaId", 1L);
        if (copia != null) {
            parametros.put("COPIA", copia);
        }
        return JasperFillManager.fillReport(report, parametros, new JRMapCollectionDataSource(
                new ArrayList<Map<String, ?>>(Arrays.asList(filas))));
    }

    @Test
    void declaraElParametroDeCopiaYLosCamposNuevos() {
        assertTrue(Arrays.stream(report.getParameters()).anyMatch(p -> "COPIA".equals(p.getName())));
        for (String campo : new String[] { "observacion", "observaciones", "ruta", "contacto", "telefono_contacto",
                "dias_credito", "nombre_negocio" }) {
            assertTrue(Arrays.stream(report.getFields()).anyMatch(f -> campo.equals(f.getName())), campo);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void laCopiaDelClienteMuestraLasObservacionesDeLineaPeroNoLaGeneral() throws Exception {
        TextosImpresos textos = new TextosImpresos(llenar("CLIENTE", fila("CUADERNO", "10 rojos, 6 azules y 2 negros")));

        assertTrue(textos.contiene("COPIA CLIENTE"));
        assertTrue(textos.contiene("Obs.: 10 rojos, 6 azules y 2 negros"));
        assertFalse(textos.contiene("Observaciones del pedido"));
        assertFalse(textos.contiene("bolsa individual"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void laCopiaDeLaTiendaAgregaLaObservacionGeneral() throws Exception {
        TextosImpresos textos = new TextosImpresos(llenar("TIENDA", fila("CUADERNO", "10 rojos")));

        assertTrue(textos.contiene("COPIA TIENDA"));
        assertTrue(textos.contiene("Obs.: 10 rojos"));
        assertTrue(textos.contiene("Observaciones del pedido: Empacar en bolsa individual por niño"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sinElParametroSeImprimeLaCopiaDelCliente() throws Exception {
        TextosImpresos textos = new TextosImpresos(llenar(null, fila("CUADERNO", null)));

        assertTrue(textos.contiene("COPIA CLIENTE"));
        assertFalse(textos.contiene("Observaciones del pedido"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void elEncabezadoMuestraNegocioEncargadoRutaCreditoVendedorYTelefonoDeLaTienda() throws Exception {
        TextosImpresos textos = new TextosImpresos(llenar("CLIENTE", fila("CUADERNO", null)));

        for (String etiqueta : new String[] { "Cliente:", "Dirección:", "Encargado:", "Teléfono:", "Ruta:", "Crédito:", "Nit:",
                "Vendedor:" }) {
            assertTrue(textos.contiene(etiqueta), etiqueta);
        }
        assertTrue(textos.contiene("LIBRERIA ELIZYOV (ANA PEREZ)"));
        assertTrue(textos.contiene("WENDY NAJERA"));
        assertTrue(textos.contiene("5555-0000"));
        assertTrue(textos.contiene("RUTA 1"));
        assertTrue(textos.contiene("15 días"));
        assertTrue(textos.contiene("Bryan Vargas"));
        assertTrue(textos.contiene("7922-0000"));
        assertTrue(textos.contiene("123456789P"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sinNombreDeNegocioSeImprimeSoloElNombreYSinCreditoQuedaEnBlanco() throws Exception {
        Map<String, Object> sinNegocio = fila("CUADERNO", null);
        sinNegocio.put("nombre_negocio", null);
        sinNegocio.put("dias_credito", null);

        TextosImpresos textos = new TextosImpresos(llenar("CLIENTE", sinNegocio));

        assertTrue(textos.contiene("ANA PEREZ"));
        assertFalse(textos.contiene("("));
        assertFalse(textos.contiene(" días"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void unaLineaSinObservacionNoImprimeElPrefijo() throws Exception {
        JasperPrint print = llenar("CLIENTE", fila("LAPIZ", null), fila("BORRADOR", "   "), fila("REGLA", "de 30 cm"));

        TextosImpresos textos = new TextosImpresos(print);

        assertTrue(textos.contiene("Obs.: de 30 cm"));
        assertFalse(textos.contiene("Obs.: null"));
        assertFalse(textos.contiene("Obs.: \n"));
        long conPrefijo = print.getPages().stream().flatMap(p -> p.getElements().stream())
                .filter(e -> e instanceof net.sf.jasperreports.engine.JRPrintText)
                .map(e -> ((net.sf.jasperreports.engine.JRPrintText) e).getFullText())
                .filter(t -> t != null && t.startsWith("Obs.:")).count();
        assertEquals(1, conPrefijo);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sePuedeExportarAPdfConVariasLineas() throws Exception {
        List<Map<String, Object>> filas = new ArrayList<>();
        for (int i = 1; i <= 60; i++) {
            filas.add(fila("PRODUCTO " + i, i % 3 == 0 ? "observación de la línea " + i : null));
        }

        byte[] pdf = JasperExportManager.exportReportToPdf(llenar("TIENDA", filas.toArray(new Map[0])));

        assertTrue(pdf.length > 1000);
        assertEquals("%PDF", new String(pdf, 0, 4));
    }
}
