package xyz.pangosoft.dtodo.service.impl;

import java.time.LocalDateTime;
import java.util.Arrays;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.repository.IMovimientoProductoRepository;
import xyz.pangosoft.dtodo.service.IEstadoService;
import xyz.pangosoft.dtodo.service.IProductoService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MovimientoProductoServiceImplTest {

    private final IMovimientoProductoRepository repository = mock(IMovimientoProductoRepository.class);
    private final MovimientoProductoServiceImpl service = new MovimientoProductoServiceImpl(
            repository,
            mock(IEstadoService.class),
            mock(IProductoService.class),
            mock(DataSource.class)
    );

    @Test
    void incluyeCompletoElDiaFinalDelRango() {
        PageRequest pageable = PageRequest.of(0, 5);
        when(repository.findListado(any(), any(), any(), any())).thenReturn(Page.empty());

        service.findListado("2026-08-01", "2026-08-05", "", pageable);

        verify(repository).findListado(
                eq(LocalDateTime.of(2026, 8, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 8, 6, 0, 0)),
                eq(""),
                eq(pageable)
        );
    }

    @Test
    void limitaLaConsultaInicialALosUltimosQuinientosMovimientos() {
        PageRequest pageable = PageRequest.of(0, 5);
        when(repository.findUltimosIds(any())).thenReturn(Arrays.asList(10L, 9L, 8L));
        when(repository.findListadoLimitado(any(), any(), any())).thenReturn(Page.empty());

        service.findListado(null, null, "", pageable);

        verify(repository).findUltimosIds(PageRequest.of(0, 500));
        verify(repository).findListadoLimitado(Arrays.asList(10L, 9L, 8L), "", pageable);
    }

    @Test
    void rechazaUnRangoInvertido() {
        assertThrows(BadRequestException.class,
                () -> service.findListado("2026-08-05", "2026-08-01", "", PageRequest.of(0, 5)));

        verifyNoInteractions(repository);
    }
}
