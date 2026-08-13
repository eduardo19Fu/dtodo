package xyz.pangosoft.dtodo.service;

import xyz.pangosoft.dtodo.dto.NotaCreditoListDto;
import xyz.pangosoft.dtodo.model.NotaCredito;
import xyz.pangosoft.dtodo.model.enums.EstadoNotaCreditoEnum;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface INotaCreditoService {

    public List<NotaCreditoListDto> findNotas();

    public List<NotaCreditoListDto> findNotasActivas(EstadoNotaCreditoEnum estado);

    Page<NotaCreditoListDto> findUltimas(String filtro, Pageable pageable);

    Page<NotaCreditoListDto> findPorFechas(String fechaIni, String fechaFin, String filtro, Pageable pageable);

    public NotaCredito findNota(Long idNota);

    public NotaCredito save(NotaCredito notaCredito, EstadoNotaCreditoEnum estado);

    public void delete(Long idNota);

    public byte[] generateReport(Long idNota);
}
