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
import xyz.pangosoft.dtodo.repository.ISucursalRepository;
import xyz.pangosoft.dtodo.service.IValorizacionInventarioReporteService;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValorizacionInventarioReporteServiceImpl implements IValorizacionInventarioReporteService {

    private static final String PLANTILLA = "/reports/valorizacion_inventario.jrxml";
    private static final DateTimeFormatter FECHA_VISIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DataSource dataSource;
    private final ISucursalRepository sucursalRepository;

    @Override
    public byte[] generar(Integer idSucursal, LocalDate fechaCorte, String formato) {
        try (Connection connection = dataSource.getConnection();
             InputStream template = getClass().getResourceAsStream(PLANTILLA)) {
            if (template == null) {
                throw new ReportGenerationException("No se encontró la plantilla de valorización de inventario.", null);
            }

            JasperReport report = JasperCompileManager.compileReport(template);
            Map<String, Object> parametros = crearParametros(idSucursal, fechaCorte, formato);
            return ReportesTabulares.valorizacion(ConsultaJrxml.filas(report, parametros, connection), parametros, formato);
        } catch (JRException | SQLException | IOException exception) {
            log.error("No fue posible generar valorización de inventario. sucursal={}, corte={}, formato={}",
                    idSucursal, fechaCorte, formato, exception);
            throw new ReportGenerationException("No fue posible generar la valorización de inventario.", exception);
        }
    }

    private Map<String, Object> crearParametros(Integer idSucursal, LocalDate fechaCorte, String formato) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ID_SUCURSAL", idSucursal);
        parameters.put("FECHA_CORTE_EXCLUSIVA", Timestamp.valueOf(fechaCorte.plusDays(1).atStartOfDay()));
        parameters.put("FECHA_CORTE", FECHA_VISIBLE.format(fechaCorte));
        parameters.put("SUCURSAL", sucursalRepository.findById(idSucursal)
                .map(sucursal -> sucursal.getNombre())
                .orElse("Sucursal #" + idSucursal));
        parameters.put("FORMATO", formato);
        return parameters;
    }
}
