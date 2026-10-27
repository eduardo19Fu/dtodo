package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

/**
 * El kardex muestra el origen de cada movimiento y el reporte de movimientos separa las salidas por origen. Se
 * ejecuta la consulta real de cada plantilla contra H2 en modo MySQL.
 */
class OrigenMovimientosReportesTest {

    private static JasperPrint llenar(String plantilla, Map<String, Object> parametros, Connection conexion) throws Exception {
        try (InputStream template = OrigenMovimientosReportesTest.class.getResourceAsStream(plantilla)) {
            JasperReport report = JasperCompileManager.compileReport(template);
            return JasperFillManager.fillReport(report, parametros, conexion);
        }
    }

    private static Map<String, Object> parametros() {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put(JRParameter.IS_IGNORE_PAGINATION, true);
        parametros.put("ID_SUCURSAL", 1);
        parametros.put("ID_PRODUCTO", 15);
        parametros.put("FECHA_INICIO", Timestamp.valueOf("2026-09-01 00:00:00"));
        parametros.put("FECHA_FIN_EXCLUSIVA", Timestamp.valueOf("2026-10-01 00:00:00"));
        parametros.put("RANGO", "Del 01/09/2026 al 30/09/2026");
        parametros.put("SUCURSAL", "Sucursal Central");
        parametros.put("PRODUCTO", "75010001 · Cuaderno");
        parametros.put("FORMATO", "XLSX");
        return parametros;
    }

    private static void movimientos(Statement statement) throws Exception {
        statement.execute("CREATE TABLE movimientos_producto (id_movimiento BIGINT PRIMARY KEY, "
                + "fecha_movimiento TIMESTAMP, tipo_movimiento VARCHAR(40), id_usuario INT, "
                + "stock_inicial INT, cantidad INT, id_sucursal INT, id_producto INT, "
                + "tipo_documento_origen VARCHAR(30), id_documento_origen BIGINT)");
        statement.execute("INSERT INTO movimientos_producto VALUES "
                + "(1, TIMESTAMP '2026-09-02 08:00:00', 'ENTRADA', 1, 50, 10, 1, 15, NULL, NULL), "
                + "(2, TIMESTAMP '2026-09-03 08:00:00', 'VENTA', 1, 60, 3, 1, 15, 'FACTURA', 10), "
                + "(3, TIMESTAMP '2026-09-04 08:00:00', 'ENTREGA_PROFORMA', 1, 57, 4, 1, 15, 'ENTREGA_PROFORMA', 77), "
                + "(4, TIMESTAMP '2026-09-05 08:00:00', 'SALIDA', 1, 53, 2, 1, 15, NULL, NULL), "
                + "(5, TIMESTAMP '2026-09-06 08:00:00', 'ANULACION_ENTREGA_PROFORMA', 1, 51, 1, 1, 15, 'ENTREGA_PROFORMA', 77), "
                + "(6, TIMESTAMP '2026-09-07 08:00:00', 'ENTRADA', 1, 52, 5, 1, 15, 'DESPACHO_BODEGA', 4), "
                + "(7, TIMESTAMP '2026-09-08 08:00:00', 'SALIDA', 1, 57, 1, 1, 15, 'DEVOLUCION_BODEGA', 9), "
                + "(8, TIMESTAMP '2026-09-09 08:00:00', 'ENTREGA_PRODUCTO_NOTA', 1, 56, 1, 1, 15, 'NOTA_CREDITO', 30), "
                + "(9, TIMESTAMP '2026-09-10 08:00:00', 'COMPRA', 1, 55, 6, 1, 15, 'COMPRA', 20), "
                + "(10, TIMESTAMP '2026-09-11 08:00:00', 'VENTA', 1, 61, 1, 1, 15, NULL, NULL), "
                + "(11, TIMESTAMP '2026-09-12 08:00:00', '', 1, 60, 1, 1, 15, NULL, NULL)");
    }

