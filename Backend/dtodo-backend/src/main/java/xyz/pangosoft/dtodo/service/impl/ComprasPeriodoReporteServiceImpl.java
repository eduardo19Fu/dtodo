package xyz.pangosoft.dtodo.service.impl;

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
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import xyz.pangosoft.dtodo.error.exceptions.ReportGenerationException;
import xyz.pangosoft.dtodo.util.ConsultaJrxml;
import xyz.pangosoft.dtodo.model.enums.EstadoCompraEnum;
import xyz.pangosoft.dtodo.repository.IProveedorRepository;
import xyz.pangosoft.dtodo.repository.ISucursalRepository;
import xyz.pangosoft.dtodo.service.IComprasPeriodoReporteService;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComprasPeriodoReporteServiceImpl implements IComprasPeriodoReporteService {

    private static final String PLANTILLA = "/reports/compras_periodo.jrxml";
    private static final DateTimeFormatter FECHA_VISIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DataSource dataSource;
    private final ISucursalRepository sucursalRepository;
    private final IProveedorRepository proveedorRepository;

    @Override
    public byte[] generar(
            Integer idSucursal,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer idProveedor,
            EstadoCompraEnum estado,
            String formato) {
        try (Connection connection = dataSource.getConnection();
             InputStream template = getClass().getResourceAsStream(PLANTILLA)) {
            if (template == null) {
                throw new ReportGenerationException(
                        "No se encontró la plantilla de compras por período.", null);
            }

            Map<String, Object> parameters = crearParametros(
                    idSucursal, fechaInicio, fechaFin, idProveedor, estado, formato);
            JasperReport report = JasperCompileManager.compileReport(template);
            return ReportesTabulares.compras(ConsultaJrxml.filas(report, parameters, connection), parameters, formato);
        } catch (JRException | SQLException | IOException exception) {
            log.error("No fue posible generar compras por período. sucursal={}, proveedor={}, formato={}",
                    idSucursal, idProveedor, formato, exception);
            throw new ReportGenerationException(
                    "No fue posible generar el reporte de compras por período.", exception);
        }
    }

    private Map<String, Object> crearParametros(
            Integer idSucursal,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer idProveedor,
            EstadoCompraEnum estado,
            String formato) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ID_SUCURSAL", idSucursal);
        parameters.put("FECHA_INICIO", java.sql.Date.valueOf(fechaInicio));
        parameters.put("FECHA_FIN_EXCLUSIVA", java.sql.Date.valueOf(fechaFin.plusDays(1)));
        parameters.put("RANGO", String.format("Del %s al %s",
                FECHA_VISIBLE.format(fechaInicio), FECHA_VISIBLE.format(fechaFin)));
        parameters.put("SUCURSAL", sucursalRepository.findById(idSucursal)
                .map(sucursal -> sucursal.getNombre())
                .orElse("Sucursal #" + idSucursal));
        parameters.put("ID_PROVEEDOR", idProveedor);
        parameters.put("PROVEEDOR", idProveedor == null
                ? "Todos los proveedores"
                : proveedorRepository.findById(idProveedor)
                        .map(proveedor -> proveedor.getNombre())
                        .orElse("Proveedor #" + idProveedor));
        parameters.put("ESTADO", estado == null ? null : estado.name());
        parameters.put("FILTRO_ESTADO", estado == null ? "Todos los estados" : nombreEstado(estado));
        return parameters;
    }

    private String nombreEstado(EstadoCompraEnum estado) {
        return estado == EstadoCompraEnum.ACTIVA ? "Activas" : "Anuladas";
    }
}
