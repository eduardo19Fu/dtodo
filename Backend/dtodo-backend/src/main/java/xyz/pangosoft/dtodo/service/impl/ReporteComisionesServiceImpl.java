package xyz.pangosoft.dtodo.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

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
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import xyz.pangosoft.dtodo.dto.AvanceComisionDto;
import xyz.pangosoft.dtodo.dto.MetaComisionDto;
import xyz.pangosoft.dtodo.error.exceptions.ReportGenerationException;
import xyz.pangosoft.dtodo.service.IComisionService;
import xyz.pangosoft.dtodo.service.IReporteComisionesService;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReporteComisionesServiceImpl implements IReporteComisionesService {

    static final String PLANTILLA = "/reports/comisiones_anio.jrxml";

    private static final DateTimeFormatter FECHA_VISIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final IComisionService comisionService;

    @Override
    public byte[] comisionesDelAnio(int anio, String formato) {
        String formatoValido = ReporteMayoristasServiceImpl.validarFormato(formato);
        AvanceComisionDto avance = comisionService.avance(anio);
        Map<String, Object> parametros = new HashMap<>();
        parametros.put(JRParameter.IS_IGNORE_PAGINATION, "XLSX".equals(formatoValido));
        parametros.put("ANIO", anio);
        parametros.put("FORMATO", formatoValido);
        parametros.put("ESTADO", estado(avance));
        parametros.put("METAS", metas(avance.getMetas()));
        parametros.put("TOTAL_ENTREGADO", avance.getTotalEntregado());
        parametros.put("TOTAL_DEVUELTO", avance.getTotalDevuelto());
        parametros.put("TOTAL_NETO", avance.getTotalNeto());
        parametros.put("TOTAL_COMISION", avance.getTotalComision());
        parametros.put("LIQUIDADO", avance.isLiquidado());
        return generar(parametros, avance, formatoValido);
    }

    static String estado(AvanceComisionDto avance) {
        if (avance.isLiquidado()) {
            String cuando = avance.getFechaLiquidacion() == null ? "" : " el " + FECHA_VISIBLE.format(avance.getFechaLiquidacion());
            return "Año liquidado" + cuando + (avance.getLiquidadoPor() == null ? "" : " por " + avance.getLiquidadoPor());
        }
        return avance.isAnioTerminado() ? "Año terminado, pendiente de liquidar" : "Avance del año en curso (sin liquidar)";
    }

    static String metas(List<MetaComisionDto> metas) {
        if (metas == null || metas.isEmpty()) {
            return "sin metas definidas";
        }
        DecimalFormat montos = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
        return metas.stream().map(m -> "Q" + montos.format(m.getMontoMinimo()) + " → " + montos.format(m.getPorcentaje()) + " %")
                .collect(Collectors.joining("  ·  "));
    }

    private byte[] generar(Map<String, Object> parametros, AvanceComisionDto avance, String formato) {
        try (InputStream template = getClass().getResourceAsStream(PLANTILLA)) {
            if (template == null) {
                throw new ReportGenerationException("No se encontró la plantilla del reporte " + PLANTILLA + ".", null);
            }
            JasperReport report = JasperCompileManager.compileReport(template);
            JasperPrint print = JasperFillManager.fillReport(report, parametros, new JRBeanCollectionDataSource(avance.getFilas()));
            return "XLSX".equals(formato) ? exportarXlsx(print) : JasperExportManager.exportReportToPdf(print);
        } catch (JRException | IOException exception) {
            log.error("No fue posible generar el reporte de comisiones. formato={}", formato, exception);
            throw new ReportGenerationException("No fue posible generar el reporte de comisiones.", exception);
        }
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
            configuration.setSheetNames(new String[] { "Comisiones" });
            exporter.setConfiguration(configuration);
            exporter.exportReport();
            return output.toByteArray();
        }
    }
}
