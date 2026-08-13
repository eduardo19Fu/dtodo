package xyz.pangosoft.dtodo.service.impl;

import org.junit.jupiter.api.Test;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.model.DetalleProforma;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.service.IEstadoService;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

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
}
