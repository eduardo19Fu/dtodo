package xyz.pangosoft.dtodo.service;

import xyz.pangosoft.dtodo.dto.ProformaFechaDto;
import xyz.pangosoft.dtodo.dto.ProformaDto;
import xyz.pangosoft.dtodo.dto.DetalleDocumentoDto;
import xyz.pangosoft.dtodo.model.Proforma;
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

    Page<ProformaDto> findAllListadoDto(String fechaIni, String fechaFin, Pageable pageable);

    Page<ProformaDto> searchListadoDto(String fechaIni, String fechaFin, String filtro, Pageable pageable);

    Page<ProformaDto> findUltimasListadoDto(String filtro, Pageable pageable);

    Page<DetalleDocumentoDto> findDetalleDto(Long idProforma, Pageable pageable);

    public Proforma findProforma(Long idproforma);

    /**
     * Localiza una Proforma por su número único de proforma.
     *
     * @param noProforma número único asignado a la proforma
     * @return la Proforma encontrada
     */
    Proforma findByNoProforma(String noProforma);

    public Proforma save(Proforma proforma, Long idproforma);

    public void delete(Proforma proforma);

    public List<Proforma> proformasPorFecha(String iniDate, String endDate);

    public List<ProformaFechaDto> proformasPorFechaSp(String iniDate, String endDate);

    // REPORTES PARA PROFORMAS

    public byte[] resportDailyProforms(Integer usuario, Date fecha) throws JRException, FileNotFoundException, SQLException;

    public byte[] showProforma(Long idfactura);

    byte[] proformasExcel(String fechaIni, String fechaFin, boolean todas);

}
