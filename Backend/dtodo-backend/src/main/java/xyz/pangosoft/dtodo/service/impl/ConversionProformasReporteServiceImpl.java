package xyz.pangosoft.dtodo.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import xyz.pangosoft.dtodo.error.exceptions.ReportGenerationException;
import xyz.pangosoft.dtodo.repository.ISucursalRepository;
import xyz.pangosoft.dtodo.repository.IUsuarioRepository;
import xyz.pangosoft.dtodo.service.IConversionProformasReporteService;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversionProformasReporteServiceImpl implements IConversionProformasReporteService {

    private static final String PLANTILLA = "/reports/conversion_proformas.jrxml";
    private static final DateTimeFormatter FECHA_VISIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DataSource dataSource;
    private final ISucursalRepository sucursalRepository;
    private final IUsuarioRepository usuarioRepository;

    @Override
    public byte[] generar(Integer idSucursal, LocalDate inicio, LocalDate fin, Integer idUsuario, String formato) {
        try (Connection connection = dataSource.getConnection();
             InputStream template = getClass().getResourceAsStream(PLANTILLA)) {
            if (template == null) {
                throw new ReportGenerationException("No se encontró la plantilla de conversión de proformas.", null);
            }
            JasperReport report = JasperCompileManager.compileReport(template);
            JasperPrint print = JasperFillManager.fillReport(
                    report, parametros(idSucursal, inicio, fin, idUsuario, formato), connection);
            return "XLSX".equals(formato) ? exportarXlsx(print) : JasperExportManager.exportReportToPdf(print);
        } catch (JRException | SQLException | IOException exception) {
            log.error("No fue posible generar conversión de proformas. sucursal={}, usuario={}, formato={}",
                    idSucursal, idUsuario, formato, exception);
            throw new ReportGenerationException("No fue posible generar el reporte de conversión de proformas.", exception);
        }
    }

    private Map<String, Object> parametros(
            Integer idSucursal, LocalDate inicio, LocalDate fin, Integer idUsuario, String formato) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(JRParameter.IS_IGNORE_PAGINATION, "XLSX".equals(formato));
        parameters.put("ID_SUCURSAL", idSucursal);
        parameters.put("ID_USUARIO", idUsuario);
        parameters.put("FECHA_INICIO", Timestamp.valueOf(inicio.atStartOfDay()));
        parameters.put("FECHA_FIN_EXCLUSIVA", Timestamp.valueOf(fin.plusDays(1).atStartOfDay()));
        parameters.put("RANGO", String.format("Del %s al %s", FECHA_VISIBLE.format(inicio), FECHA_VISIBLE.format(fin)));
        parameters.put("SUCURSAL", sucursalRepository.findById(idSucursal)
                .map(sucursal -> sucursal.getNombre()).orElse("Sucursal #" + idSucursal));
        parameters.put("USUARIO", usuarioRepository.findById(idUsuario)
                .map(usuario -> String.format("%s %s", usuario.getPrimerNombre(), usuario.getApellido()).trim())
                .filter(nombre -> !nombre.isBlank()).orElse("Usuario #" + idUsuario));
        parameters.put("FORMATO", formato);
        return parameters;
    }

    private byte[] exportarXlsx(JasperPrint print) throws JRException, IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            JRXlsxExporter exporter = new JRXlsxExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(output));
            SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
            configuration.setDetectCellType(true);
            configuration.setRemoveEmptySpaceBetweenRows(true);
            configuration.setWhitePageBackground(false);
            configuration.setOnePagePerSheet(false);
            configuration.setSheetNames(new String[] { "Conversión de proformas" });
            exporter.setConfiguration(configuration);
            exporter.exportReport();
            return output.toByteArray();
        }
    }
}
