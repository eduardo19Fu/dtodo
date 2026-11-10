package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/**
 * Ventas entregadas por ruta y vendedor: la plantilla con datos de ejemplo y su consulta real contra H2 en modo MySQL,
 * con entregas parciales, anuladas, fuera del período y de proformas minoristas.
 */
class EntregadoPorRutaVendedorReporteTest {

    private static final String PLANTILLA = "/reports/mayoristas_entregado_ruta_vendedor.jrxml";

    private static JasperReport compilar() throws Exception {
        try (InputStream in = EntregadoPorRutaVendedorReporteTest.class.getResourceAsStream(PLANTILLA)) {
            assertNotNull(in);
            return JasperCompileManager.compileReport(in);
        }
    }

    private static Map<String, Object> parametros(Integer idRuta, Integer idVendedor) {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put(JRParameter.IS_IGNORE_PAGINATION, true);
        parametros.put("ID_RUTA", idRuta);
        parametros.put("RUTA", idRuta == null ? "Todas las rutas" : "RUTA " + idRuta);
        parametros.put("ID_VENDEDOR", idVendedor);
        parametros.put("VENDEDOR", idVendedor == null ? "Todos los vendedores" : "Luis Perez");
        parametros.put("FECHA_INICIO", Timestamp.valueOf("2026-10-01 00:00:00"));
        parametros.put("FECHA_FIN_EXCLUSIVA", Timestamp.valueOf("2026-11-01 00:00:00"));
        parametros.put("RANGO", "Del 01/10/2026 al 31/10/2026");
        parametros.put("FORMATO", "PDF");
        return parametros;
    }

