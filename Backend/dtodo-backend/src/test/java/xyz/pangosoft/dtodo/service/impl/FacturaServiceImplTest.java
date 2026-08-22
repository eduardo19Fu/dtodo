package xyz.pangosoft.dtodo.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import xyz.pangosoft.dtodo.dto.FacturaDto;
import xyz.pangosoft.dtodo.fel.IFelService;
import xyz.pangosoft.dtodo.repository.IFacturaRepository;
import xyz.pangosoft.dtodo.repository.ITipoFacturaRepository;
import xyz.pangosoft.dtodo.service.ICertificadorService;
import xyz.pangosoft.dtodo.service.ICorrelativoService;
import xyz.pangosoft.dtodo.service.IEmisorService;
import xyz.pangosoft.dtodo.service.IEstadoService;
import xyz.pangosoft.dtodo.service.IMovimientoProductoService;
import xyz.pangosoft.dtodo.service.ITipoFacturaService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FacturaServiceImplTest {

    @Test
    void ordenaLasUltimasFacturasAntesDePaginar() {
        IFacturaRepository repository = mock(IFacturaRepository.class);
        FacturaServiceImpl service = new FacturaServiceImpl(
                repository,
                mock(ITipoFacturaRepository.class),
                mock(IEmisorService.class),
                mock(IEstadoService.class),
                mock(ITipoFacturaService.class),
                mock(ICorrelativoService.class),
                mock(ICertificadorService.class),
                mock(IMovimientoProductoService.class),
                mock(IUsuarioService.class),
                mock(IFelService.class),
                mock(DataSource.class)
        );
        FacturaDto segunda = FacturaDto.builder().noFactura(200L).build();
        FacturaDto primera = FacturaDto.builder().noFactura(100L).build();
        when(repository.findUltimasListadoDto(any()))
                .thenReturn(new ArrayList<>(Arrays.asList(segunda, primera)));

        Page<FacturaDto> resultado = service.findUltimasListadoDto("",
                PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "noFactura")));

        assertEquals(100L, resultado.getContent().get(0).getNoFactura());
        assertEquals(200L, resultado.getContent().get(1).getNoFactura());
    }
}
