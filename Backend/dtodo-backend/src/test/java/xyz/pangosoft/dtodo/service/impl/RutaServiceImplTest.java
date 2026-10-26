package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.dto.RutaDto;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.Role;
import xyz.pangosoft.dtodo.model.Ruta;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.TipoClienteEnum;
import xyz.pangosoft.dtodo.repository.IClienteRepository;
import xyz.pangosoft.dtodo.repository.IRutaRepository;
import xyz.pangosoft.dtodo.service.IEstadoService;
import xyz.pangosoft.dtodo.service.ISucursalService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

class RutaServiceImplTest {

    private final IRutaRepository rutaRepository = mock(IRutaRepository.class);
    private final IClienteRepository clienteRepository = mock(IClienteRepository.class);
    private final ISucursalService sucursalService = mock(ISucursalService.class);
    private final IUsuarioService usuarioService = mock(IUsuarioService.class);
    private final IEstadoService estadoService = mock(IEstadoService.class);
    private RutaServiceImpl service;

    private final Sucursal central = Sucursal.builder().idSucursal(1).nombre("Central").build();
    private final Estado activo = Estado.builder().idEstado(1).estado("ACTIVO").build();
    private final Estado inactivo = Estado.builder().idEstado(2).estado("INACTIVO").build();

    @BeforeEach
    void preparar() {
        service = new RutaServiceImpl(rutaRepository, clienteRepository, sucursalService, usuarioService, estadoService);
        when(sucursalService.findById(1)).thenReturn(central);
        when(estadoService.findByEstado("ACTIVO")).thenReturn(activo);
        when(estadoService.findById(1)).thenReturn(activo);
        when(estadoService.findById(2)).thenReturn(inactivo);
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(inv -> {
            Ruta ruta = inv.getArgument(0);
            if (ruta.getIdRuta() == null) {
                ruta.setIdRuta(10);
            }
            return ruta;
        });
        when(rutaRepository.findDtoById(anyInt())).thenAnswer(inv ->
                Optional.of(new RutaDto(inv.getArgument(0), "RUTA", null, null, null, null, 1, "Central", 1, "ACTIVO", 0L)));
    }

    private static Ruta solicitud(String nombre) {
        Ruta ruta = new Ruta();
        ruta.setNombre(nombre);
        ruta.setSucursal(Sucursal.builder().idSucursal(1).build());
        return ruta;
    }

    private static Usuario usuarioConRoles(Integer id, boolean habilitado, String... roles) {
        Usuario usuario = Usuario.builder().idUsuario(id).usuario("u" + id).enabled(habilitado).build();
        Role[] asignados = Arrays.stream(roles).map(r -> Role.builder().role(r).build()).toArray(Role[]::new);
        usuario.setRoles(Arrays.asList(asignados));
        return usuario;
    }

    @Test
    void creaUnaRutaActivaSinVendedor() {
        Ruta ruta = solicitud("  RUTA 3  ");

        RutaDto resultado = service.save(ruta);

        assertEquals(10, resultado.getIdRuta());
        assertEquals("RUTA 3", ruta.getNombre());
        assertSame(activo, ruta.getEstado());
        assertSame(central, ruta.getSucursal());
        assertNull(ruta.getVendedor());
    }

    @Test
    void rechazaUnNombreVacio() {
        assertThrows(ReglaNegocioException.class, () -> service.save(solicitud("   ")));

        verify(rutaRepository, never()).save(any());
    }

    @Test
    void exigeLaSucursal() {
        Ruta ruta = new Ruta();
        ruta.setNombre("RUTA 1");

        assertThrows(ReglaNegocioException.class, () -> service.save(ruta));
    }

    @Test
    void rechazaUnNombreRepetidoEnLaMismaSucursal() {
        when(rutaRepository.existsByNombreIgnoreCaseAndSucursal_IdSucursal("RUTA 1", 1)).thenReturn(true);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> service.save(solicitud("RUTA 1")));

