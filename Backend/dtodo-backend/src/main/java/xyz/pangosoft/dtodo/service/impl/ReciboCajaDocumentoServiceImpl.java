package xyz.pangosoft.dtodo.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
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
import net.sf.jasperreports.engine.JasperReport;
import xyz.pangosoft.dtodo.error.exceptions.ReportGenerationException;
import xyz.pangosoft.dtodo.service.IReciboCajaDocumentoService;
import xyz.pangosoft.dtodo.service.IReciboCajaService;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReciboCajaDocumentoServiceImpl implements IReciboCajaDocumentoService {

    static final String PLANTILLA = "/reports/recibo_caja.jrxml";

    private final DataSource dataSource;
    private final IReciboCajaService reciboService;

    @Override
    public byte[] imprimir(Long idRecibo) {
        // buscar() aplica el alcance por sucursal y responde 404 si el recibo no existe o es de otra sucursal.
        reciboService.buscar(idRecibo);
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ID_RECIBO", idRecibo);
        try (Connection connection = dataSource.getConnection(); InputStream template = getClass().getResourceAsStream(PLANTILLA)) {
            if (template == null) {
                throw new ReportGenerationException("No se encontró la plantilla " + PLANTILLA + ".", null);
            }
            JasperReport report = JasperCompileManager.compileReport(template);
            return JasperExportManager.exportReportToPdf(JasperFillManager.fillReport(report, parametros, connection));
        } catch (JRException | SQLException | IOException exception) {
            log.error("No fue posible generar el recibo {}", idRecibo, exception);
            throw new ReportGenerationException("No fue posible generar el recibo de caja.", exception);
        }
    }
}
