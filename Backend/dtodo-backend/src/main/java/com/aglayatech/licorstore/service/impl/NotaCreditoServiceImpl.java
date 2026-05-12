package com.aglayatech.licorstore.service.impl;

import com.aglayatech.licorstore.dto.NotaCreditoListDto;
import com.aglayatech.licorstore.error.exceptions.BadRequestException;
import com.aglayatech.licorstore.error.exceptions.DuplicateNotaCreditoException;
import com.aglayatech.licorstore.error.exceptions.NotFoundException;
import com.aglayatech.licorstore.error.exceptions.ReportGenerationException;
import com.aglayatech.licorstore.model.MovimientoProducto;
import com.aglayatech.licorstore.model.NotaCredito;
import com.aglayatech.licorstore.model.NotaCreditoDetalle;
import com.aglayatech.licorstore.model.Producto;
import com.aglayatech.licorstore.model.Usuario;
import com.aglayatech.licorstore.model.enums.EstadoNotaCreditoEnum;
import com.aglayatech.licorstore.model.enums.TipoDocumentoOrigenEnum;
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

    @Transactional
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
                    validarOrigenUnico(notaCredito);
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

    /**
     * Valida que el documento origen (Factura o Proforma) sobre el que se está
     * emitiendo la Nota de Crédito no tenga ya una nota previamente registrada,
     * sin importar el estado en que se encuentre (incluso despachada o anulada).
     *
     * <p>Este check aplica únicamente para notas nuevas; por eso solo se invoca
     * en la transición a {@link EstadoNotaCreditoEnum#ENTREGA_PENDIENTE} cuando
     * la nota aún no posee {@code idNotaCredito}.</p>
     *
     * @param notaCredito nota de crédito recibida desde el controlador
     * @throws DuplicateNotaCreditoException si ya existe una nota para el mismo documento origen
     * @throws BadRequestException           si los datos del origen son inválidos
     */
    private void validarOrigenUnico(NotaCredito notaCredito) {
        // Solo validar al crear (no al actualizar / cambiar estado de una existente)
        if (notaCredito.getIdNotaCredito() != null) {
            return;
        }

        // Por compatibilidad con datos legacy, si no viene el tipo origen se asume FACTURA
        TipoDocumentoOrigenEnum tipoOrigen = notaCredito.getTipoDocumentoOrigen();
        if (tipoOrigen == null) {
            tipoOrigen = TipoDocumentoOrigenEnum.FACTURA;
            notaCredito.setTipoDocumentoOrigen(tipoOrigen);
        }

        switch (tipoOrigen) {
            case FACTURA:
                final String correlativo = notaCredito.getCorrelativoFacturaSat();
                final String serie = notaCredito.getSerieFacturaSat();
                if (correlativo == null || correlativo.isEmpty() || serie == null || serie.isEmpty()) {
                    throw new BadRequestException(
                            "Para crear una Nota de Crédito de tipo FACTURA se requieren correlativo y serie SAT.",
                            null);
                }
                if (notaCreditoRepository.existsByCorrelativoFacturaSatAndSerieFacturaSat(correlativo, serie)) {
                    log.warn("Ya existe una Nota de Credito para la Factura SAT {}-{}", serie, correlativo);
                    throw new DuplicateNotaCreditoException(
                            String.format("Ya existe una Nota de Crédito registrada para la factura %s-%s.", serie, correlativo));
                }
                // Limpiar campos de Proforma para evitar inconsistencias
                notaCredito.setNoProforma(null);
                break;
            case PROFORMA:
                final String noProforma = notaCredito.getNoProforma();
                if (noProforma == null || noProforma.isEmpty()) {
                    throw new BadRequestException(
                            "Para crear una Nota de Crédito de tipo PROFORMA se requiere el número de proforma.",
                            null);
                }
                if (notaCreditoRepository.existsByNoProforma(noProforma)) {
                    log.warn("Ya existe una Nota de Credito para la Proforma {}", noProforma);
                    throw new DuplicateNotaCreditoException(
                            String.format("Ya existe una Nota de Crédito registrada para la proforma %s.", noProforma));
                }
                // Limpiar campos de Factura para evitar inconsistencias
                notaCredito.setCorrelativoFacturaSat(null);
                notaCredito.setSerieFacturaSat(null);
                break;
            default:
                throw new BadRequestException("Tipo de documento origen no soportado: " + tipoOrigen, null);
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
