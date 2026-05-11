package com.aglayatech.licorstore.service.impl;

import com.aglayatech.licorstore.dto.DespachoNotaDto;
import com.aglayatech.licorstore.error.exceptions.NotFoundException;
import com.aglayatech.licorstore.error.exceptions.ReportGenerationException;
import com.aglayatech.licorstore.model.DespachoNota;
import com.aglayatech.licorstore.model.NotaCredito;
import com.aglayatech.licorstore.model.NotaCreditoDetalle;
import com.aglayatech.licorstore.model.enums.EstadoNotaCreditoEnum;
import com.aglayatech.licorstore.repository.IDespachoNotaRepository;
import com.aglayatech.licorstore.repository.INotaCreditoRepository;
import com.aglayatech.licorstore.service.IDespachoNotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DespachoNotaServiceImpl implements IDespachoNotaService {

    private final IDespachoNotaRepository despachoNotaRepository;
    private final INotaCreditoRepository notaCreditoRepository;
    private final DataSource localDataSource;

    @Transactional(readOnly = true)
    @Override
    public List<DespachoNotaDto> findByNotaCredito(Long idNotaCredito) {
        return despachoNotaRepository.findByNotaCreditoAsDto(idNotaCredito);
    }

    @Transactional(readOnly = true)
    @Override
    public List<DespachoNotaDto> findByEvento(String idEvento) {
        return despachoNotaRepository.findByIdEventoAsDto(idEvento);
    }

    @Transactional
    @Override
    public List<DespachoNotaDto> registrarDespacho(Long idNotaCredito, List<DespachoNota> despachos) {
        log.info("------> Registrando despacho para nota de credito: {}", idNotaCredito);

        NotaCredito nota = notaCreditoRepository.findById(idNotaCredito)
                .orElseThrow(() -> new NotFoundException("Nota de crédito no encontrada con ID: " + idNotaCredito));

        if (nota.getEstado() != EstadoNotaCreditoEnum.ENTREGA_PENDIENTE) {
            throw new IllegalStateException("Solo se pueden despachar notas con estado ENTREGA_PENDIENTE");
        }

        String idEvento = UUID.randomUUID().toString();
        LocalDateTime fechaEvento = LocalDateTime.now();
        log.info("Asignando idEvento {} para los despachos de la operacion actual", idEvento);

        for (DespachoNota despacho : despachos) {
            int yaDespachadoProd = despachoNotaRepository.totalDespachado(idNotaCredito, despacho.getProducto().getIdProducto());
            int cantidadEnNota = nota.getItems().stream()
                    .filter(item -> item.getProducto().getIdProducto().equals(despacho.getProducto().getIdProducto()))
                    .findFirst()
                    .map(NotaCreditoDetalle::getCantidad)
                    .orElse(0);

            int pendiente = cantidadEnNota - yaDespachadoProd;
            if (despacho.getCantidad() > pendiente) {
                throw new IllegalStateException("La cantidad a despachar del producto " + despacho.getCodProducto()
                        + " excede la cantidad pendiente (" + pendiente + ")");
            }

            despacho.setNotaCredito(nota);
            despacho.setIdEvento(idEvento);
            despacho.setFechaDespacho(fechaEvento);
            despachoNotaRepository.save(despacho);
            log.info("Despacho registrado - Producto: {}, Cantidad: {}", despacho.getCodProducto(), despacho.getCantidad());
        }

        boolean todosCompletos = true;
        for (NotaCreditoDetalle item : nota.getItems()) {
            int totalDesp = despachoNotaRepository.totalDespachado(idNotaCredito, item.getProducto().getIdProducto());
            if (totalDesp < item.getCantidad()) {
                todosCompletos = false;
                break;
            }
        }

        if (todosCompletos) {
            log.info("------> Todos los productos han sido despachados. Cambiando estado a ENTREGADO");
            nota.setEstado(EstadoNotaCreditoEnum.ENTREGADO);
            notaCreditoRepository.save(nota);
        }

        return despachoNotaRepository.findByIdEventoAsDto(idEvento);
    }

    @Transactional(readOnly = true)
    @Override
    public int totalDespachado(Long idNotaCredito, Integer idProducto) {
        return despachoNotaRepository.totalDespachado(idNotaCredito, idProducto);
    }

    @Override
    public byte[] generateComprobanteDespacho(String idEvento) {
        try (Connection con = localDataSource.getConnection()) {
            InputStream file = getClass().getResourceAsStream("/reports/rpt_despacho.jrxml");

            if (file == null) {
                throw new NotFoundException("Archivo de reporte rpt_despacho.jrxml no encontrado");
            }

            Map<String, Object> params = new HashMap<>();
            params.put("idEvento", idEvento);

            JasperReport jasperReport = JasperCompileManager.compileReport(file);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, con);

            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (JRException e) {
            log.error("Ha ocurrido un error durante la generación del comprobante de despacho: {}", e.getMessage());
            throw new ReportGenerationException(e.getMessage(), e.getCause());
        } catch (SQLException e) {
            log.error("Ha ocurrido un error al ejecutar SQL para el comprobante: {}", e.getMessage());
            throw new com.aglayatech.licorstore.error.exceptions.SQLException(e.getMessage(), e.getCause());
        } catch (Exception e) {
            log.error("Error inesperado generando comprobante de despacho: {}", e.getMessage());
            throw new RuntimeException("Ha ocurrido un error inesperado", e);
        }
    }
}
