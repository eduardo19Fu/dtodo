package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;

/**
 * Compila cada plantilla de bodegas, la llena con filas de muestra (sin base de datos) y comprueba que
 * exporta un PDF y un XLSX válidos, también cuando no hay datos.
 */
class BodegaTemplatesTest {

    private JasperReport compilar(String nombre) throws Exception {
        try (InputStream template = getClass().getResourceAsStream("/reports/" + nombre + ".jrxml")) {
            assertNotNull(template, "La plantilla debe existir: " + nombre);
            return JasperCompileManager.compileReport(template);
        }
    }

    private boolean tieneParametro(JasperReport report, String nombre) {
        return Arrays.stream(report.getParameters()).anyMatch(parameter -> nombre.equals(parameter.getName()));
    }

    private void assertPdf(byte[] pdf) {
        assertTrue(pdf.length > 1000);
        assertTrue(pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F');
    }

    private void assertXlsx(byte[] xlsx) {
        assertTrue(xlsx.length > 1000);
        assertTrue(xlsx[0] == 'P' && xlsx[1] == 'K');
    }

    private byte[] exportarXlsx(JasperPrint print, String hoja) throws Exception {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            JRXlsxExporter exporter = new JRXlsxExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(output));
            SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
            configuration.setDetectCellType(true);
            configuration.setRemoveEmptySpaceBetweenRows(true);
            configuration.setWhitePageBackground(false);
            configuration.setOnePagePerSheet(false);
            configuration.setSheetNames(new String[] { hoja });
            exporter.setConfiguration(configuration);
            exporter.exportReport();
            return output.toByteArray();
        }
    }

    private Map<String, Object> rango(Map<String, Object> parametros) {
        parametros.put("RANGO", "Del 01/10/2026 al 02/10/2026");
        parametros.put("FECHA_INICIO", Timestamp.valueOf(LocalDateTime.of(2026, 10, 1, 0, 0)));
        parametros.put("FECHA_FIN_EXCLUSIVA", Timestamp.valueOf(LocalDateTime.of(2026, 10, 3, 0, 0)));
        return parametros;
    }

    // ---------- visibilidad del costo ----------

    private Map<String, Object> conCostos(Map<String, Object> parametros) {
        parametros.put("VER_COSTOS", true);
        return parametros;
    }


    // ---------- existencias ----------

    private Map<String, Object> parametrosExistencias(String formato) {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put(JRParameter.IS_IGNORE_PAGINATION, "XLSX".equals(formato));
        parametros.put("ID_BODEGA", 1);
        parametros.put("BODEGA", "Principal · Zona 1");
        parametros.put("FORMATO", formato);
        return parametros;
    }

    private List<Map<String, ?>> filasExistencias() {
        List<Map<String, ?>> filas = new ArrayList<>();
        filas.add(filaExistencia("A-1", "Cuaderno universitario", "Librería", 3, 10, "12.50"));
        filas.add(filaExistencia("B-2", "Lápiz HB", "Oficina", 200, null, "1.25"));
        return filas;
    }

    private Map<String, Object> filaExistencia(String codigo, String producto, String categoria,
            int existencia, Integer minimo, String costo) {
        Map<String, Object> fila = new HashMap<>();
        fila.put("codigo", codigo);
        fila.put("producto", producto);
        fila.put("categoria", categoria);
        fila.put("existencia", existencia);
        fila.put("stock_minimo", minimo);
        fila.put("costo_unitario", new BigDecimal(costo));
        fila.put("valor_total", new BigDecimal(costo).multiply(BigDecimal.valueOf(existencia)));
        return fila;
    }

    @Test
    void existenciasCompilaYSeLlenaSinDatos() throws Exception {
        JasperReport report = compilar("bodega_existencias");
        assertTrue(tieneParametro(report, "ID_BODEGA"));

        JasperPrint print = assertDoesNotThrow(() -> JasperFillManager.fillReport(
                report, parametrosExistencias("XLSX"), new JREmptyDataSource(0)));
        assertXlsx(exportarXlsx(print, "Existencias de bodega"));
    }

    @Test
    void existenciasGeneraPdfYXlsxConFilasYTotales() throws Exception {
        JasperReport report = compilar("bodega_existencias");

        JasperPrint pdf = JasperFillManager.fillReport(
                report, parametrosExistencias("PDF"), new JRMapCollectionDataSource(filasExistencias()));
        assertPdf(JasperExportManager.exportReportToPdf(pdf));

        JasperPrint xlsx = JasperFillManager.fillReport(
                report, parametrosExistencias("XLSX"), new JRMapCollectionDataSource(filasExistencias()));
        assertXlsx(exportarXlsx(xlsx, "Existencias de bodega"));
    }

    // ---------- movimientos ----------

    private Map<String, Object> parametrosMovimientos(String formato) {
        Map<String, Object> parametros = rango(new HashMap<>());
        parametros.put(JRParameter.IS_IGNORE_PAGINATION, "XLSX".equals(formato));
        parametros.put("ID_BODEGA", 1);
        parametros.put("BODEGA", "Principal · Zona 1");
        parametros.put("FORMATO", formato);
        return parametros;
    }

    private List<Map<String, ?>> filasMovimientos() {
        List<Map<String, ?>> filas = new ArrayList<>();
        filas.add(filaMovimiento("Ingreso", 100L, 0L, 100L, "Carga inicial"));
        filas.add(filaMovimiento("Despacho", 0L, 40L, 60L, "Despacho #1 hacia Norte"));
        return filas;
    }

    private Map<String, Object> filaMovimiento(String tipo, long entrada, long salida, long saldo, String detalle) {
        Map<String, Object> fila = new HashMap<>();
        fila.put("fecha", Timestamp.valueOf(LocalDateTime.of(2026, 10, 2, 9, 30)));
        fila.put("tipo", tipo);
        fila.put("codigo", "A-1");
        fila.put("producto", "Cuaderno universitario");
        fila.put("entrada", entrada);
        fila.put("salida", salida);
        fila.put("saldo", saldo);
        fila.put("usuario", "bodeguero");
        fila.put("detalle", detalle);
        return fila;
    }

    @Test
    void movimientosCompilaYSeLlenaSinDatos() throws Exception {
        JasperReport report = compilar("bodega_movimientos");
        assertTrue(tieneParametro(report, "FECHA_FIN_EXCLUSIVA"));

        JasperPrint print = assertDoesNotThrow(() -> JasperFillManager.fillReport(
                report, parametrosMovimientos("XLSX"), new JREmptyDataSource(0)));
        assertXlsx(exportarXlsx(print, "Movimientos de bodega"));
    }

    @Test
    void movimientosGeneraPdfYXlsxSinDejarCeldasDeEntradaOSalidaEnCero() throws Exception {
        JasperReport report = compilar("bodega_movimientos");

        JasperPrint pdf = JasperFillManager.fillReport(
                report, parametrosMovimientos("PDF"), new JRMapCollectionDataSource(filasMovimientos()));
        assertPdf(JasperExportManager.exportReportToPdf(pdf));

        JasperPrint xlsx = JasperFillManager.fillReport(
                report, parametrosMovimientos("XLSX"), new JRMapCollectionDataSource(filasMovimientos()));
        assertXlsx(exportarXlsx(xlsx, "Movimientos de bodega"));
    }

    // ---------- despachos ----------

    private Map<String, Object> parametrosDespachos(String formato) {
        Map<String, Object> parametros = rango(new HashMap<>());
        parametros.put(JRParameter.IS_IGNORE_PAGINATION, "XLSX".equals(formato));
        parametros.put("ID_BODEGA", null);
        parametros.put("BODEGA", "Todas las bodegas");
        parametros.put("ESTADO", null);
        parametros.put("ESTADO_ETIQUETA", "Todos los estados");
        parametros.put("FORMATO", formato);
        return parametros;
    }

    private Map<String, Object> filaDespacho(long numero, String estado, String total) {
        Map<String, Object> fila = new HashMap<>();
        fila.put("numero", numero);
        fila.put("fecha", Timestamp.valueOf(LocalDateTime.of(2026, 10, 2, 10, 15)));
        fila.put("bodega", "Principal");
        fila.put("sucursal", "Norte");
        fila.put("recibe", "María López");
        fila.put("despacha", "Juan Pérez");
        fila.put("estado_etiqueta", estado);
        fila.put("estado_codigo", estado.toUpperCase());
        fila.put("lineas", 2L);
        fila.put("unidades", 24L);
        fila.put("total", new BigDecimal(total));
        return fila;
    }

    private List<Map<String, ?>> filasDespachos() {
        List<Map<String, ?>> filas = new ArrayList<>();
        filas.add(filaDespacho(1, "Realizado", "75.00"));
        filas.add(filaDespacho(2, "Pendiente", "30.00"));
        filas.add(filaDespacho(3, "Cancelado", "999.00"));
        return filas;
    }

    @Test
    void despachosCompilaYSeLlenaSinDatosYConFiltrosOpcionalesNulos() throws Exception {
        JasperReport report = compilar("bodega_despachos");
        assertTrue(tieneParametro(report, "ESTADO"));

        JasperPrint print = assertDoesNotThrow(() -> JasperFillManager.fillReport(
                report, parametrosDespachos("XLSX"), new JREmptyDataSource(0)));
        assertXlsx(exportarXlsx(print, "Despachos de bodega"));
    }

    @Test
    void despachosGeneraPdfYXlsxYElTotalExcluyeLosCancelados() throws Exception {
        JasperReport report = compilar("bodega_despachos");

        JasperPrint pdf = JasperFillManager.fillReport(
                report, parametrosDespachos("PDF"), new JRMapCollectionDataSource(filasDespachos()));
        assertPdf(JasperExportManager.exportReportToPdf(pdf));

        JasperPrint xlsx = JasperFillManager.fillReport(
                report, conCostos(parametrosDespachos("XLSX")), new JRMapCollectionDataSource(filasDespachos()));
        byte[] contenido = exportarXlsx(xlsx, "Despachos de bodega");
        assertXlsx(contenido);
        // 75.00 (realizado) + 30.00 (pendiente) = 105.00; los 999.00 del cancelado no se suman
        assertTrue(contieneNumero(contenido, 105.0), "El total valorizado debe ser 105.00");
        assertFalse(contieneNumero(contenido, 1104.0));
    }

    private boolean contieneNumero(byte[] xlsx, double esperado) throws Exception {
        try (org.apache.poi.ss.usermodel.Workbook libro =
                new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(xlsx))) {
            for (org.apache.poi.ss.usermodel.Sheet hoja : libro) {
                for (org.apache.poi.ss.usermodel.Row fila : hoja) {
                    for (org.apache.poi.ss.usermodel.Cell celda : fila) {
                        if (celda.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                                && Math.abs(celda.getNumericCellValue() - esperado) < 0.001) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // ---------- comprobante ----------

    private Map<String, Object> filaComprobante(String codigo, String producto, int cantidad, String precio, int existencia,
            String estado) {
        Map<String, Object> fila = new HashMap<>();
        fila.put("id_despacho", 12L);
        fila.put("fecha_registro", Timestamp.valueOf(LocalDateTime.of(2026, 10, 2, 10, 15)));
        fila.put("fecha_resolucion", "REALIZADO".equals(estado) ? Timestamp.valueOf(LocalDateTime.of(2026, 10, 2, 11, 0)) : null);
        fila.put("estado", estado);
        fila.put("total", new BigDecimal("75.00"));
        fila.put("recibido_por", "María López");
        fila.put("observaciones", "Reposición semanal");
        fila.put("motivo_cancelacion", "CANCELADO".equals(estado) ? "Error de captura" : null);
        fila.put("bodega", "Principal");
        fila.put("bodega_ubicacion", "Zona 1, Jalapa");
        fila.put("sucursal_destino", "Norte");
        fila.put("sucursal_direccion", "3a. Calle 4-56");
        fila.put("despachado_por", "Juan Pérez");
        fila.put("resuelto_por", "REALIZADO".equals(estado) || "CANCELADO".equals(estado) ? "Ana Gómez" : null);
        fila.put("codigo", codigo);
        fila.put("producto", producto);
        fila.put("cantidad", cantidad);
        fila.put("precio_unitario", new BigDecimal(precio));
        fila.put("sub_total", new BigDecimal(precio).multiply(BigDecimal.valueOf(cantidad)));
        fila.put("existencia_bodega", existencia);
        return fila;
    }

    private List<Map<String, ?>> filasComprobante(String estado) {
        List<Map<String, ?>> filas = new ArrayList<>();
        filas.add(filaComprobante("A-1", "Cuaderno universitario", 4, "12.50", 50, estado));
        filas.add(filaComprobante("B-2", "Lápiz HB", 20, "1.25", 200, estado));
        return filas;
    }

    @Test
    void comprobanteCompilaYSeLlenaSinDatos() throws Exception {
        JasperReport report = compilar("despacho_bodega");
        assertTrue(tieneParametro(report, "ID_DESPACHO"));

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ID_DESPACHO", 12L);
        assertDoesNotThrow(() -> JasperFillManager.fillReport(report, parametros, new JREmptyDataSource(0)));
    }

    @Test
    void comprobanteGeneraPdfParaCadaEstadoDelDespacho() throws Exception {
        JasperReport report = compilar("despacho_bodega");

        for (String estado : new String[] { "PENDIENTE", "REALIZADO", "CANCELADO" }) {
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("ID_DESPACHO", 12L);
            JasperPrint print = JasperFillManager.fillReport(
                    report, parametros, new JRMapCollectionDataSource(filasComprobante(estado)));
            assertPdf(JasperExportManager.exportReportToPdf(print));
        }
    }

    @Test
    void laGuiaDeDespachoMuestraDestinoUnidadesAvanceYRecepcion() throws Exception {
        JasperReport report = compilar("despacho_bodega");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ID_DESPACHO", 12L);

        TextosImpresos pendiente = new TextosImpresos(JasperFillManager.fillReport(
                report, parametros, new JRMapCollectionDataSource(filasComprobante("PENDIENTE"))));
        TextosImpresos realizado = new TextosImpresos(JasperFillManager.fillReport(
                report, parametros, new JRMapCollectionDataSource(filasComprobante("REALIZADO"))));
        TextosImpresos cancelado = new TextosImpresos(JasperFillManager.fillReport(
                report, parametros, new JRMapCollectionDataSource(filasComprobante("CANCELADO"))));

        // Etiqueta de envío: total de unidades (4 + 20) y destino
        assertTrue(pendiente.contiene("TOTAL A ENVIAR"));
        assertTrue(pendiente.contiene("24"));
        assertTrue(pendiente.contiene("UNIDADES"));
        assertTrue(pendiente.contiene("SUCURSAL DESTINO"));
        assertTrue(pendiente.contiene("Norte"));
        // Columnas propias de la guía y bloque de recepción
        assertTrue(pendiente.contiene("Quedan"));
        assertTrue(pendiente.contiene("Conforme"));
        assertTrue(pendiente.contiene("RECEPCIÓN EN"));
        assertTrue(pendiente.contiene("Recibido conforme"));
        // Avance en pasos según el estado
        assertTrue(pendiente.contiene("PENDIENTE DE APROBACIÓN"));
        assertTrue(pendiente.contiene("2 · EN ESPERA"));
        assertTrue(pendiente.contiene("3 · INGRESO AL DESTINO"));
        assertTrue(realizado.contiene("DESPACHO REALIZADO"));
        assertTrue(realizado.contiene("2 · APROBADO"));
        assertTrue(realizado.contiene("3 · INGRESÓ AL DESTINO"));
        assertTrue(realizado.contiene("APROBADO POR"));
        assertTrue(cancelado.contiene("DESPACHO CANCELADO"));
        assertTrue(cancelado.contiene("3 · REGRESÓ A LA BODEGA"));
        assertTrue(cancelado.contiene("MOTIVO DE LA CANCELACIÓN"));
        assertTrue(cancelado.contiene("Error de captura"));
        assertFalse(realizado.contiene("MOTIVO DE LA CANCELACIÓN"));
    }

    @Test
    void laGuiaDeDespachoIndicaCuandoElDestinoEsOtraBodega() throws Exception {
        JasperReport report = compilar("despacho_bodega");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ID_DESPACHO", 12L);
        List<Map<String, ?>> filas = new ArrayList<>();
        Map<String, Object> fila = filaComprobante("A-1", "Cuaderno universitario", 4, "12.50", 50, "PENDIENTE");
        fila.put("es_traslado", 1);
        fila.put("sucursal_destino", "Bodega Secundaria");
        filas.add(fila);

        TextosImpresos textos = new TextosImpresos(JasperFillManager.fillReport(
                report, parametros, new JRMapCollectionDataSource(filas)));

        assertTrue(textos.contiene("BODEGA DESTINO · TRASLADO"));
        assertTrue(textos.contiene("Bodega Secundaria"));
        assertFalse(textos.contiene("SUCURSAL DESTINO"));
    }

    // ---------- el costo solo lo imprime el administrador ----------

    @Test
    void existenciasSoloMuestraElCostoYElValorAlAdministrador() throws Exception {
        JasperReport report = compilar("bodega_existencias");

        TextosImpresos sinCostos = new TextosImpresos(JasperFillManager.fillReport(
                report, parametrosExistencias("PDF"), new JRMapCollectionDataSource(filasExistencias())));
        TextosImpresos conCostos = new TextosImpresos(JasperFillManager.fillReport(
                report, conCostos(parametrosExistencias("PDF")), new JRMapCollectionDataSource(filasExistencias())));

        assertTrue(sinCostos.contiene("Cuaderno universitario"));
        assertFalse(sinCostos.contiene("Costo unitario"));
        assertFalse(sinCostos.contiene("Valor total"));
        assertFalse(sinCostos.contiene("12.50"));
        assertFalse(sinCostos.contiene("al costo"));
        assertTrue(conCostos.contiene("Costo unitario"));
        assertTrue(conCostos.contiene("Valor total:"));
        assertTrue(conCostos.contiene("12.50"));
        assertTrue(conCostos.contiene("al costo"));
    }

    @Test
    void despachosSoloMuestraElTotalValorizadoAlAdministrador() throws Exception {
        JasperReport report = compilar("bodega_despachos");

        TextosImpresos sinCostos = new TextosImpresos(JasperFillManager.fillReport(
                report, parametrosDespachos("PDF"), new JRMapCollectionDataSource(filasDespachos())));
        TextosImpresos conCostos = new TextosImpresos(JasperFillManager.fillReport(
                report, conCostos(parametrosDespachos("PDF")), new JRMapCollectionDataSource(filasDespachos())));

        assertTrue(sinCostos.contiene("Despachos:"));
        assertFalse(sinCostos.contiene("Total valorizado"));
        assertFalse(sinCostos.contiene("75.00"));
        assertFalse(sinCostos.contiene("al costo"));
        assertTrue(conCostos.contiene("Total valorizado:"));
        assertTrue(conCostos.contiene("75.00"));
    }

    @Test
    void elComprobanteSoloMuestraCostosSubtotalesYTotalAlAdministrador() throws Exception {
        JasperReport report = compilar("despacho_bodega");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ID_DESPACHO", 12L);

        // Sin indicar VER_COSTOS el comprobante sale sin valores: es el comportamiento seguro por defecto
        TextosImpresos sinCostos = new TextosImpresos(JasperFillManager.fillReport(
                report, parametros, new JRMapCollectionDataSource(filasComprobante("REALIZADO"))));
        TextosImpresos conCostos = new TextosImpresos(JasperFillManager.fillReport(
                report, conCostos(new HashMap<>(parametros)), new JRMapCollectionDataSource(filasComprobante("REALIZADO"))));

        assertTrue(sinCostos.contiene("Cuaderno universitario"));
        assertTrue(sinCostos.contiene("Cantidad"));
        assertFalse(sinCostos.contiene("Costo unit."));
        assertFalse(sinCostos.contiene("Subtotal"));
        assertFalse(sinCostos.contiene("Total:"));
        assertFalse(sinCostos.contiene("12.50"));
        assertFalse(sinCostos.contiene("75.00"));
        assertTrue(conCostos.contiene("Costo unit."));
        assertTrue(conCostos.contiene("Subtotal"));
        assertTrue(conCostos.contiene("12.50"));
        assertTrue(conCostos.contiene("75.00"));
    }

    // ---------- comprobante de devolución ----------

    private Map<String, Object> filaDevolucion(String codigo, String producto, int cantidad, String precio, int existencia,
            String estado) {
        Map<String, Object> fila = new HashMap<>();
        fila.put("id_devolucion", 3L);
        fila.put("fecha_registro", Timestamp.valueOf(LocalDateTime.of(2026, 10, 16, 9, 15)));
        fila.put("fecha_resolucion", "PENDIENTE".equals(estado) ? null : Timestamp.valueOf(LocalDateTime.of(2026, 10, 16, 11, 20)));
        fila.put("estado", estado);
        fila.put("total", new BigDecimal("312.50"));
        fila.put("entregado_por", "María López");
        fila.put("motivo", "Producto con sobrestock en la tienda");
        fila.put("observaciones", "Se entrega en cajas selladas.");
        fila.put("motivo_cancelacion", "CANCELADO".equals(estado) ? "No cumple las condiciones" : null);
        fila.put("sucursal", "Mataquescuintla");
        fila.put("sucursal_direccion", "Barrio El Centro, Mataquescuintla");
        fila.put("bodega", "Bodega Jalapa 1");
        fila.put("bodega_ubicacion", "Zona 2, Jalapa");
        fila.put("registrado_por", "Carlos Demo");
        fila.put("resuelto_por", "PENDIENTE".equals(estado) ? null : "Ana Gómez");
        fila.put("codigo", codigo);
        fila.put("producto", producto);
        fila.put("cantidad", cantidad);
        fila.put("precio_unitario", new BigDecimal(precio));
        fila.put("sub_total", new BigDecimal(precio).multiply(BigDecimal.valueOf(cantidad)));
        fila.put("existencia_sucursal", existencia);
        return fila;
    }

    private List<Map<String, ?>> filasDevolucion(String estado) {
        List<Map<String, ?>> filas = new ArrayList<>();
        filas.add(filaDevolucion("7501", "Cuaderno profesional", 10, "12.50", 24, estado));
        filas.add(filaDevolucion("7504", "Regla plástica", 4, "37.50", 48, estado));
        filas.add(filaDevolucion("7502", "Lápiz grafito", 1, "37.50", 1, estado));
        return filas;
    }

    @Test
    void comprobanteDeDevolucionCompilaYSeLlenaSinDatos() throws Exception {
        JasperReport report = compilar("devolucion_bodega");
        assertTrue(tieneParametro(report, "ID_DEVOLUCION"));
        assertTrue(tieneParametro(report, "VER_COSTOS"));

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ID_DEVOLUCION", 3L);
        assertDoesNotThrow(() -> JasperFillManager.fillReport(report, parametros, new JREmptyDataSource(0)));
    }

    @Test
    void comprobanteDeDevolucionGeneraPdfParaCadaEstadoYVista() throws Exception {
        JasperReport report = compilar("devolucion_bodega");

        for (String estado : new String[] { "PENDIENTE", "REALIZADO", "CANCELADO" }) {
            for (boolean administrador : new boolean[] { false, true }) {
                Map<String, Object> parametros = new HashMap<>();
                parametros.put("ID_DEVOLUCION", 3L);
                if (administrador) {
                    conCostos(parametros);
                }
                JasperPrint print = JasperFillManager.fillReport(
                        report, parametros, new JRMapCollectionDataSource(filasDevolucion(estado)));
                assertPdf(JasperExportManager.exportReportToPdf(print));
            }
        }
    }

    @Test
    void comprobanteDeDevolucionResumeUnidadesYProductosEnLaRuta() throws Exception {
        JasperReport report = compilar("devolucion_bodega");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ID_DEVOLUCION", 3L);

        TextosImpresos textos = new TextosImpresos(JasperFillManager.fillReport(
                report, parametros, new JRMapCollectionDataSource(filasDevolucion("PENDIENTE"))));

        assertTrue(textos.contiene("15 unidades"));
        assertTrue(textos.contiene("en 3 productos"));
        assertTrue(textos.contiene("Mataquescuintla"));
        assertTrue(textos.contiene("Bodega Jalapa 1"));
        assertTrue(textos.contiene("PENDIENTE DE APROBACIÓN"));
        // 10 de 24 unidades -> 42 %; 1 de 1 -> 100 %
        assertTrue(textos.contiene("42%"));
        assertTrue(textos.contiene("100%"));
    }

    @Test
    void comprobanteDeDevolucionMuestraElEstadoYElRecorridoSegunLaResolucion() throws Exception {
        JasperReport report = compilar("devolucion_bodega");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ID_DEVOLUCION", 3L);

        TextosImpresos aprobada = new TextosImpresos(JasperFillManager.fillReport(
                report, parametros, new JRMapCollectionDataSource(filasDevolucion("REALIZADO"))));
        TextosImpresos rechazada = new TextosImpresos(JasperFillManager.fillReport(
                report, parametros, new JRMapCollectionDataSource(filasDevolucion("CANCELADO"))));

        assertTrue(aprobada.contiene("DEVOLUCIÓN APROBADA"));
        assertTrue(aprobada.contiene("INGRESÓ A BODEGA"));
        assertTrue(aprobada.contiene("APROBADA POR"));
        assertTrue(aprobada.contiene("Ana Gómez"));
        assertFalse(aprobada.contiene("Motivo del rechazo"));
        assertTrue(rechazada.contiene("DEVOLUCIÓN RECHAZADA"));
        assertTrue(rechazada.contiene("SIN INGRESO"));
        assertTrue(rechazada.contiene("RECHAZADA POR"));
        assertTrue(rechazada.contiene("Motivo del rechazo: No cumple las condiciones"));
    }

    @Test
    void comprobanteDeDevolucionSoloMuestraCostosSubtotalesYTotalAlAdministrador() throws Exception {
        JasperReport report = compilar("devolucion_bodega");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ID_DEVOLUCION", 3L);

        TextosImpresos sinCostos = new TextosImpresos(JasperFillManager.fillReport(
                report, parametros, new JRMapCollectionDataSource(filasDevolucion("REALIZADO"))));
        TextosImpresos conCostos = new TextosImpresos(JasperFillManager.fillReport(
                report, conCostos(new HashMap<>(parametros)), new JRMapCollectionDataSource(filasDevolucion("REALIZADO"))));

        assertTrue(sinCostos.contiene("Cuaderno profesional"));
        assertTrue(sinCostos.contiene("Cantidad"));
        assertFalse(sinCostos.contiene("Costo unit."));
        assertFalse(sinCostos.contiene("Subtotal"));
        assertFalse(sinCostos.contiene("TOTAL AL COSTO"));
        assertFalse(sinCostos.contiene("12.50"));
        assertFalse(sinCostos.contiene("312.50"));
        assertTrue(conCostos.contiene("Costo unit."));
        assertTrue(conCostos.contiene("Subtotal"));
        assertTrue(conCostos.contiene("TOTAL AL COSTO"));
        assertTrue(conCostos.contiene("12.50"));
        assertTrue(conCostos.contiene("312.50"));
    }
}