        assertTrue(error.getMessage().contains("RUTA 1"));
        verify(rutaRepository, never()).save(any());
    }

    @Test
    void asignaUnVendedorConElRolYHabilitado() {
        Usuario vendedor = usuarioConRoles(5, true, "ROLE_VENDEDOR");
        when(usuarioService.findById(5)).thenReturn(vendedor);
        Ruta ruta = solicitud("RUTA 1");
        ruta.setVendedor(Usuario.builder().idUsuario(5).build());

        service.save(ruta);

        assertSame(vendedor, ruta.getVendedor());
    }

    @Test
    void rechazaUnVendedorSinElRolDeVendedor() {
        when(usuarioService.findById(6)).thenReturn(usuarioConRoles(6, true, "ROLE_COBRADOR"));
        Ruta ruta = solicitud("RUTA 1");
        ruta.setVendedor(Usuario.builder().idUsuario(6).build());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> service.save(ruta));

        assertTrue(error.getMessage().contains("rol de vendedor"));
    }

    @Test
    void rechazaUnVendedorDeshabilitado() {
        when(usuarioService.findById(7)).thenReturn(usuarioConRoles(7, false, "ROLE_VENDEDOR"));
        Ruta ruta = solicitud("RUTA 1");
        ruta.setVendedor(Usuario.builder().idUsuario(7).build());

        assertThrows(ReglaNegocioException.class, () -> service.save(ruta));
    }

    @Test
    void alActualizarCopiaSoloLosCamposEditablesYConservaLaFechaDeRegistro() {
        Ruta existente = new Ruta();
        existente.setIdRuta(3);
        existente.setNombre("RUTA 3");
        existente.setSucursal(central);
        existente.setEstado(activo);
        existente.setFechaRegistro(java.time.LocalDateTime.of(2026, 10, 1, 8, 0));
        when(rutaRepository.findById(3)).thenReturn(Optional.of(existente));

        Ruta cambios = solicitud("RUTA 3 - San Pedro");
        cambios.setIdRuta(3);
        cambios.setZona("San Pedro Pinula");
        cambios.setDiasVisita("Lunes cada 15 días");
        cambios.setFechaRegistro(java.time.LocalDateTime.of(1999, 1, 1, 0, 0));

        service.save(cambios);

        assertEquals("RUTA 3 - San Pedro", existente.getNombre());
        assertEquals("San Pedro Pinula", existente.getZona());
        assertEquals("Lunes cada 15 días", existente.getDiasVisita());
        assertEquals(java.time.LocalDateTime.of(2026, 10, 1, 8, 0), existente.getFechaRegistro());
        assertSame(activo, existente.getEstado());
    }

    @Test
    void permiteInactivarUnaRutaPeroNoUsarOtroEstado() {
        Ruta existente = new Ruta();
        existente.setIdRuta(3);
        existente.setNombre("RUTA 3");
        existente.setSucursal(central);
        existente.setEstado(activo);
        when(rutaRepository.findById(3)).thenReturn(Optional.of(existente));

        Ruta inactivar = solicitud("RUTA 3");
        inactivar.setIdRuta(3);
        inactivar.setEstado(Estado.builder().idEstado(2).build());
        service.save(inactivar);
        assertSame(inactivo, existente.getEstado());

        when(estadoService.findById(9)).thenReturn(Estado.builder().idEstado(9).estado("ANULADO").build());
        Ruta invalida = solicitud("RUTA 3");
        invalida.setIdRuta(3);
        invalida.setEstado(Estado.builder().idEstado(9).build());
        assertThrows(ReglaNegocioException.class, () -> service.save(invalida));
    }

    @Test
    void rechazaActualizarUnaRutaQueNoExiste() {
        when(rutaRepository.findById(99)).thenReturn(Optional.empty());
        Ruta ruta = solicitud("RUTA 9");
        ruta.setIdRuta(99);

        assertThrows(NotFoundException.class, () -> service.save(ruta));
    }

    @Test
    void rechazaRenombrarUnaRutaConElNombreDeOtra() {
        Ruta existente = new Ruta();
        existente.setIdRuta(3);
        existente.setSucursal(central);
        when(rutaRepository.findById(3)).thenReturn(Optional.of(existente));
        when(rutaRepository.existsByNombreIgnoreCaseAndSucursal_IdSucursalAndIdRutaNot("RUTA 1", 1, 3)).thenReturn(true);
        Ruta cambios = solicitud("RUTA 1");
        cambios.setIdRuta(3);

        assertThrows(ReglaNegocioException.class, () -> service.save(cambios));

        verify(rutaRepository, never()).save(any());
    }

    @Test
    void laCarteraDevuelveLosClientesDeLaRutaEnFormaDeDto() {
        Ruta ruta = new Ruta();
        ruta.setIdRuta(1);
        ruta.setNombre("RUTA 1");
        when(rutaRepository.findById(1)).thenReturn(Optional.of(ruta));
        Cliente cliente = Cliente.builder().idCliente(4).nombre("Libreria Uno").tipoCliente(TipoClienteEnum.MAYORISTA)
                .ruta(ruta).build();
        when(clienteRepository.findByRuta_IdRutaOrderByNombre(1)).thenReturn(Collections.singletonList(cliente));

        assertEquals(1, service.findClientes(1).size());
        assertEquals("RUTA 1", service.findClientes(1).get(0).getRuta());
        assertEquals(TipoClienteEnum.MAYORISTA, service.findClientes(1).get(0).getTipoCliente());
    }

    @Test
    void elFiltroNuloSeConvierteEnVacio() {
        when(rutaRepository.findListado(anyString(), any())).thenReturn(org.springframework.data.domain.Page.empty());

        service.findListado(null, org.springframework.data.domain.PageRequest.of(0, 5));

        verify(rutaRepository).findListado(org.mockito.ArgumentMatchers.eq(""), any());
    }
}
