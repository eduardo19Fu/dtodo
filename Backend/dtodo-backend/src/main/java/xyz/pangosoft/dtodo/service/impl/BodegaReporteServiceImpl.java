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
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.ReportGenerationException;
import xyz.pangosoft.dtodo.util.ConsultaJrxml;
import xyz.pangosoft.dtodo.model.Bodega;
import xyz.pangosoft.dtodo.model.enums.EstadoDespachoBodegaEnum;
import xyz.pangosoft.dtodo.repository.IBodegaRepository;
import xyz.pangosoft.dtodo.repository.IDevolucionBodegaRepository;
import xyz.pangosoft.dtodo.repository.IDespachoBodegaRepository;
import xyz.pangosoft.dtodo.service.IBodegaReporteService;
import xyz.pangosoft.dtodo.util.Utils;

@Service
@RequiredArgsConstructor
@Slf4j
public class BodegaReporteServiceImpl implements IBodegaReporteService {

    static final String PLANTILLA_COMPROBANTE = "/reports/despacho_bodega.jrxml";
    static final String PLANTILLA_COMPROBANTE_DEVOLUCION = "/reports/devolucion_bodega.jrxml";
    static final String PLANTILLA_EXISTENCIAS = "/reports/bodega_existencias.jrxml";
    static final String PLANTILLA_MOVIMIENTOS = "/reports/bodega_movimientos.jrxml";
    static final String PLANTILLA_DESPACHOS = "/reports/bodega_despachos.jrxml";

    private static final DateTimeFormatter FECHA_VISIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DataSource dataSource;
    private final IBodegaRepository bodegaRepository;
    private final IDespachoBodegaRepository despachoRepository;
    private final IDevolucionBodegaRepository devolucionRepository;

    @Override
    public byte[] generarComprobanteDespacho(Long idDespacho) {
        if (!despachoRepository.existsById(idDespacho)) {
            throw new NotFoundException("El despacho " + idDespacho + " no se encuentra registrado en la base de datos");
        }
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ID_DESPACHO", idDespacho);
        return generar(PLANTILLA_COMPROBANTE, parametros, "PDF", null, "el comprobante del despacho " + idDespacho);
    }

    @Override
    public byte[] generarComprobanteDevolucion(Long idDevolucion) {
        if (!devolucionRepository.existsById(idDevolucion)) {
            throw new NotFoundException("La devolución " + idDevolucion + " no se encuentra registrada en la base de datos");
        }
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ID_DEVOLUCION", idDevolucion);
        return generar(PLANTILLA_COMPROBANTE_DEVOLUCION, parametros, "PDF", null,
                "el comprobante de la devolución " + idDevolucion);
    }

    @Override
    public byte[] generarExistencias(Integer idBodega, String formato) {
        Bodega bodega = obtenerBodega(idBodega);
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ID_BODEGA", idBodega);
        parametros.put("BODEGA", etiquetaBodega(bodega));
        return generar(PLANTILLA_EXISTENCIAS, parametros, formato, "Existencias de bodega",
                "el reporte de existencias de la bodega " + idBodega);
    }

    @Override
    public byte[] generarMovimientos(Integer idBodega, LocalDate fechaInicio, LocalDate fechaFin, String formato) {
        Bodega bodega = obtenerBodega(idBodega);
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ID_BODEGA", idBodega);
        parametros.put("BODEGA", etiquetaBodega(bodega));
        agregarRango(parametros, fechaInicio, fechaFin);
        return generar(PLANTILLA_MOVIMIENTOS, parametros, formato, "Movimientos de bodega",
                "el reporte de movimientos de la bodega " + idBodega);
    }

    @Override
    public byte[] generarDespachos(
            Integer idBodega,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            EstadoDespachoBodegaEnum estado,
            String formato) {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ID_BODEGA", idBodega);
        parametros.put("BODEGA", idBodega == null ? "Todas las bodegas" : etiquetaBodega(obtenerBodega(idBodega)));
        parametros.put("ESTADO", estado == null ? null : estado.name());
        parametros.put("ESTADO_ETIQUETA", etiquetaEstado(estado));
        agregarRango(parametros, fechaInicio, fechaFin);
        return generar(PLANTILLA_DESPACHOS, parametros, formato, "Despachos de bodega",
                "el reporte de despachos de bodega");
    }

    private byte[] generar(
            String plantilla,
            Map<String, Object> parametros,
            String formato,
            String nombreHoja,
            String descripcionError) {
        parametros.put("FORMATO", formato);
        // El costo y los totales valorizados solo los ve un administrador
        parametros.put("VER_COSTOS", Utils.esAdministrador());

        try (Connection connection = dataSource.getConnection();
             InputStream template = getClass().getResourceAsStream(plantilla)) {
            if (template == null) {
                throw new ReportGenerationException("No se encontró la plantilla de " + descripcionError + ".", null);
            }

            JasperReport report = JasperCompileManager.compileReport(template);
            if (PLANTILLA_DESPACHOS.equals(plantilla)) {
                return ReportesTabulares.despachos(ConsultaJrxml.filas(report, parametros, connection), parametros, formato);
            }
            if (PLANTILLA_EXISTENCIAS.equals(plantilla)) {
                return ReportesTabulares.existencias(ConsultaJrxml.filas(report, parametros, connection), parametros, formato);
            }
            if (PLANTILLA_MOVIMIENTOS.equals(plantilla)) {
                return ReportesTabulares.movimientos(ConsultaJrxml.filas(report, parametros, connection), parametros, formato);
            }
            JasperPrint print = JasperFillManager.fillReport(report, parametros, connection);
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException | SQLException | IOException exception) {
            log.error("No fue posible generar {}. formato={}", descripcionError, formato, exception);
            throw new ReportGenerationException("No fue posible generar " + descripcionError + ".", exception);
        }
    }

    private Bodega obtenerBodega(Integer idBodega) {
        return bodegaRepository.findById(idBodega).orElseThrow(() ->
                new NotFoundException("La bodega " + idBodega + " no se encuentra registrada en la base de datos"));
    }

    private String etiquetaBodega(Bodega bodega) {
        return bodega.getUbicacion() == null || bodega.getUbicacion().isBlank()
                ? bodega.getNombre()
                : bodega.getNombre() + " · " + bodega.getUbicacion();
    }

    private String etiquetaEstado(EstadoDespachoBodegaEnum estado) {
        if (estado == null) {
            return "Todos los estados";
        }
        switch (estado) {
            case PENDIENTE:
                return "Pendientes de aprobación";
            case REALIZADO:
                return "Realizados";
            default:
                return "Cancelados";
        }
    }

    private void agregarRango(Map<String, Object> parametros, LocalDate fechaInicio, LocalDate fechaFin) {
        parametros.put("FECHA_INICIO", Timestamp.valueOf(fechaInicio.atStartOfDay()));
        parametros.put("FECHA_FIN_EXCLUSIVA", Timestamp.valueOf(fechaFin.plusDays(1).atStartOfDay()));
        parametros.put("RANGO", String.format("Del %s al %s",
                FECHA_VISIBLE.format(fechaInicio), FECHA_VISIBLE.format(fechaFin)));
    }
}
