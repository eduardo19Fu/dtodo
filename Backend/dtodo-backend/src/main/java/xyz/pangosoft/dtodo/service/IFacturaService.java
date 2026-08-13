package xyz.pangosoft.dtodo.service;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import xyz.pangosoft.dtodo.model.TipoFactura;
import xyz.pangosoft.dtodo.dto.FacturaListadoDto;
import xyz.pangosoft.dtodo.dto.DetalleDocumentoDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import xyz.pangosoft.dtodo.model.Factura;

import net.sf.jasperreports.engine.JRException;

public interface IFacturaService {
	
	public List<Factura> findAll();

	public List<Factura> findAllWithProcedure(Date date1, Date date2);
	
	public Page<Factura> findAll(Pageable pageable);

	Page<FacturaListadoDto> findAllListadoDto(String fechaIni, String fechaFin, Pageable pageable);

	Page<FacturaListadoDto> searchListadoDto(String fechaIni, String fechaFin, String filtro, Pageable pageable);

	Page<FacturaListadoDto> findUltimasListadoDto(String filtro, Pageable pageable);

	Page<DetalleDocumentoDto> findDetalleDto(Long idFactura, Pageable pageable);
	
	public Factura findFactura(Long idfactura);

	public Factura findFacturaCorrelativo(Long correlativo);

	public Factura findFacturaByCorrelativoSatAndSerieSat(String correlativoSat, String serieSat);

	public Factura save(Factura factura);
	
	public Factura facturaFel(Factura factura);

	Factura anularFacturaFel(Long idfactura, Integer idusuario);

	public TipoFactura findTipoFactura(Integer idTipoFactura);

	public Integer totalVentas();

	public List<Factura> facturasPorFecha(String iniDate, String endDate);
	
	/********* PDF REPORTS SERVICES ***********/
	
	public byte[] resportDailySales(Integer usuario, String fecha);
	
	public byte[] showBill(Long idfactura) throws JRException, FileNotFoundException, SQLException;

	public byte[] showBill2(Long idfactura) throws JRException, FileNotFoundException, SQLException;

}
