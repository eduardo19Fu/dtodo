package xyz.pangosoft.dtodo.service.impl;

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
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import xyz.pangosoft.dtodo.error.exceptions.ReportGenerationException;
import xyz.pangosoft.dtodo.util.ConsultaJrxml;
import xyz.pangosoft.dtodo.repository.IClienteRepository;
import xyz.pangosoft.dtodo.repository.ISucursalRepository;
import xyz.pangosoft.dtodo.service.IVentasClienteReporteService;

@Service
@RequiredArgsConstructor
@Slf4j
public class VentasClienteReporteServiceImpl implements IVentasClienteReporteService {

    private static final String PLANTILLA = "/reports/ventas_cliente.jrxml";
    private static final DateTimeFormatter FECHA_VISIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DataSource dataSource;
    private final ISucursalRepository sucursalRepository;
    private final IClienteRepository clienteRepository;

    @Override
    public byte[] generar(
            Integer idSucursal,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer idCliente,
            String formato) {
        try (Connection connection = dataSource.getConnection();
             InputStream template = getClass().getResourceAsStream(PLANTILLA)) {
            if (template == null) {
                throw new ReportGenerationException(
                        "No se encontró la plantilla de ventas por cliente.", null);
            }

            JasperReport report = JasperCompileManager.compileReport(template);
            Map<String, Object> parametros = crearParametros(idSucursal, fechaInicio, fechaFin, idCliente, formato);
            return ReportesTabulares.ventasCliente(ConsultaJrxml.filas(report, parametros, connection), parametros, formato);
        } catch (JRException | SQLException | IOException exception) {
            log.error("No fue posible generar ventas por cliente. sucursal={}, cliente={}",
                    idSucursal, idCliente, exception);
            throw new ReportGenerationException(
                    "No fue posible generar el reporte de ventas por cliente.", exception);
        }
    }

    private Map<String, Object> crearParametros(
            Integer idSucursal,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer idCliente,
            String formato) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ID_SUCURSAL", idSucursal);
        parameters.put("FECHA_INICIO", Timestamp.valueOf(fechaInicio.atStartOfDay()));
        parameters.put("FECHA_FIN_EXCLUSIVA", Timestamp.valueOf(fechaFin.plusDays(1).atStartOfDay()));
        parameters.put("RANGO", String.format("Del %s al %s",
                FECHA_VISIBLE.format(fechaInicio), FECHA_VISIBLE.format(fechaFin)));
        parameters.put("SUCURSAL", sucursalRepository.findById(idSucursal)
                .map(sucursal -> sucursal.getNombre())
                .orElse("Sucursal #" + idSucursal));
        parameters.put("ID_CLIENTE", idCliente);
        parameters.put("CLIENTE", idCliente == null
                ? "Todos los clientes"
                : clienteRepository.findById(idCliente)
                        .map(cliente -> cliente.getNombre())
                        .orElse("Cliente #" + idCliente));
        parameters.put("FORMATO", formato);
        return parameters;
    }
}