    private static Connection base(String nombre) throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + nombre + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE usuarios (id_usuario INT PRIMARY KEY, primer_nombre VARCHAR(50), apellido VARCHAR(50))");
            statement.execute("CREATE TABLE rutas (id_ruta INT PRIMARY KEY, nombre VARCHAR(50))");
            statement.execute("CREATE TABLE proformas (id_proforma BIGINT PRIMARY KEY, tipo_proforma VARCHAR(20), id_estado INT, "
                    + "id_usuario INT, id_ruta INT)");
            statement.execute("CREATE TABLE proformas_detalle (id_detalle BIGINT PRIMARY KEY, cantidad INT, sub_total_descuento DECIMAL(12,2))");
            statement.execute("CREATE TABLE proformas_entregas (id_entrega BIGINT PRIMARY KEY, id_proforma BIGINT, fecha_entrega DATE, anulada TINYINT)");
            statement.execute("CREATE TABLE proformas_entregas_detalle (id_entrega_detalle BIGINT PRIMARY KEY, id_entrega BIGINT, "
                    + "id_detalle BIGINT, cantidad INT)");
            statement.execute("INSERT INTO usuarios VALUES (1, 'Ana', 'Lopez'), (2, 'Luis', 'Perez')");
            statement.execute("INSERT INTO rutas VALUES (1, 'RUTA 1'), (2, 'RUTA 2')");
            statement.execute("INSERT INTO proformas VALUES "
                    + "(1, 'MAYORISTA', 1, 1, 1), (2, 'MAYORISTA', 1, 2, 1), (3, 'MAYORISTA', 1, 1, 2), (4, 'MINORISTA', 1, 1, 1)");
            statement.execute("INSERT INTO proformas_detalle VALUES (10, 10, 100.00), (20, 5, 25.00), (30, 8, 80.00), (40, 4, 40.00)");
            statement.execute("INSERT INTO proformas_entregas VALUES "
                    + "(1, 1, DATE '2026-10-05', 0), (2, 1, DATE '2026-10-20', 0), (3, 1, DATE '2026-10-21', 1), "
                    + "(4, 2, DATE '2026-10-10', 0), (5, 3, DATE '2026-11-02', 0), (6, 4, DATE '2026-10-12', 0)");
            statement.execute("INSERT INTO proformas_entregas_detalle VALUES "
                    + "(1, 1, 10, 4), (2, 2, 10, 6), (3, 3, 10, 3), (4, 4, 20, 5), (5, 5, 30, 8), (6, 6, 40, 4)");
        }
        return connection;
    }

    @Test
    void sumaSoloLoEntregadoEnElPeriodoDeProformasMayoristasPorRutaYVendedor() throws Exception {
        try (Connection connection = base("entregado_todo")) {
            JasperPrint print = JasperFillManager.fillReport(compilar(), parametros(null, null), connection);
            TextosImpresos textos = new TextosImpresos(print);
            String todo = String.join("|", textos.todos());

            assertTrue(textos.contiene("Ruta: RUTA 1"));
            assertFalse(textos.contiene("Ruta: RUTA 2"), "La entrega de noviembre no cuenta");
            // Ana: pedido 1 entregado en dos partes (4 + 6 unidades = Q 100); la anulada y la minorista no cuentan.
            assertTrue(todo.contains("Ana Lopez|1|10|Q 100.00"), todo);
            assertTrue(todo.contains("Luis Perez|1|5|Q 25.00"), todo);
            // Subtotal de la ruta y total general.
            assertTrue(todo.contains("|2|15|Q 125.00"), todo);
            assertTrue(textos.contiene("Total entregado:"));
            assertTrue(textos.contiene("Del 01/10/2026 al 31/10/2026"));
        }
    }

    @Test
    void unaEntregaParcialSumaSoloLoEntregadoAlPrecioDeLaLinea() throws Exception {
        try (Connection connection = base("entregado_parcial");
             Statement statement = connection.createStatement()) {
            statement.execute("DELETE FROM proformas_entregas_detalle WHERE id_entrega_detalle = 2");

            String todo = String.join("|", new TextosImpresos(
                    JasperFillManager.fillReport(compilar(), parametros(null, null), connection)).todos());

            // Solo las 4 unidades de la primera entrega, a Q10 cada una.
            assertTrue(todo.contains("Ana Lopez|1|4|Q 40.00"), todo);
        }
    }

    @Test
    void filtraPorRutaYPorVendedor() throws Exception {
        try (Connection connection = base("entregado_filtros")) {
            TextosImpresos soloLuis = new TextosImpresos(JasperFillManager.fillReport(compilar(), parametros(null, 2), connection));
            assertTrue(soloLuis.contiene("Luis Perez"));
            assertFalse(soloLuis.contiene("Ana Lopez"));

            TextosImpresos ruta2 = new TextosImpresos(JasperFillManager.fillReport(compilar(), parametros(2, null), connection));
            assertTrue(ruta2.contiene("No hay entregas de pedidos mayoristas en el período seleccionado."));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void laPlantillaSeLlenaConDatosDeEjemplo() throws Exception {
        Map<String, Object> fila = new HashMap<>();
        fila.put("ruta", "RUTA 1");
        fila.put("vendedor", "Ana Lopez");
        fila.put("pedidos", 3L);
        fila.put("unidades", 40L);
        fila.put("valor_entregado", new BigDecimal("1250.50"));

        Map<String, Object> fila2 = new HashMap<>(fila);
        fila2.put("vendedor", "Luis Perez");
        fila2.put("valor_entregado", new BigDecimal("300.00"));
        Map<String, Object> fila3 = new HashMap<>(fila);
        fila3.put("ruta", "RUTA 2");
        JasperPrint impreso = JasperFillManager.fillReport(compilar(), parametros(null, null),
                new JRMapCollectionDataSource(new ArrayList<Map<String, ?>>(Arrays.asList(fila, fila2, fila3))));
        if (System.getProperty("plantilla.salida") != null) {
            java.nio.file.Files.write(new java.io.File(System.getProperty("plantilla.salida"), "entregado.pdf").toPath(),
                    net.sf.jasperreports.engine.JasperExportManager.exportReportToPdf(impreso));
        }
        TextosImpresos textos = new TextosImpresos(impreso);

        assertTrue(textos.contiene("Ventas entregadas por ruta y vendedor"));
        assertTrue(textos.contiene("Subtotal de la ruta:"));
        assertTrue(textos.contiene("Q 1,250.50"));
    }
}
