package xyz.pangosoft.dtodo.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.Proveedor;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.repository.IProveedorRepository;
import xyz.pangosoft.dtodo.service.IEstadoService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProveedorServiceImplTest {

    private final IProveedorRepository repository = mock(IProveedorRepository.class);
    private final IEstadoService estadoService = mock(IEstadoService.class);
    private final ProveedorServiceImpl service = new ProveedorServiceImpl(repository, estadoService);

    @Test
    void registraUnProveedorNuevoConEstadoActivo() {
        Estado activo = Estado.builder().idEstado(1).estado("ACTIVO").build();
        Proveedor nuevo = Proveedor.builder().nombre("Distribuidora XYZ").build();
        when(estadoService.findByEstado("ACTIVO")).thenReturn(activo);
        when(repository.save(any(Proveedor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Proveedor resultado = service.save(nuevo);

        assertEquals(activo, resultado.getEstado());
        verify(repository).save(nuevo);
    }

    @Test
    void alActualizarConservaLaFechaDeRegistroYElUsuarioCreador() {
        Usuario creador = Usuario.builder().idUsuario(1).build();
        LocalDateTime fechaOriginal = LocalDateTime.of(2026, 1, 1, 0, 0);
        Estado activo = Estado.builder().idEstado(1).estado("ACTIVO").build();
        Proveedor existente = Proveedor.builder()
                .idProveedor(1)
                .nombre("Distribuidora XYZ")
                .fechaRegistro(fechaOriginal)
                .usuario(creador)
                .estado(activo)
                .build();
        when(repository.findById(1)).thenReturn(Optional.of(existente));
        when(repository.save(any(Proveedor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Proveedor cambios = Proveedor.builder().idProveedor(1).nombre("Distribuidora XYZ Renombrada").build();

        Proveedor resultado = service.save(cambios);

        assertEquals(fechaOriginal, resultado.getFechaRegistro());
        assertEquals(creador, resultado.getUsuario());
        assertEquals(activo, resultado.getEstado());
        verify(estadoService, never()).findByEstado(any());
    }

    @Test
    void lanzaNotFoundSiElProveedorNoExiste() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.findById(99));
    }
}
