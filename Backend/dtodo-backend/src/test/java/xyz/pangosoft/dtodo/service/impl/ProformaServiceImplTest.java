package xyz.pangosoft.dtodo.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import xyz.pangosoft.dtodo.dto.ProformaDto;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.model.DetalleProforma;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import java.time.LocalDate;
import java.util.Optional;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.service.IEstadoService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

import javax.sql.DataSource;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProformaServiceImplTest {

    private final IProformaRepository proformaRepository = mock(IProformaRepository.class);
    private final ProformaServiceImpl service = new ProformaServiceImpl(
            proformaRepository,
            mock(IEstadoService.class),
            mock(IUsuarioService.class),
            mock(DataSource.class)
    );

    @Test
    void noGuardaProformaConCantidadNula() {
        Proforma proforma = new Proforma();
        proforma.getItemsProforma().add(DetalleProforma.builder().cantidad(null).build());

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.save(proforma, null)
        );

        assertTrue(exception.getMessage().contains("cantidad"));
        verifyNoInteractions(proformaRepository);
    }

    @Test
    void noGuardaProformaConCantidadCero() {
        Proforma proforma = new Proforma();
        proforma.getItemsProforma().add(DetalleProforma.builder().cantidad(0).build());

        assertThrows(BadRequestException.class, () -> service.save(proforma, null));
        verifyNoInteractions(proformaRepository);
    }

    @Test
    void ordenaLasUltimasProformasSinCambiarElConjuntoConsultado() {
        ProformaDto segunda = ProformaDto.builder().noProforma("200P").build();
        ProformaDto primera = ProformaDto.builder().noProforma("100P").build();
        when(proformaRepository.findUltimasListadoDto(any(), any()))
                .thenReturn(new ArrayList<>(Arrays.asList(segunda, primera)));

        Page<ProformaDto> resultado = service.findUltimasListadoDto("", null,
                PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "noProforma")));

        assertEquals("100P", resultado.getContent().get(0).getNoProforma());
        assertEquals("200P", resultado.getContent().get(1).getNoProforma());
    }

    @Test
    void propagaElIdUsuarioAlListadoPaginadoParaUnUsuarioNoAdmin() {
        PageRequest pageable = PageRequest.of(0, 5);
        when(proformaRepository.findAllListadoDto(any(), any(), eq(9), eq(pageable))).thenReturn(Page.empty());

        service.findAllListadoDto("2026-08-01", "2026-08-05", 9, pageable);

        verify(proformaRepository).findAllListadoDto(any(), any(), eq(9), eq(pageable));
    }

    @Test
    void rechazaUsuarioInvalidoParaExportarProformas() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.proformasExcel(null, null, true, -1));

        assertTrue(exception.getMessage().contains("usuario"));
    }

    @Test
    void permiteExportarProformasSinUsuarioEspecificoYExigeRangoDeFechas() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.proformasExcel(null, null, false, null));

        assertTrue(exception.getMessage().contains("fecha"));
    }

    @Test
    void totalProformasRespetaElUsuarioIndicado() {
        when(proformaRepository.countByUsuario(12)).thenReturn(4L);

        Long total = service.totalProformas(12);

        assertEquals(4L, total);
        verify(proformaRepository).countByUsuario(12);
    }

    @Test
    void totalProformasSinUsuarioDevuelveElConteoGlobal() {
        when(proformaRepository.countByUsuario(null)).thenReturn(25L);

        Long total = service.totalProformas(null);

        assertEquals(25L, total);
        verify(proformaRepository).countByUsuario(null);
    }

    @Test
    void laProformaMayoristaNuevaVenceALos15Dias() {
        when(proformaRepository.save(any(Proforma.class))).thenAnswer(inv -> inv.getArgument(0));
        Proforma proforma = new Proforma();
        proforma.setTipoProforma(TipoProformaEnum.MAYORISTA);

        Proforma guardada = service.save(proforma, null);

        assertEquals(LocalDate.now().plusDays(15), guardada.getFechaVencimiento());
    }

    @Test
    void laProformaMinoristaNuevaNoTieneVencimientoYQuedaComoMinorista() {
        when(proformaRepository.save(any(Proforma.class))).thenAnswer(inv -> inv.getArgument(0));

        Proforma guardada = service.save(new Proforma(), null);

        assertEquals(TipoProformaEnum.MINORISTA, guardada.getTipoProforma());
        assertNull(guardada.getFechaVencimiento());
    }

    @Test
    void editarUnaMayoristaConservaElVencimientoYActualizaObservacionesYEntrega() {
        Proforma existente = new Proforma();
        existente.setIdProforma(7L);
        existente.setTipoProforma(TipoProformaEnum.MAYORISTA);
        existente.setFechaVencimiento(LocalDate.now().plusDays(3));
        when(proformaRepository.findById(7L)).thenReturn(Optional.of(existente));
        when(proformaRepository.save(any(Proforma.class))).thenAnswer(inv -> inv.getArgument(0));
        Proforma cambios = new Proforma();
        cambios.setTipoProforma(TipoProformaEnum.MAYORISTA);
        cambios.setObservaciones("entregar temprano");
        cambios.setFechaEntregaEstimada(LocalDate.now().plusDays(5));

        Proforma guardada = service.save(cambios, 7L);

        assertEquals(LocalDate.now().plusDays(3), guardada.getFechaVencimiento());
        assertEquals("entregar temprano", guardada.getObservaciones());
        assertEquals(LocalDate.now().plusDays(5), guardada.getFechaEntregaEstimada());
    }

    @Test
    void editarRecalculaLaAutorizacionPeroConservaElVencimientoAutorizado() {
        Proforma existente = new Proforma();
        existente.setIdProforma(7L);
        existente.setTipoProforma(TipoProformaEnum.MAYORISTA);
        existente.setEstadoAutorizacion(EstadoAutorizacionEnum.APROBADA);
        existente.setVencimientoAutorizado(true);
        when(proformaRepository.findById(7L)).thenReturn(Optional.of(existente));
        when(proformaRepository.save(any(Proforma.class))).thenAnswer(inv -> inv.getArgument(0));
        Proforma cambios = new Proforma();
        cambios.setTipoProforma(TipoProformaEnum.MAYORISTA);
        cambios.setEstadoAutorizacion(EstadoAutorizacionEnum.PENDIENTE);

        Proforma guardada = service.save(cambios, 7L);

        assertEquals(EstadoAutorizacionEnum.PENDIENTE, guardada.getEstadoAutorizacion());
        assertTrue(guardada.isVencimientoAutorizado());
    }
}
