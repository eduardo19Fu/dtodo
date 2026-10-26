package xyz.pangosoft.dtodo.service.impl;

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
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperReport;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.ReportGenerationException;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.repository.IRutaRepository;
import xyz.pangosoft.dtodo.service.IPedidoDocumentoService;

@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoDocumentoServiceImpl implements IPedidoDocumentoService {

    static final String PLANTILLA_BODEGA = "/reports/pedido_bodega.jrxml";
    static final String PLANTILLA_NOTA_ENVIO = "/reports/nota_envio.jrxml";

    private static final DateTimeFormatter FECHA_VISIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DataSource dataSource;
    private final IProformaRepository proformaRepository;
    private final IRutaRepository rutaRepository;

    @Override
    public byte[] picking(Long idProforma) {
        return documentoDeBodega(idProforma, "PICKING");
    }

    @Override
    public byte[] empaque(Long idProforma) {
        return documentoDeBodega(idProforma, "EMPAQUE");
    }

    @Override
    public byte[] notaEnvio(Integer idRuta, String fecha) {
        if (idRuta == null || idRuta <= 0) {
            throw new BadRequestException("Debes indicar la ruta de la nota de envío.", null);
        }
        LocalDate dia = parsearFecha(fecha);
        String ruta = rutaRepository.findById(idRuta).map(r -> r.getNombre())
                .orElseThrow(() -> new NotFoundException("La ruta solicitada no se encuentra registrada."));
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ID_RUTA", idRuta);
        parametros.put("RUTA", ruta);
        parametros.put("FECHA", FECHA_VISIBLE.format(dia));
        parametros.put("FECHA_INICIO", Timestamp.valueOf(dia.atStartOfDay()));
        parametros.put("FECHA_FIN_EXCLUSIVA", Timestamp.valueOf(dia.plusDays(1).atStartOfDay()));
        return generar(PLANTILLA_NOTA_ENVIO, parametros);
    }

    private byte[] documentoDeBodega(Long idProforma, String documento) {
        Proforma proforma = proformaRepository.findById(idProforma).orElse(null);
        if (proforma == null || proforma.getTipoProforma() != TipoProformaEnum.MAYORISTA || proforma.getEstadoPedido() == null) {
            throw new NotFoundException("El pedido solicitado no se encuentra registrado.");
        }
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ID_PROFORMA", idProforma);
        parametros.put("DOCUMENTO", documento);
        return generar(PLANTILLA_BODEGA, parametros);
    }

    private byte[] generar(String plantilla, Map<String, Object> parametros) {
        try (Connection connection = dataSource.getConnection();
             InputStream template = getClass().getResourceAsStream(plantilla)) {
            if (template == null) {
                throw new ReportGenerationException("No se encontró la plantilla " + plantilla + ".", null);
            }
            JasperReport report = JasperCompileManager.compileReport(template);
            return JasperExportManager.exportReportToPdf(JasperFillManager.fillReport(report, parametros, connection));
        } catch (JRException | SQLException | java.io.IOException exception) {
            log.error("No fue posible generar el documento {}", plantilla, exception);
            throw new ReportGenerationException("No fue posible generar el documento del pedido.", exception);
        }
    }

    static LocalDate parsearFecha(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(fecha);
        } catch (DateTimeParseException exception) {
            throw new BadRequestException("La fecha debe utilizar el formato yyyy-MM-dd.", exception);
        }
    }
}
