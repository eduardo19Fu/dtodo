package xyz.pangosoft.dtodo.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
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
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import xyz.pangosoft.dtodo.error.exceptions.ReportGenerationException;
import xyz.pangosoft.dtodo.repository.IProveedorRepository;
import xyz.pangosoft.dtodo.repository.ISucursalRepository;
import xyz.pangosoft.dtodo.service.IComprasProveedorProductoReporteService;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComprasProveedorProductoReporteServiceImpl implements IComprasProveedorProductoReporteService {

    private static final String PLANTILLA = "/reports/compras_proveedor_producto.jrxml";
    private static final DateTimeFormatter FECHA_VISIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DataSource dataSource;
    private final ISucursalRepository sucursalRepository;
    private final IProveedorRepository proveedorRepository;

    @Override
    public byte[] generar(Integer idSucursal, LocalDate fechaInicio, LocalDate fechaFin, Integer idProveedor) {
        try (Connection connection = dataSource.getConnection();
             InputStream template = getClass().getResourceAsStream(PLANTILLA);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (template == null) {
                throw new ReportGenerationException(
                        "No se encontró la plantilla de compras por proveedor o producto.", null);
            }

            Map<String, Object> parameters = crearParametros(idSucursal, fechaInicio, fechaFin, idProveedor);
            JasperReport report = JasperCompileManager.compileReport(template);
            JasperPrint print = JasperFillManager.fillReport(report, parameters, connection);

            JRXlsxExporter exporter = new JRXlsxExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(output));
            SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
            configuration.setDetectCellType(true);
            configuration.setRemoveEmptySpaceBetweenRows(true);
            configuration.setWhitePageBackground(false);
            configuration.setOnePagePerSheet(false);
            configuration.setSheetNames(new String[] { "Compras por proveedor" });
            exporter.setConfiguration(configuration);
            exporter.exportReport();
            return output.toByteArray();
        } catch (JRException | SQLException | IOException exception) {
            log.error("No fue posible generar compras por proveedor o producto. sucursal={}, proveedor={}",
                    idSucursal, idProveedor, exception);
            throw new ReportGenerationException(
                    "No fue posible generar compras por proveedor o producto.", exception);
        }
    }

    private Map<String, Object> crearParametros(
            Integer idSucursal, LocalDate fechaInicio, LocalDate fechaFin, Integer idProveedor) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(JRParameter.IS_IGNORE_PAGINATION, true);
        parameters.put("ID_SUCURSAL", idSucursal);
        parameters.put("ID_PROVEEDOR", idProveedor);
        parameters.put("FECHA_INICIO", java.sql.Date.valueOf(fechaInicio));
        parameters.put("FECHA_FIN_EXCLUSIVA", java.sql.Date.valueOf(fechaFin.plusDays(1)));
        parameters.put("RANGO", String.format("Del %s al %s",
                FECHA_VISIBLE.format(fechaInicio), FECHA_VISIBLE.format(fechaFin)));
        parameters.put("SUCURSAL", sucursalRepository.findById(idSucursal)
                .map(sucursal -> sucursal.getNombre())
                .orElse("Sucursal #" + idSucursal));
        parameters.put("FILTRO_PROVEEDOR", idProveedor == null ? "Todos los proveedores"
                : proveedorRepository.findById(idProveedor)
                        .map(proveedor -> proveedor.getNombre())
                        .orElse("Proveedor #" + idProveedor));
        return parameters;
    }
}
