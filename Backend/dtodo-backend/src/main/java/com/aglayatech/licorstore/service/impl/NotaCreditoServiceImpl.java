package com.aglayatech.licorstore.service.impl;

import com.aglayatech.licorstore.dto.NotaCreditoListDto;
import com.aglayatech.licorstore.error.exceptions.NotFoundException;
import com.aglayatech.licorstore.error.exceptions.ReportGenerationException;
import com.aglayatech.licorstore.model.MovimientoProducto;
import com.aglayatech.licorstore.model.NotaCredito;
import com.aglayatech.licorstore.model.NotaCreditoDetalle;
import com.aglayatech.licorstore.model.Producto;
import com.aglayatech.licorstore.model.Usuario;
import com.aglayatech.licorstore.model.enums.EstadoNotaCreditoEnum;
import com.aglayatech.licorstore.model.enums.TipoMovimientoEnum;
import com.aglayatech.licorstore.repository.INotaCreditoRepository;
import com.aglayatech.licorstore.service.IMovimientoProductoService;
import com.aglayatech.licorstore.service.INotaCreditoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotaCreditoServiceImpl implements INotaCreditoService {

    private final INotaCreditoRepository notaCreditoRepository;
    private final IMovimientoProductoService movimientoProductoService;
    private final DataSource localDataSource;

    @Transactional(readOnly = true)
    @Override
    public List<NotaCreditoListDto> findNotas() {
        String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("Enter {}", __method);

        try {
            List<NotaCreditoListDto> notas = notaCreditoRepository.findAllAsDto();
            log.info("Retornando listado de {} notas de credito registradas", notas.size());
            return notas;
        } catch (DataAccessException e) {
            log.error("Ha ocurrido un error a nivel de base de datos: {}", e.getMessage());
            throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos " + e.getMessage(), e.getCause());
        } finally {
            log.debug("{} Exit", __method);
        }

    }

    @Transactional(readOnly = true)
    @Override
    public List<NotaCreditoListDto> findNotasActivas(EstadoNotaCreditoEnum estado) {
        String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("Enter {}", __method);

        try {
            List<NotaCreditoListDto> notas = notaCreditoRepository.findByEstadoAsDto(estado);
            log.info("Retornando listado de {} notas de credito con estado: {}", notas.size(), estado);
            return notas;
        } catch (DataAccessException e) {
            log.error("Ha ocurrido un error a nivel de base de datos: {}", e.getMessage());
            throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos: " + e.getMessage(), e.getCause());
        } finally {
            log.debug("{} Exit", __method);
        }
    }

    @Override
    public NotaCredito findNota(Long idNota) {
        String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("Enter {}", __method);

        try {
            Optional<NotaCredito> notaCredito = notaCreditoRepository.findById(idNota);
            if(notaCredito.isPresent()) {
                log.info("Retornando Nota de Credito: {}", notaCredito.get().getIdNotaCredito());
                return notaCredito.get();
            } else {
                log.warn("No existen nota de credito registradas con el ID: {}", idNota);
                throw new NotFoundException("No existen nota de credito registradas con el ID: " + idNota);
            }
        } catch (DataAccessException e) {
            log.error("Ha ocurrido un error a nivel de base de datos: {}", e.getMessage());
            throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos: " + e.getMessage(), e.getCause());
        } finally {
            log.debug("{} Exit", __method);
        }
    }

    @Override
    public NotaCredito save(NotaCredito notaCredito, EstadoNotaCreditoEnum estado) {
        String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("Enter {}", __method);

        try {
            NotaCredito notaCreditoSaved = null;
            notaCredito.setEstado(estado);

            switch(notaCredito.getEstado()) {
                case ENTREGA_PENDIENTE:
                    log.info("------> Registrando nota de Credito");
                    notaCreditoSaved = notaCreditoRepository.save(notaCredito);
                    break;
                case ANULADO:
                    log.info("------> Anulando nota de Credito");
                    notaCreditoSaved = notaCreditoRepository.save(notaCredito);
                    break;
                case ENTREGADO:
                    log.info("------> Registrando entrega de productos");
                    for(NotaCreditoDetalle item : notaCredito.getItems()) {
                        log.info("Registrando entrega de producto: {}", item.getProducto().getCodProducto());
                        movimientoProductoService.save(buildMovimiento(item.getProducto(), item.getCantidad(), TipoMovimientoEnum.ENTREGA_PRODUCTO_NOTA, notaCredito.getUsuario()));
                    }

                    log.info("------> Registrando actualización de estado de la nota de credito");
                    notaCreditoSaved = notaCreditoRepository.save(notaCredito);
                    break;
                default:
                    log.warn("********** Operación para Notas de Credito no definida ***********");
                    break;
            }

            return notaCreditoSaved;
        } catch (DataAccessException e) {
            log.error("Ha ocurrido un error a nivel de base de datos al registrar Nota de Credito: {}", e.getMessage());
            throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos: " + e.getMessage(), e.getCause());
        } finally {
            log.debug("{} Exit", __method);
        }
    }

    @Override
    public void delete(Long idNota) {
        notaCreditoRepository.deleteById(idNota);
    }

    @Override
    public byte[] generateReport(Long idNota) {
        try (Connection con = localDataSource.getConnection()) {
            Map<String, Object> params = new HashMap<>();
            InputStream file = getClass().getResourceAsStream("/reports/rpt_nota_credito.jrxml");

            if (file == null) {
                throw new NotFoundException("Archivo de reporte rpt_nota_credito.jrxml no encontrado");
            }

            params.put("idNotaCredito", idNota);

            JasperReport jasperReport = JasperCompileManager.compileReport(file);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, con);

            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (JRException e) {
            log.error("Ha ocurrido un error durante la generación del reporte de nota de crédito: {}", e.getMessage());
            throw new ReportGenerationException(e.getMessage(), e.getCause());
        } catch (SQLException e) {
            log.error("Ha ocurrido un error al intentar ejecutar una instrucción SQL: {}", e.getMessage());
            throw new com.aglayatech.licorstore.error.exceptions.SQLException(e.getMessage(), e.getCause());
        } catch (Exception e) {
            log.error("Ha ocurrido un error inesperado: {}", e.getMessage());
            throw new RuntimeException("Ha ocurrido un error inesperado", e);
        }
    }

    private MovimientoProducto buildMovimiento(Producto producto, int cantidad, TipoMovimientoEnum tipoMovimiento, Usuario usuario) {
        return MovimientoProducto.builder()
                .stockInicial(producto.getStock())
                .cantidad(cantidad)
                .producto(producto)
                .usuario(usuario)
                .tipoMovimiento(tipoMovimiento)
                .build();
    }
}
