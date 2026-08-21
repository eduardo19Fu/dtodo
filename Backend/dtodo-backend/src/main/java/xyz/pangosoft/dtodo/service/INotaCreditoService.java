package xyz.pangosoft.dtodo.service;

import xyz.pangosoft.dtodo.dto.NotaCreditoDto;
import xyz.pangosoft.dtodo.dto.NotaCreditoDetalleDto;
import xyz.pangosoft.dtodo.model.NotaCredito;
import xyz.pangosoft.dtodo.model.enums.EstadoNotaCreditoEnum;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface INotaCreditoService {

    public List<NotaCreditoDto> findNotas();

    public List<NotaCreditoDto> findNotasActivas(EstadoNotaCreditoEnum estado);

    Page<NotaCreditoDto> findUltimas(String filtro, Pageable pageable);

    Page<NotaCreditoDto> findPorFechas(String fechaIni, String fechaFin, String filtro, Pageable pageable);

    public NotaCredito findNota(Long idNota);

    NotaCreditoDetalleDto findDetalle(Long idNota);

    public NotaCredito save(NotaCredito notaCredito, EstadoNotaCreditoEnum estado);

    public void delete(Long idNota);

    public byte[] generateReport(Long idNota);
}
