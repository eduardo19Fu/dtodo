package com.aglayatech.licorstore.service.impl;

import com.aglayatech.licorstore.model.Proforma;
import com.aglayatech.licorstore.repository.IProformaRepository;
import com.aglayatech.licorstore.service.IProformaService;
import net.sf.jasperreports.engine.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProformaServiceImpl implements IProformaService {

    @Autowired
    private IProformaRepository proformaRepository;

    @Autowired
    private DataSource dataSource;

    @Override
    public List<Proforma> findAll() {
        return this.proformaRepository.findAll(Sort.by(Direction.DESC, "fechaEmision"));
    }

    @Override
    public Page<Proforma> findAll(Pageable pageable) {
        return this.proformaRepository.findAll(pageable);
    }

    @Override
    public Proforma findProforma(Long idproforma) {
        return this.proformaRepository.findById(idproforma).orElse(null);
    }

    @Override
    public Proforma save(Proforma proforma) {
        return this.proformaRepository.save(proforma);
    }

    @Override
    public void delete(Long id) {
        proformaRepository.deleteById(id);
    }

    @Override
    public List<Proforma> proformasPorFecha(Date iniDate, Date endDate) {
        return proformaRepository.findProformasByFecha(iniDate, endDate);
    }

    @Override
    public byte[] resportDailyProforms(Integer usuario, Date fecha) throws JRException, FileNotFoundException, SQLException {
        return new byte[0];
    }

    @Override
    public byte[] showProforma(Long idproforma) throws JRException, FileNotFoundException, SQLException {
        Connection con = dataSource.getConnection();
        Map<String, Object> params = new HashMap<>();
        params.put("idproforma", idproforma);
        InputStream file = getClass().getResourceAsStream("/reports/proforma.jrxml");

        JasperReport jasperReport = JasperCompileManager.compileReport(file);
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, con);

        ByteArrayOutputStream byteArrayOutputStream = getByteArrayOutputStream(jasperPrint);

        con.close();
        return byteArrayOutputStream.toByteArray();

    }
    protected ByteArrayOutputStream getByteArrayOutputStream(JasperPrint jasperPrint) throws JRException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        JasperExportManager.exportReportToPdfStream(jasperPrint, byteArrayOutputStream);
        return byteArrayOutputStream;
    }

}
