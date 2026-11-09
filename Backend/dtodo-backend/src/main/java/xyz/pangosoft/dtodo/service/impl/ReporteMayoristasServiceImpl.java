package xyz.pangosoft.dtodo.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.error.exceptions.ReportGenerationException;
import xyz.pangosoft.dtodo.repository.IRutaRepository;
import xyz.pangosoft.dtodo.repository.IUsuarioRepository;
import xyz.pangosoft.dtodo.service.IReporteMayoristasService;
import xyz.pangosoft.dtodo.util.ExcelTabla;
import xyz.pangosoft.dtodo.util.ExcelTabla.Columna;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReporteMayoristasServiceImpl implements IReporteMayoristasService {

    static final String PLANTILLA_CLIENTES_RUTA = "/reports/mayoristas_clientes_ruta.jrxml";
    static final String PLANTILLA_SIN_COMPRAS = "/reports/mayoristas_clientes_sin_compras.jrxml";
    static final String PLANTILLA_PROFORMAS_VENDEDOR = "/reports/mayoristas_proformas_vendedor.jrxml";
    static final String PLANTILLA_ENTREGADO = "/reports/mayoristas_entregado_ruta_vendedor.jrxml";

    private static final DateTimeFormatter FECHA_VISIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DataSource dataSource;
    private final IRutaRepository rutaRepository;
    private final IUsuarioRepository usuarioRepository;

    @Override
    public byte[] clientesPorRuta(Integer idRuta, String formato) {
        String formatoValido = validarFormato(formato);
        validarRuta(idRuta);
        Map<String, Object> parametros = parametrosBase(idRuta, formatoValido);
        return generar(PLANTILLA_CLIENTES_RUTA, parametros, formatoValido, "Clientes por ruta");
    }

    @Override
    public byte[] clientesSinCompras(Integer idRuta, String fechaInicio, String fechaFin, String formato) {
        String formatoValido = validarFormato(formato);
        validarRuta(idRuta);
        LocalDate[] rango = validarRango(fechaInicio, fechaFin);
        Map<String, Object> parametros = parametrosBase(idRuta, formatoValido);
        agregarPeriodo(parametros, rango);
        return generar(PLANTILLA_SIN_COMPRAS, parametros, formatoValido, "Clientes sin compras");
    }

    @Override
    public byte[] proformasPorVendedor(Integer idRuta, String fechaInicio, String fechaFin, String formato) {
        String formatoValido = validarFormato(formato);
        validarRuta(idRuta);
        LocalDate[] rango = validarRango(fechaInicio, fechaFin);
        Map<String, Object> parametros = parametrosBase(idRuta, formatoValido);
        agregarPeriodo(parametros, rango);
        return generar(PLANTILLA_PROFORMAS_VENDEDOR, parametros, formatoValido, "Proformas por vendedor");
    }

    @Override
    public byte[] entregadoPorRutaYVendedor(Integer idRuta, Integer idVendedor, String fechaInicio, String fechaFin, String formato) {
        String formatoValido = validarFormato(formato);
        validarRuta(idRuta);
        if (idVendedor != null && idVendedor <= 0) {
            throw new BadRequestException("El vendedor seleccionado no es válido.", null);
        }
        LocalDate[] rango = validarRango(fechaInicio, fechaFin);
        Map<String, Object> parametros = parametrosBase(idRuta, formatoValido);
        agregarPeriodo(parametros, rango);
        parametros.put("ID_VENDEDOR", idVendedor);
        parametros.put("VENDEDOR", idVendedor == null ? "Todos los vendedores"
                : usuarioRepository.findById(idVendedor).map(usuario -> ((usuario.getPrimerNombre() == null ? "" : usuario.getPrimerNombre()) + " "
                        + (usuario.getApellido() == null ? "" : usuario.getApellido())).trim()).orElse("Vendedor #" + idVendedor));
        return generar(PLANTILLA_ENTREGADO, parametros, formatoValido, "Ventas entregadas");
    }

    private Map<String, Object> parametrosBase(Integer idRuta, String formato) {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ID_RUTA", idRuta);
        parametros.put("RUTA", idRuta == null ? "Todas las rutas"
                : rutaRepository.findById(idRuta).map(ruta -> ruta.getNombre()).orElse("Ruta #" + idRuta));
        parametros.put("FORMATO", formato);
        return parametros;
    }

    private void agregarPeriodo(Map<String, Object> parametros, LocalDate[] rango) {
        parametros.put("FECHA_INICIO", Timestamp.valueOf(rango[0].atStartOfDay()));
        parametros.put("FECHA_FIN_EXCLUSIVA", Timestamp.valueOf(rango[1].plusDays(1).atStartOfDay()));
        parametros.put("RANGO", String.format("Del %s al %s", FECHA_VISIBLE.format(rango[0]), FECHA_VISIBLE.format(rango[1])));
    }

    private byte[] generar(String plantilla, Map<String, Object> parametros, String formato, String hoja) {
        try (Connection connection = dataSource.getConnection();
             InputStream template = getClass().getResourceAsStream(plantilla)) {
            if (template == null) {
                throw new ReportGenerationException("No se encontró la plantilla del reporte " + plantilla + ".", null);
            }
            JasperReport report = JasperCompileManager.compileReport(template);
            if ("XLSX".equals(formato)) {
                return tablaExcel(plantilla, report, parametros, connection, hoja);
            }
            JasperPrint print = JasperFillManager.fillReport(report, parametros, connection);
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException | SQLException | IOException exception) {
            log.error("No fue posible generar el reporte {}. formato={}", plantilla, formato, exception);
            throw new ReportGenerationException("No fue posible generar el reporte de mayoristas.", exception);
        }
    }

    private static final Pattern PARAMETRO_SQL = Pattern.compile("\\$P\\{(\\w+)}");

    /**
     * Excel como tabla plana: se ejecuta la misma consulta de la plantilla (así el PDF y el Excel nunca difieren en datos) y se
     * escribe una fila por registro con celdas reales.
     */
    private byte[] tablaExcel(String plantilla, JasperReport report, Map<String, Object> parametros, Connection connection,
            String hoja) throws SQLException {
        String consulta = report.getMainDataset().getQuery().getText();
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
        return excelDe(plantilla, filas, parametros, hoja);
    }

    static byte[] excelDe(String plantilla, List<Map<String, Object>> filas, Map<String, Object> parametros, String hoja) {
        List<String> contexto = new ArrayList<>();
        contexto.add("Ruta: " + parametros.get("RUTA"));
        if (parametros.get("VENDEDOR") != null) {
            contexto.add("Vendedor: " + parametros.get("VENDEDOR"));
        }
        if (parametros.get("RANGO") != null) {
            contexto.add(String.valueOf(parametros.get("RANGO")));
        }
        return ExcelTabla.generar(hoja, hoja, contexto, columnasDe(plantilla), filas);
    }

    private static Columna<Map<String, Object>> texto(String titulo, String campo) {
        return Columna.texto(titulo, fila -> fila.get(campo));
    }

    private static List<Columna<Map<String, Object>>> columnasDe(String plantilla) {
        switch (plantilla) {
            case PLANTILLA_CLIENTES_RUTA:
                return List.of(texto("Ruta", "ruta"), texto("Vendedor", "vendedor"), texto("Código", "codigo"), texto("Negocio", "negocio"),
                        texto("Cliente", "cliente"), texto("Contacto", "contacto"), texto("Teléfono", "telefono"),
                        texto("Dirección", "direccion"), texto("Categoría de descuento", "categoria"),
                        Columna.entero("Días de crédito", fila -> fila.get("dias_credito"), false));
            case PLANTILLA_SIN_COMPRAS:
                return List.of(texto("Ruta", "ruta"), texto("Vendedor", "vendedor"), texto("Código", "codigo"), texto("Negocio", "negocio"),
                        texto("Cliente", "cliente"), texto("Contacto", "contacto"), texto("Teléfono", "telefono"),
                        Columna.fecha("Última compra", fila -> fila.get("ultima_compra")),
                        Columna.entero("Días sin comprar", fila -> fila.get("dias_sin_compra"), false));
            case PLANTILLA_PROFORMAS_VENDEDOR:
                return List.of(texto("Vendedor", "vendedor"),
                        Columna.entero("Proformas generadas", fila -> fila.get("generadas"), true),
                        Columna.dinero("Valor proformado", fila -> fila.get("valor_proformado"), true),
                        Columna.entero("Facturadas", fila -> fila.get("facturadas"), true),
                        Columna.dinero("Valor facturado", fila -> fila.get("valor_facturado"), true),
                        Columna.entero("Por autorizar", fila -> fila.get("por_autorizar"), true),
                        Columna.porcentaje("Conversión", fila -> fila.get("tasa_conversion") == null ? null
                                : new java.math.BigDecimal(String.valueOf(fila.get("tasa_conversion"))).multiply(new java.math.BigDecimal(100))));
            default:
                return List.of(texto("Ruta", "ruta"), texto("Vendedor", "vendedor"),
                        Columna.entero("Pedidos", fila -> fila.get("pedidos"), true),
                        Columna.entero("Unidades", fila -> fila.get("unidades"), true),
                        Columna.dinero("Valor entregado", fila -> fila.get("valor_entregado"), true));
        }
    }

    static String validarFormato(String formato) {
        String valor = formato == null ? "PDF" : formato.toUpperCase();
        if (!"PDF".equals(valor) && !"XLSX".equals(valor)) {
            throw new BadRequestException("El formato solicitado no es válido.", null);
        }
        return valor;
    }

    static void validarRuta(Integer idRuta) {
        if (idRuta != null && idRuta <= 0) {
            throw new BadRequestException("La ruta seleccionada no es válida.", null);
        }
    }

    static LocalDate[] validarRango(String fechaInicio, String fechaFin) {
        if (fechaInicio == null || fechaFin == null || fechaInicio.isBlank() || fechaFin.isBlank()) {
            throw new BadRequestException("Debes indicar la fecha de inicio y la fecha final.", null);
        }
        try {
            LocalDate inicio = LocalDate.parse(fechaInicio);
            LocalDate fin = LocalDate.parse(fechaFin);
            if (fin.isBefore(inicio)) {
                throw new BadRequestException("La fecha final no puede ser anterior a la fecha de inicio.", null);
            }
            return new LocalDate[] { inicio, fin };
        } catch (DateTimeParseException exception) {
            throw new BadRequestException("Las fechas deben utilizar el formato yyyy-MM-dd.", exception);
        }
    }
}
