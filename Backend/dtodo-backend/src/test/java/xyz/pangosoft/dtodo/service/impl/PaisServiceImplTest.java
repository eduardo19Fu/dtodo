package xyz.pangosoft.dtodo.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Pais;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.repository.IPaisRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaisServiceImplTest {

    private final IPaisRepository repository = mock(IPaisRepository.class);
    private final PaisServiceImpl service = new PaisServiceImpl(repository);

    @Test
    void registraUnPaisNuevo() {
        Pais nuevo = Pais.builder().nombre("Guatemala").build();
        when(repository.save(any(Pais.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pais resultado = service.save(nuevo);

        assertEquals("Guatemala", resultado.getNombre());
        verify(repository).save(nuevo);
    }

    @Test
    void alActualizarConservaLaFechaDeRegistroYElUsuarioCreador() {
        Usuario creador = Usuario.builder().idUsuario(1).build();
        LocalDateTime fechaOriginal = LocalDateTime.of(2026, 1, 1, 0, 0);
        Pais existente = Pais.builder().idPais(1).nombre("Guatemala").fechaRegistro(fechaOriginal).usuario(creador).build();
        when(repository.findById(1)).thenReturn(Optional.of(existente));
        when(repository.save(any(Pais.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pais cambios = Pais.builder().idPais(1).nombre("Guatemala, C.A.").build();

        Pais resultado = service.save(cambios);

        assertEquals(fechaOriginal, resultado.getFechaRegistro());
        assertEquals(creador, resultado.getUsuario());
    }

    @Test
    void lanzaNotFoundSiElPaisNoExiste() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.findById(99));
    }
}
