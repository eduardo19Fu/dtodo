package com.aglayatech.licorstore.service;

import com.aglayatech.licorstore.model.Proforma;
import net.sf.jasperreports.engine.JRException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public interface IProformaService {

    public List<Proforma> findAll();

    public Page<Proforma> findAll(Pageable pageable);

    public Proforma findProforma(Long idproforma);

    public Proforma save(Proforma proforma);

    public void delete(Long id);

    // REPORTES PARA PROFORMAS

    public byte[] resportDailyProforms(Integer usuario, Date fecha) throws JRException, FileNotFoundException, SQLException;

    public byte[] showProforma(Long idfactura) throws JRException, FileNotFoundException, SQLException;

}
