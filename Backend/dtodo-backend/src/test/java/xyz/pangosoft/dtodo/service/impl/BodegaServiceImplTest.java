package xyz.pangosoft.dtodo.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Bodega;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.repository.IBodegaRepository;
import xyz.pangosoft.dtodo.service.IEstadoService;
import xyz.pangosoft.dtodo.service.ISucursalService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BodegaServiceImplTest {

    private final IBodegaRepository bodegaRepo = mock(IBodegaRepository.class);
    private final IEstadoService estadoService = mock(IEstadoService.class);
    private final ISucursalService sucursalService = mock(ISucursalService.class);
    private final IUsuarioService usuarioService = mock(IUsuarioService.class);

    private final BodegaServiceImpl service =
            new BodegaServiceImpl(bodegaRepo, estadoService, sucursalService, usuarioService);

    private final Estado activo = Estado.builder().idEstado(1).estado("ACTIVO").build();
    private final Estado inactivo = Estado.builder().idEstado(2).estado("INACTIVO").build();

    private Bodega bodegaNueva(String nombre) {
        return Bodega.builder().nombre(nombre).ubicacion("Zona 1").build();
    }

    @Test
    void registraUnaBodegaNuevaActivaConElUsuarioQueLaCreoYSinAceptarElEstadoDelCliente() {
        Usuario creador = Usuario.builder().idUsuario(7).usuario("bodeguero").build();
        when(bodegaRepo.existsByNombreIgnoreCase("Principal")).thenReturn(false);
        when(estadoService.findByEstado("ACTIVO")).thenReturn(activo);
        when(usuarioService.findById(7)).thenReturn(creador);
        when(bodegaRepo.save(any(Bodega.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Bodega solicitud = bodegaNueva("  Principal  ");
        solicitud.setEstado(inactivo);
        Bodega resultado = service.save(solicitud, 7);

        assertEquals("Principal", resultado.getNombre());
        assertSame(activo, resultado.getEstado());
        assertSame(creador, resultado.getUsuario());
    }

    @Test
    void asignaLaSucursalIndicadaAlRegistrarLaBodega() {
        Sucursal sucursal = Sucursal.builder().idSucursal(3).nombre("Norte").build();
        when(sucursalService.findById(3)).thenReturn(sucursal);
        when(estadoService.findByEstado("ACTIVO")).thenReturn(activo);
        when(bodegaRepo.save(any(Bodega.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Bodega solicitud = bodegaNueva("Norte");
        solicitud.setSucursal(Sucursal.builder().idSucursal(3).build());

        assertSame(sucursal, service.save(solicitud, 7).getSucursal());
    }

    @Test
    void rechazaUnNombreVacioOSoloEspacios() {
        assertThrows(BadRequestException.class, () -> service.save(bodegaNueva("   "), 7));

        verify(bodegaRepo, never()).save(any(Bodega.class));
    }

    @Test
    void rechazaUnNombreDuplicadoSinImportarMayusculas() {
        when(bodegaRepo.existsByNombreIgnoreCase("PRINCIPAL")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.save(bodegaNueva("PRINCIPAL"), 7));

        verify(bodegaRepo, never()).save(any(Bodega.class));
    }

    @Test
    void alActualizarConservaCreadorYFechaDeRegistroAunqueElClienteLosCambie() {
        Usuario creadorOriginal = Usuario.builder().idUsuario(1).usuario("admin").build();
        LocalDateTime fechaOriginal = LocalDateTime.of(2026, 9, 1, 8, 0);
        Bodega existente = Bodega.builder().idBodega(5).nombre("Vieja").ubicacion("Zona 1")
                .estado(activo).usuario(creadorOriginal).fechaRegistro(fechaOriginal).build();
        when(bodegaRepo.findById(5)).thenReturn(Optional.of(existente));
        when(bodegaRepo.existsByNombreIgnoreCaseAndIdBodegaNot("Nueva", 5)).thenReturn(false);
        when(bodegaRepo.save(any(Bodega.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Bodega solicitud = Bodega.builder().idBodega(5).nombre("Nueva").ubicacion("Zona 9")
                .descripcion("Descripción").encargado("Ana").telefono("5555")
                .usuario(Usuario.builder().idUsuario(99).build())
                .fechaRegistro(LocalDateTime.of(2000, 1, 1, 0, 0)).build();

        Bodega resultado = service.save(solicitud, 99);

        assertEquals("Nueva", resultado.getNombre());
        assertEquals("Zona 9", resultado.getUbicacion());
        assertEquals("Ana", resultado.getEncargado());
        assertSame(creadorOriginal, resultado.getUsuario());
        assertEquals(fechaOriginal, resultado.getFechaRegistro());
        assertSame(activo, resultado.getEstado());
    }

    @Test
    void alActualizarPermiteDesactivarLaBodegaPeroNoUsarOtrosEstados() {
        Bodega existente = Bodega.builder().idBodega(5).nombre("Vieja").ubicacion("Z").estado(activo).build();
        when(bodegaRepo.findById(5)).thenReturn(Optional.of(existente));
        when(estadoService.findById(2)).thenReturn(inactivo);
        when(estadoService.findById(3)).thenReturn(Estado.builder().idEstado(3).estado("PAGADO").build());
        when(bodegaRepo.save(any(Bodega.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Bodega desactivar = Bodega.builder().idBodega(5).nombre("Vieja").ubicacion("Z")
                .estado(Estado.builder().idEstado(2).build()).build();
        assertSame(inactivo, service.save(desactivar, 1).getEstado());

        Bodega invalido = Bodega.builder().idBodega(5).nombre("Vieja").ubicacion("Z")
                .estado(Estado.builder().idEstado(3).build()).build();
        assertThrows(BadRequestException.class, () -> service.save(invalido, 1));
    }

    @Test
    void alActualizarRechazaElNombreDeOtraBodegaPeroNoElPropio() {
        Bodega existente = Bodega.builder().idBodega(5).nombre("Vieja").ubicacion("Z").estado(activo).build();
        when(bodegaRepo.findById(5)).thenReturn(Optional.of(existente));
        when(bodegaRepo.existsByNombreIgnoreCaseAndIdBodegaNot("Ocupado", 5)).thenReturn(true);

        Bodega solicitud = Bodega.builder().idBodega(5).nombre("Ocupado").ubicacion("Z").build();

        assertThrows(BadRequestException.class, () -> service.save(solicitud, 1));
    }

    @Test
    void actualizarUnaBodegaInexistenteLanzaNotFound() {
        when(bodegaRepo.findById(404)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.save(Bodega.builder().idBodega(404).nombre("X").ubicacion("Z").build(), 1));
    }

    @Test
    void permiteQuitarLaSucursalDeUnaBodega() {
        Bodega existente = Bodega.builder().idBodega(5).nombre("Vieja").ubicacion("Z").estado(activo)
                .sucursal(Sucursal.builder().idSucursal(3).build()).build();
        when(bodegaRepo.findById(5)).thenReturn(Optional.of(existente));
        when(bodegaRepo.save(any(Bodega.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Bodega resultado = service.save(Bodega.builder().idBodega(5).nombre("Vieja").ubicacion("Z").build(), 1);

        assertNull(resultado.getSucursal());
    }

    @Test
    void findActivaByIdRechazaLasBodegasInactivas() {
        Bodega inactiva = Bodega.builder().idBodega(5).nombre("Cerrada").estado(inactivo).build();
        when(bodegaRepo.findById(5)).thenReturn(Optional.of(inactiva));

        assertThrows(BadRequestException.class, () -> service.findActivaById(5));
    }

    @Test
    void findActivaByIdDevuelveLaBodegaActiva() {
        Bodega activa = Bodega.builder().idBodega(5).nombre("Abierta").estado(activo).build();
        when(bodegaRepo.findById(5)).thenReturn(Optional.of(activa));

        assertSame(activa, service.findActivaById(5));
    }

    @Test
    void findAllConSoloActivasOmiteLasInactivas() {
        Bodega a = Bodega.builder().idBodega(1).nombre("A").estado(activo).build();
        Bodega b = Bodega.builder().idBodega(2).nombre("B").estado(inactivo).build();
        when(bodegaRepo.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenAnswer(invocation -> new java.util.ArrayList<>(java.util.List.of(a, b)));

        assertEquals(java.util.List.of(a), service.findAll(true));
        assertEquals(java.util.List.of(a, b), service.findAll(false));
    }

    @Test
    void guardaElNombreSinEspaciosSobrantesAlActualizar() {
        Bodega existente = Bodega.builder().idBodega(5).nombre("Vieja").ubicacion("Z").estado(activo).build();
        when(bodegaRepo.findById(5)).thenReturn(Optional.of(existente));
        when(bodegaRepo.save(any(Bodega.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Bodega> guardada = ArgumentCaptor.forClass(Bodega.class);

        service.save(Bodega.builder().idBodega(5).nombre("  Renombrada ").ubicacion("Z").build(), 1);

        verify(bodegaRepo).save(guardada.capture());
        assertEquals("Renombrada", guardada.getValue().getNombre());
    }
}
