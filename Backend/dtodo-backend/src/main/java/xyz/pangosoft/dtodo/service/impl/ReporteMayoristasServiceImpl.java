package xyz.pangosoft.dtodo.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.error.exceptions.ReportGenerationException;
import xyz.pangosoft.dtodo.repository.IRutaRepository;
import xyz.pangosoft.dtodo.repository.IUsuarioRepository;
import xyz.pangosoft.dtodo.service.IReporteMayoristasService;

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
        parametros.put(JRParameter.IS_IGNORE_PAGINATION, "XLSX".equals(formato));
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
            JasperPrint print = JasperFillManager.fillReport(report, parametros, connection);
            return "XLSX".equals(formato) ? exportarXlsx(print, hoja) : JasperExportManager.exportReportToPdf(print);
        } catch (JRException | SQLException | IOException exception) {
            log.error("No fue posible generar el reporte {}. formato={}", plantilla, formato, exception);
            throw new ReportGenerationException("No fue posible generar el reporte de mayoristas.", exception);
        }
    }

    private byte[] exportarXlsx(JasperPrint print, String hoja) throws JRException, IOException {
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
