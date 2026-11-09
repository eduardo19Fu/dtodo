package xyz.pangosoft.dtodo.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import xyz.pangosoft.dtodo.dto.CobranzaClienteDto;
import xyz.pangosoft.dtodo.dto.CobranzaDto;
import xyz.pangosoft.dtodo.dto.MovimientoEstadoCuentaDto;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.ReportGenerationException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.repository.IClienteRepository;
import xyz.pangosoft.dtodo.repository.IRutaRepository;
import xyz.pangosoft.dtodo.service.ICobranzaService;
import xyz.pangosoft.dtodo.service.IReporteCobranzaService;
import xyz.pangosoft.dtodo.util.ExcelTabla;
import xyz.pangosoft.dtodo.util.ExcelTabla.Columna;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReporteCobranzaServiceImpl implements IReporteCobranzaService {

    static final String PLANTILLA_SALDOS = "/reports/cobranza_saldos.jrxml";
    static final String PLANTILLA_ESTADO_CUENTA = "/reports/estado_cuenta_cliente.jrxml";

    private final ICobranzaService cobranzaService;
    private final IClienteRepository clienteRepository;
    private final IRutaRepository rutaRepository;

    @Override
    public byte[] cuentasPorCobrar(Integer idRuta, boolean soloVencidos, String formato) {
        String formatoValido = ReporteMayoristasServiceImpl.validarFormato(formato);
        ReporteMayoristasServiceImpl.validarRuta(idRuta);
        CobranzaDto cobranza = cobranzaService.cuentasPorCobrar(idRuta, soloVencidos);
        String ruta = idRuta == null ? "todas las rutas"
                : rutaRepository.findById(idRuta).map(r -> r.getNombre()).orElse("ruta " + idRuta);
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("FORMATO", formatoValido);
        parametros.put("FECHA", CobranzaServiceImpl.FECHA_VISIBLE.format(cobranza.getFecha()));
        parametros.put("FILTRO", ruta + (soloVencidos ? " · solo con saldo vencido" : ""));
        parametros.put("TOTAL_POR_VENCER", cobranza.getPorVencer());
        parametros.put("TOTAL_1_30", cobranza.getVencido1a30());
        parametros.put("TOTAL_31_60", cobranza.getVencido31a60());
        parametros.put("TOTAL_MAS_60", cobranza.getVencidoMasDe60());
        parametros.put("TOTAL_VENCIDO", cobranza.getVencido());
        parametros.put("TOTAL", cobranza.getTotal());
        if ("XLSX".equals(formatoValido)) {
            return ExcelTabla.generar("Cuentas por cobrar", "Cuentas por cobrar",
                    List.of("Ruta: " + parametros.get("FILTRO"), "Al " + parametros.get("FECHA")), columnasSaldos(), cobranza.getClientes());
        }
        return generar(PLANTILLA_SALDOS, parametros, new JRBeanCollectionDataSource(cobranza.getClientes()), formatoValido,
                "Cuentas por cobrar");
    }

    static List<Columna<CobranzaClienteDto>> columnasSaldos() {
        return List.of(
                Columna.texto("Cliente", CobranzaClienteDto::getCliente),
                Columna.texto("Negocio", CobranzaClienteDto::getNegocio),
                Columna.texto("Ruta", CobranzaClienteDto::getRuta),
                Columna.dinero("Límite de crédito", CobranzaClienteDto::getLimiteCredito, false),
                Columna.dinero("Por vencer", CobranzaClienteDto::getPorVencer, true),
                Columna.dinero("Vencido 1-30 días", CobranzaClienteDto::getVencido1a30, true),
                Columna.dinero("Vencido 31-60 días", CobranzaClienteDto::getVencido31a60, true),
                Columna.dinero("Vencido más de 60 días", CobranzaClienteDto::getVencidoMasDe60, true),
                Columna.dinero("Total vencido", CobranzaClienteDto::getVencido, true),
                Columna.dinero("Total adeudado", CobranzaClienteDto::getTotal, true),
                Columna.entero("Días de atraso", CobranzaClienteDto::getDiasMaxAtraso, false));
    }

    @Override
    public byte[] estadoDeCuenta(Integer idCliente) {
        if (idCliente == null || idCliente <= 0) {
            throw new BadRequestException("El cliente seleccionado no es válido.", null);
        }
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new NotFoundException("El cliente solicitado no se encuentra registrado."));
        List<MovimientoEstadoCuentaDto> movimientos = cobranzaService.estadoDeCuenta(idCliente);
        BigDecimal saldo = movimientos.isEmpty() ? BigDecimal.ZERO : movimientos.get(movimientos.size() - 1).getSaldo();
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("CLIENTE", cliente.getNombre() + (cliente.getNombreNegocio() == null ? "" : " · " + cliente.getNombreNegocio()));
        parametros.put("NIT", cliente.getNit());
        parametros.put("CODIGO", cliente.getCodigoCliente());
        parametros.put("LIMITE", cliente.getLimiteCredito() == null ? "sin límite" : "Q" + cliente.getLimiteCredito().setScale(2));
        parametros.put("DIAS_CREDITO", cliente.getDiasCredito() == null ? "sin crédito" : cliente.getDiasCredito() + " días");
        parametros.put("FECHA", CobranzaServiceImpl.FECHA_VISIBLE.format(LocalDate.now()));
        parametros.put("SALDO", saldo);
        return generar(PLANTILLA_ESTADO_CUENTA, parametros, new JRBeanCollectionDataSource(movimientos), "PDF", "Estado de cuenta");
    }

    private byte[] generar(String plantilla, Map<String, Object> parametros, JRDataSource datos, String formato, String hoja) {
        try (InputStream template = getClass().getResourceAsStream(plantilla)) {
            if (template == null) {
                throw new ReportGenerationException("No se encontró la plantilla del reporte " + plantilla + ".", null);
            }
            JasperReport report = JasperCompileManager.compileReport(template);
            JasperPrint print = JasperFillManager.fillReport(report, parametros, datos);
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException | IOException exception) {
            log.error("No fue posible generar el reporte {}. formato={}", plantilla, formato, exception);
            throw new ReportGenerationException("No fue posible generar el reporte de cobranza.", exception);
        }
    }
}
