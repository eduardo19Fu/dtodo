package xyz.pangosoft.dtodo.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import xyz.pangosoft.dtodo.dto.ProformaDto;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.model.DetalleProforma;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.service.IEstadoService;

import javax.sql.DataSource;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProformaServiceImplTest {

    private final IProformaRepository proformaRepository = mock(IProformaRepository.class);
    private final ProformaServiceImpl service = new ProformaServiceImpl(
            proformaRepository,
            mock(IEstadoService.class),
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
        when(proformaRepository.findUltimasListadoDto(any()))
                .thenReturn(new ArrayList<>(Arrays.asList(segunda, primera)));

        Page<ProformaDto> resultado = service.findUltimasListadoDto("",
                PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "noProforma")));

        assertEquals("100P", resultado.getContent().get(0).getNoProforma());
        assertEquals("200P", resultado.getContent().get(1).getNoProforma());
    }
}
