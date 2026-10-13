package xyz.pangosoft.dtodo.service.impl;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.ReportGenerationException;
import xyz.pangosoft.dtodo.model.Bodega;
import xyz.pangosoft.dtodo.model.enums.EstadoDespachoBodegaEnum;
import xyz.pangosoft.dtodo.repository.IBodegaRepository;
import xyz.pangosoft.dtodo.repository.IDespachoBodegaRepository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BodegaReporteServiceImplTest {

    private final DataSource dataSource = mock(DataSource.class);
    private final IBodegaRepository bodegaRepository = mock(IBodegaRepository.class);
    private final IDespachoBodegaRepository despachoRepository = mock(IDespachoBodegaRepository.class);

    private final BodegaReporteServiceImpl service =
            new BodegaReporteServiceImpl(dataSource, bodegaRepository, despachoRepository);

    private static final LocalDate INICIO = LocalDate.of(2026, 10, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 10, 2);

    @Test
    void elComprobanteDeUnDespachoInexistenteLanzaNotFoundSinAbrirConexion() throws Exception {
        when(despachoRepository.existsById(404L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.generarComprobanteDespacho(404L));

        verify(dataSource, never()).getConnection();
    }

    @Test
    void lasExistenciasDeUnaBodegaInexistenteLanzanNotFound() {
        when(bodegaRepository.findById(9)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.generarExistencias(9, "PDF"));
    }

    @Test
    void losMovimientosDeUnaBodegaInexistenteLanzanNotFound() {
        when(bodegaRepository.findById(9)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.generarMovimientos(9, INICIO, FIN, "PDF"));
    }

    @Test
    void losDespachosDeUnaBodegaIndicadaPeroInexistenteLanzanNotFound() {
        when(bodegaRepository.findById(9)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.generarDespachos(9, INICIO, FIN, EstadoDespachoBodegaEnum.PENDIENTE, "PDF"));
    }

    @Test
    void unaFalloDeConexionSeReportaComoErrorDeGeneracionDeReporte() throws Exception {
        when(bodegaRepository.findById(1)).thenReturn(Optional.of(Bodega.builder().idBodega(1).nombre("Principal").build()));
        when(dataSource.getConnection()).thenThrow(new SQLException("sin conexión"));

        assertThrows(ReportGenerationException.class, () -> service.generarExistencias(1, "XLSX"));
    }
}