    @Test
    void elKardexMuestraElOrigenDeCadaMovimiento() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:kardex_origen;MODE=MySQL;DB_CLOSE_DELAY=-1");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE usuarios (id_usuario INT PRIMARY KEY, usuario VARCHAR(50))");
            statement.execute("CREATE TABLE facturas (id_factura BIGINT PRIMARY KEY, no_factura BIGINT, serie VARCHAR(20))");
            statement.execute("CREATE TABLE compras (id_compra BIGINT PRIMARY KEY, no_comprobante VARCHAR(50))");
            statement.execute("CREATE TABLE notas_credito (id_nota_credito BIGINT PRIMARY KEY)");
            statement.execute("INSERT INTO usuarios VALUES (1, 'caja1')");
            statement.execute("INSERT INTO facturas VALUES (10, 456, 'A')");
            statement.execute("INSERT INTO compras VALUES (20, 'FAC-52')");
            statement.execute("INSERT INTO notas_credito VALUES (30)");
            movimientos(statement);

            TextosImpresos textos = new TextosImpresos(llenar("/reports/kardex_producto.jrxml", parametros(), connection));

            assertTrue(textos.contiene("Origen: Manual"));
            assertTrue(textos.contiene("Origen: Factura"));
            assertTrue(textos.contiene("Origen: Entrega de pedido mayorista"));
            assertTrue(textos.contiene("Origen: Despacho de bodega"));
            assertTrue(textos.contiene("Origen: Devolución a bodega"));
            assertTrue(textos.contiene("Origen: Nota de crédito"));
            assertTrue(textos.contiene("Origen: Compra"));
            assertTrue(textos.contiene("Origen: No registrado"));
            assertTrue(textos.contiene("Documento: Entrega de pedido #77"));
            assertTrue(textos.contiene("Documento: Devolución a bodega #9"));
            assertTrue(textos.contiene("Entrega de pedido"));
            assertTrue(textos.contiene("Anulación de entrega de pedido"));
        }
    }

    @Test
    void laEntregaDePedidoSaleComoSalidaYSuAnulacionComoEntradaEnElSaldoDelKardex() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:kardex_saldo;MODE=MySQL;DB_CLOSE_DELAY=-1");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE usuarios (id_usuario INT PRIMARY KEY, usuario VARCHAR(50))");
            statement.execute("CREATE TABLE facturas (id_factura BIGINT PRIMARY KEY, no_factura BIGINT, serie VARCHAR(20))");
            statement.execute("CREATE TABLE compras (id_compra BIGINT PRIMARY KEY, no_comprobante VARCHAR(50))");
            statement.execute("CREATE TABLE notas_credito (id_nota_credito BIGINT PRIMARY KEY)");
            statement.execute("INSERT INTO usuarios VALUES (1, 'caja1')");
            statement.execute("CREATE TABLE movimientos_producto (id_movimiento BIGINT PRIMARY KEY, "
                    + "fecha_movimiento TIMESTAMP, tipo_movimiento VARCHAR(40), id_usuario INT, "
                    + "stock_inicial INT, cantidad INT, id_sucursal INT, id_producto INT, "
                    + "tipo_documento_origen VARCHAR(30), id_documento_origen BIGINT)");
            statement.execute("INSERT INTO movimientos_producto VALUES "
                    + "(1, TIMESTAMP '2026-09-04 08:00:00', 'ENTREGA_PROFORMA', 1, 100, 4, 1, 15, 'ENTREGA_PROFORMA', 77), "
                    + "(2, TIMESTAMP '2026-09-06 08:00:00', 'ANULACION_ENTREGA_PROFORMA', 1, 96, 4, 1, 15, 'ENTREGA_PROFORMA', 77)");

            TextosImpresos textos = new TextosImpresos(llenar("/reports/kardex_producto.jrxml", parametros(), connection));

            // 100 − 4 = 96 y 96 + 4 = 100
            assertTrue(textos.contiene("96"));
            assertTrue(textos.contiene("100"));
        }
    }

    @Test
    void elReporteDeMovimientosSeparaLasSalidasPorOrigen() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:movimientos_origen;MODE=MySQL;DB_CLOSE_DELAY=-1");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE tipos_producto (id_tipo_producto INT PRIMARY KEY, tipo_producto VARCHAR(50))");
            statement.execute("CREATE TABLE productos (id_producto INT PRIMARY KEY, cod_producto VARCHAR(30), "
                    + "nombre VARCHAR(100), id_tipo_producto INT)");
            statement.execute("CREATE TABLE inventario_sucursal (id_producto INT, id_sucursal INT, stock INT)");
            statement.execute("INSERT INTO tipos_producto VALUES (1, 'PAPELERIA')");
            statement.execute("INSERT INTO productos VALUES (15, 'A-15', 'CUADERNO', 1)");
            statement.execute("INSERT INTO inventario_sucursal VALUES (15, 1, 62)");
            movimientos(statement);

            JasperPrint print = llenar("/reports/movimientos_inventario.jrxml", parametros(), connection);
            TextosImpresos textos = new TextosImpresos(print);

            // Entradas: 10 + 1 (anulación de entrega) + 5 (despacho) + 6 (compra) = 22
            // Salidas: 3 + 1 (facturas) + 4 (entrega) + 2 + 1 (manuales) + 1 (nota) = 12; por origen: 4 / 4 / 4
            // Saldo: 50 + 22 - 12 = 60
            assertTrue(textos.contiene("Por factura"));
            assertTrue(textos.contiene("Por entrega de pedido"));
            assertTrue(textos.contiene("Otras salidas"));
            assertTrue(textos.contiene("CUADERNO"));
            assertEquals(1, print.getPages().size());
            String texto = String.join("|", textos.todos());
            assertTrue(texto.contains("|50|22|12|4|4|4|60|"), texto);
        }
    }
}
