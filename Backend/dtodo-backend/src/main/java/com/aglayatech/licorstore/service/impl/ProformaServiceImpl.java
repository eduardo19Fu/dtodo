package com.aglayatech.licorstore.service.impl;

import com.aglayatech.licorstore.error.exceptions.DataAccessException;
import com.aglayatech.licorstore.error.exceptions.NoContentException;
import com.aglayatech.licorstore.error.exceptions.NotFoundException;
import com.aglayatech.licorstore.error.exceptions.ReportGenerationException;
import com.aglayatech.licorstore.model.Proforma;
import com.aglayatech.licorstore.repository.IProformaRepository;
import com.aglayatech.licorstore.service.IProformaService;
import com.aglayatech.licorstore.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.util.JRLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.TransactionManagementConfigurationSelector;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProformaServiceImpl implements IProformaService {

    private final IProformaRepository proformaRepository;

    private final DataSource dataSource;

    @Transactional(readOnly = true)
    @Override
    public List<Proforma> findAll() {
        String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("Enter {}", __method);

        try {
            List<Proforma> proformas = new ArrayList<>();
            proformas = proformaRepository.findAll(Sort.by(Direction.DESC, "fechaEmision"));

            if(!proformas.isEmpty()) {
                log.info("Devolviendo listado de proformas");
                return proformas;
            } else {
                log.warn("No existen proformas disponibles");
                throw new NoContentException("No existen proformas disponibles");
            }
        } catch (DataAccessException e) {
            log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
            throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
        } catch (Exception e) {
            log.error("Ha ocurrido un error inesperado: {}", e);
            throw new RuntimeException("Ha ocurrido un error inesperado: ", e);
        } finally {
            log.debug("{} Exit", __method);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Proforma> findAll(Pageable pageable) {
        String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("Enter {}", __method);

        try {
            Page<Proforma> proformasPaginadas = proformaRepository.findAll(pageable);

            if(!proformasPaginadas.isEmpty()) {
                log.info("Devolviendo listado de proformas por página: {}", pageable.getPageNumber());
                return proformasPaginadas;
            } else {
                log.info("No existen proformas disponibles");
                throw new NoContentException("No existen proformas disponibles para la página: " + pageable.getPageNumber());
            }
        } catch (DataAccessException e) {
            log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
            throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
        } catch (Exception e) {
            log.error("Ha ocurrido un error inesperado: {}", e);
            throw new RuntimeException("Ha ocurrido un error inesperado: ", e);
        } finally {
            log.debug("{} Exit", __method);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Proforma findProforma(Long idproforma) {
        String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("Enter {}", __method);

        try {
            Proforma proforma = proformaRepository.findById(idproforma).orElse(null);

            if(proforma != null) {
                log.info("Devolviendo proforma: {}", proforma);
                return proforma;
            } else {
                log.warn("No existe proforma disponible con el ID: {}", idproforma);
                throw new NotFoundException("No existe proforma con el ID: " + idproforma);
            }
        } catch (DataAccessException e) {
            log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
            throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
        } catch (Exception e) {
            log.error("Ha ocurrido un error inesperado: {}", e);
            throw new RuntimeException("Ha ocurrido un error inesperado: ", e);
        } finally {
            log.debug("{} Exit", __method);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Proforma save(Proforma proforma) {
        String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("Enter {}", __method);

        Proforma newProforma = null;

        try {
            if(proforma.getIdProforma() == null) {
                log.info("Registrando proforma: {}", proforma);
                newProforma = proformaRepository.save(proforma);
            } else {
                log.info("Actualizando proforma con ID: {}", proforma.getIdProforma());
                newProforma = proformaRepository.save(proforma);
            }
            return newProforma;
        } catch (DataAccessException e) {
            log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
            throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
        } catch (Exception e) {
            log.error("Ha ocurrido un error inesperado: {}", e);
            throw new RuntimeException("Ha ocurrido un error inesperado: ", e);
        } finally {
            log.debug("{} Exit", __method);
        }
    }

    @Transactional(rollbackFor = {Exception.class, DataAccessException.class})
    @Override
    public void delete(Proforma proforma) {
        String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("Enter {}", __method);

        try {
            if(proforma != null) {
                log.info("Eliminando proforma con ID: {}", proforma.getIdProforma());
                proformaRepository.deleteById(proforma.getIdProforma());
            } else {
                log.warn("Proforma a eliminar no se encuentra registrado");
            }
        } catch (DataAccessException e) {
            log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
            throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
        } catch (Exception e) {
            log.error("Ha ocurrido un error inesperado: {}", e);
            throw new RuntimeException("Ha ocurrido un error inesperado: ", e);
        }
    }

    @Override
    public List<Proforma> proformasPorFecha(String iniDate, String endDate) {
        String __method = new Object() {}.getClass().getEnclosingClass().getSimpleName() + "::" + new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug("Enter {}", __method);

        try {
            if(!iniDate.isEmpty() && !endDate.isEmpty()) {
                Date fechaIni = Utils.stringToDate(iniDate);
                Date fechaFin = Utils.stringToDate(endDate);
                // List<Proforma> proformas = proformaRepository.findAllProformas(fechaIni, fechaFin);
                List<Proforma> proformas = proformaRepository.findAllByFechaEmisionBetween(fechaIni, fechaFin);

                if (!proformas.isEmpty()) {
                    log.info("Obteniendo listado de proformas registradas entre {} y {}", iniDate, endDate);
                    return proformas;
                } else {
                    log.warn("No se encuentra registrado ninguna proforma en el rango de fechas {} y {} indicado", iniDate, endDate);
                    throw new NoContentException("No existe ninguna proforma registrada en el rango de fechas indicado");
                }
            } else {
                log.warn("Parámetros de fecha fueron enviados vacío y no puedes ser convertidos.");
                throw new NoContentException("Parametros de fecha fueron enviados vacios");
            }
        } catch (DataAccessException e) {
            log.error("Ha ocurrido un error a nivel de base de datos: {}", e);
            throw new com.aglayatech.licorstore.error.exceptions.DataAccessException("Ha ocurrido un error a nivel de base de datos => ", e);
        } catch (ParseException e) {
            log.error("Ha ocurrido un error al tratar de convertir las fechas especificadas");
            throw new com.aglayatech.licorstore.error.exceptions.ParseException("Ha ocurrido un error al tratar de convertir las fechas especificadas", e.getCause());
        } catch (Exception e) {
            log.error("Ha ocurrido un error inesperado: {}", e);
            throw new RuntimeException("Ha ocurrido un error inesperado: ", e);
        } finally {
            log.debug("{} Exit", __method);
        }
    }

    @Override
    public byte[] resportDailyProforms(Integer usuario, Date fecha) throws JRException, FileNotFoundException, SQLException {
        return new byte[0];
    }

    @Override
    public byte[] showProforma(Long idproforma) {
        try {
            Connection con = dataSource.getConnection();
            Map<String, Object> params = new HashMap<>();
            params.put("proformaId", idproforma);

            InputStream file = this.getClass().getResourceAsStream("/reports/proforma.jrxml");
            if(file == null) {
                log.error("El archivo no se encuentra en la ruta especificada");
                throw new ReportGenerationException("El archivo no se encuentra en la ruta especificada", null);
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(file);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, con);

            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (JRException e) {
            log.error("Ha ocurrido un error durante la generación de la proforma: {}", e.getMessage());
            throw new ReportGenerationException(e.getMessage(), e.getCause());
        } catch (SQLException e) {
            log.error("Ha ocurrido un error al intentar ejecutar una instucción SQL: {}", e.getMessage());
            throw new com.aglayatech.licorstore.error.exceptions.SQLException(e.getMessage(), e.getCause());
        } catch (Exception e) {
            log.error("Ha ocurrido un error inesperado: {}", e);
            throw new RuntimeException("Ha ocurrido un error inesperado: ", e);
        }
    }

}
