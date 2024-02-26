package com.aglayatech.licorstore.service;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import com.aglayatech.licorstore.model.TipoFactura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.aglayatech.licorstore.model.Factura;

import net.sf.jasperreports.engine.JRException;

public interface IFacturaService {
	
	public List<Factura> findAll();

	public List<Factura> findAllWithProcedure(Date date1, Date date2);
	
	public Page<Factura> findAll(Pageable pageable);
	
	public Factura findFactura(Long idfactura);

	public Factura findFacturaCorrelativo(Long correlativo);
	
	public Factura save(Factura factura);

	public TipoFactura findTipoFactura(Integer idTipoFactura);

	public Integer totalVentas();

	public List<Factura> facturasPorFecha(Date iniDate, Date endDate);
	
	/********* PDF REPORTS SERVICES ***********/
	
	public byte[] resportDailySales(Integer usuario, Date fecha) throws JRException, FileNotFoundException, SQLException;
	
	public byte[] showBill(Long idfactura) throws JRException, FileNotFoundException, SQLException;

	public byte[] showBill2(Long idfactura) throws JRException, FileNotFoundException, SQLException;

}
