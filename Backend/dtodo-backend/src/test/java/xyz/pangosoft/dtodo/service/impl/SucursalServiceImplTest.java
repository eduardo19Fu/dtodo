package xyz.pangosoft.dtodo.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.repository.ISucursalRepository;
import xyz.pangosoft.dtodo.service.IEstadoService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SucursalServiceImplTest {

    private final ISucursalRepository repository = mock(ISucursalRepository.class);
    private final IEstadoService estadoService = mock(IEstadoService.class);
    private final SucursalServiceImpl service = new SucursalServiceImpl(repository, estadoService);

    @Test
    void registraUnaSucursalNuevaConEstadoActivo() {
        Estado activo = Estado.builder().idEstado(1).estado("ACTIVO").build();
        Sucursal nueva = Sucursal.builder().nombre("Sucursal Norte").direccion("Km 5").build();
        when(estadoService.findByEstado("ACTIVO")).thenReturn(activo);
        when(repository.save(any(Sucursal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Sucursal resultado = service.save(nueva);

        assertEquals(activo, resultado.getEstado());
        verify(repository).save(nueva);
    }

    @Test
    void alActualizarConservaLaFechaDeRegistroElUsuarioCreadorYElFlagPrincipal() {
        Usuario creador = Usuario.builder().idUsuario(1).build();
        LocalDateTime fechaOriginal = LocalDateTime.of(2026, 1, 1, 0, 0);
        Estado activo = Estado.builder().idEstado(1).estado("ACTIVO").build();
        Sucursal existente = Sucursal.builder()
                .idSucursal(1)
                .nombre("Sucursal Central")
                .fechaRegistro(fechaOriginal)
                .usuario(creador)
                .esPrincipal(true)
                .estado(activo)
                .build();
        when(repository.findById(1)).thenReturn(Optional.of(existente));
        when(repository.save(any(Sucursal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Sucursal cambios = Sucursal.builder()
                .idSucursal(1)
                .nombre("Sucursal Central Renombrada")
                .direccion("Nueva dirección")
                .esPrincipal(false)
                .build();

        Sucursal resultado = service.save(cambios);

        assertEquals(fechaOriginal, resultado.getFechaRegistro());
        assertEquals(creador, resultado.getUsuario());
        assertEquals(true, resultado.isEsPrincipal());
        assertEquals("Sucursal Central Renombrada", resultado.getNombre());
        assertEquals(activo, resultado.getEstado());
        verify(estadoService, never()).findByEstado(any());
    }

    @Test
    void alActualizarRespetaElEstadoEnviadoSiVieneEspecificado() {
        Estado activo = Estado.builder().idEstado(1).estado("ACTIVO").build();
        Estado inactivo = Estado.builder().idEstado(2).estado("INACTIVO").build();
        Sucursal existente = Sucursal.builder()
                .idSucursal(1)
                .nombre("Sucursal Central")
                .fechaRegistro(LocalDateTime.now())
                .estado(activo)
                .build();
        when(repository.findById(1)).thenReturn(Optional.of(existente));
        when(repository.save(any(Sucursal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Sucursal cambios = Sucursal.builder()
                .idSucursal(1)
                .nombre("Sucursal Central")
                .direccion("Direccion")
                .estado(inactivo)
                .build();

        Sucursal resultado = service.save(cambios);

        assertEquals(inactivo, resultado.getEstado());
    }

    @Test
    void lanzaNotFoundSiLaSucursalNoExiste() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.findById(99));
    }

    @Test
    void lanzaNotFoundSiNoHaySucursalPrincipal() {
        when(repository.findByEsPrincipalTrue()).thenReturn(null);

        assertThrows(NotFoundException.class, service::findPrincipal);
    }
}
