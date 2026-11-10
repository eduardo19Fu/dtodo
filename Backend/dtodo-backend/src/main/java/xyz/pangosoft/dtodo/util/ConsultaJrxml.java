package xyz.pangosoft.dtodo.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jasperreports.engine.JasperReport;

/**
 * Ejecuta la consulta SQL que trae una plantilla de Jasper, con sus parámetros {@code $P{...}}, y devuelve las filas como mapas
 * (clave: el alias de la columna en minúsculas). Así el PDF y el Excel de un reporte salen de la misma consulta sin repetirla.
 */
public final class ConsultaJrxml {

    private static final Pattern PARAMETRO_SQL = Pattern.compile("\\$P\\{(\\w+)}");

    private ConsultaJrxml() {
    }

    public static List<Map<String, Object>> filas(JasperReport reporte, Map<String, Object> parametros, Connection connection)
            throws SQLException {
        String consulta = reporte.getMainDataset().getQuery().getText();
        List<String> orden = new ArrayList<>();
        Matcher matcher = PARAMETRO_SQL.matcher(consulta);
        while (matcher.find()) {
            orden.add(matcher.group(1));
        }
        List<Map<String, Object>> filas = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(matcher.replaceAll("?"))) {
            for (int i = 0; i < orden.size(); i++) {
                statement.setObject(i + 1, parametros.get(orden.get(i)));
            }
            try (ResultSet resultado = statement.executeQuery()) {
                ResultSetMetaData meta = resultado.getMetaData();
                while (resultado.next()) {
                    Map<String, Object> fila = new LinkedHashMap<>();
                    for (int c = 1; c <= meta.getColumnCount(); c++) {
                        fila.put(meta.getColumnLabel(c).toLowerCase(), resultado.getObject(c));
                    }
                    filas.add(fila);
                }
            }
        }
        return filas;
    }
}
