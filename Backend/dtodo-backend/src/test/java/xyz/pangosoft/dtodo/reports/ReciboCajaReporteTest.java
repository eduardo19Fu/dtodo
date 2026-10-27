package xyz.pangosoft.dtodo.reports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

/** El recibo de caja impreso: la plantilla y su consulta real contra H2 en modo MySQL. */
class ReciboCajaReporteTest {

    private static final String PLANTILLA = "/reports/recibo_caja.jrxml";

    private static JasperReport compilar() throws Exception {
        try (InputStream in = ReciboCajaReporteTest.class.getResourceAsStream(PLANTILLA)) {
            assertNotNull(in);
            return JasperCompileManager.compileReport(in);
        }
    }

    private static Connection base(String nombre) throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + nombre + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE sucursales (id_sucursal INT PRIMARY KEY, nombre VARCHAR(80))");
            statement.execute("CREATE TABLE clientes (id_cliente INT PRIMARY KEY, nombre VARCHAR(100), nombre_negocio VARCHAR(150), nit VARCHAR(20))");
            statement.execute("CREATE TABLE usuarios (id_usuario INT PRIMARY KEY, primer_nombre VARCHAR(50), apellido VARCHAR(50))");
            statement.execute("CREATE TABLE proformas (id_proforma BIGINT PRIMARY KEY, no_proforma VARCHAR(30))");
            statement.execute("CREATE TABLE facturas (id_factura BIGINT PRIMARY KEY, no_factura BIGINT)");
            statement.execute("CREATE TABLE recibos_caja (id_recibo BIGINT PRIMARY KEY, id_sucursal INT, numero BIGINT, id_cliente INT, "
                    + "id_proforma BIGINT, id_factura BIGINT, monto DECIMAL(12,2), forma_pago VARCHAR(20), referencia VARCHAR(100), "
                    + "observaciones VARCHAR(255), fecha_pago DATE, id_usuario INT, anulado TINYINT, motivo_anulacion VARCHAR(500))");
            statement.execute("INSERT INTO sucursales VALUES (2, 'Sucursal Centro')");
            statement.execute("INSERT INTO clientes VALUES (5, 'Maria Gomez', 'Libreria Uno', '4514627-6')");
            statement.execute("INSERT INTO usuarios VALUES (1, 'Ana', 'Lopez')");
            statement.execute("INSERT INTO proformas VALUES (30, 'P-0030')");
            statement.execute("INSERT INTO facturas VALUES (40, 1234)");
            statement.execute("INSERT INTO recibos_caja VALUES "
                    + "(1, 2, 15, 5, 30, 40, 1500.50, 'DEPOSITO', 'BANRURAL 99887', 'Abono del mes', DATE '2026-10-20', 1, 0, NULL), "
                    + "(2, 2, 16, 5, NULL, NULL, 200.00, 'EFECTIVO', NULL, NULL, DATE '2026-10-21', 1, 1, 'Monto equivocado')");
        }
        return connection;
    }

    private static JasperPrint imprimir(Connection connection, long idRecibo) throws Exception {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ID_RECIBO", idRecibo);
        return JasperFillManager.fillReport(compilar(), parametros, connection);
    }

    @Test
    void imprimeElReciboConSuNumeroClienteYConcepto() throws Exception {
        try (Connection connection = base("recibo_uno")) {
            String todo = String.join("|", new TextosImpresos(imprimir(connection, 1)).todos());

            assertTrue(todo.contains("RECIBO DE CAJA"), todo);
            assertTrue(todo.contains("No. S2-000015"), todo);
            assertTrue(todo.contains("Sucursal Centro"), todo);
            assertTrue(todo.contains("Q 1,500.50"), todo);
            assertTrue(todo.contains("Maria Gomez · Libreria Uno"), todo);
            assertTrue(todo.contains("NIT: 4514627-6"), todo);
            assertTrue(todo.contains("Pago de pedido P-0030 factura 1234"), todo);
            assertTrue(todo.contains("Depósito"), todo);
            assertTrue(todo.contains("BANRURAL 99887"), todo);
            assertTrue(todo.contains("Emitió: Ana Lopez"), todo);
            assertFalse(todo.contains("RECIBO ANULADO"), todo);
        }
    }

    @Test
    void unAbonoSinDocumentoDiceQueEsALaCuentaYUnAnuladoLoMarca() throws Exception {
        try (Connection connection = base("recibo_dos")) {
            String todo = String.join("|", new TextosImpresos(imprimir(connection, 2)).todos());

            assertTrue(todo.contains("Abono a la cuenta del cliente"), todo);
            assertTrue(todo.contains("Efectivo"), todo);
            assertTrue(todo.contains("RECIBO ANULADO · Monto equivocado"), todo);
        }
    }
}
