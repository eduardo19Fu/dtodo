package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.Ruta;
import xyz.pangosoft.dtodo.model.enums.CategoriaClienteEnum;
import xyz.pangosoft.dtodo.model.enums.TipoClienteEnum;
import xyz.pangosoft.dtodo.repository.IClienteRepository;
import xyz.pangosoft.dtodo.repository.IRutaRepository;

/** Reglas del cliente mayorista al guardar: ruta obligatoria, código único y campos editables. */
class ClienteMayoristaServiceTest {

    private final IClienteRepository clienteRepository = mock(IClienteRepository.class);
    private final IRutaRepository rutaRepository = mock(IRutaRepository.class);
    private ClienteServiceImpl service;

    private final Ruta rutaActiva = ruta(1, "RUTA 1", "ACTIVO");
    private final Ruta rutaInactiva = ruta(2, "RUTA 2", "INACTIVO");

    private static Ruta ruta(Integer id, String nombre, String estado) {
        Ruta ruta = new Ruta();
        ruta.setIdRuta(id);
        ruta.setNombre(nombre);
        ruta.setEstado(Estado.builder().estado(estado).build());
        return ruta;
    }

    @BeforeEach
    void preparar() {
        service = new ClienteServiceImpl(clienteRepository, rutaRepository);
        when(rutaRepository.findById(1)).thenReturn(Optional.of(rutaActiva));
        when(rutaRepository.findById(2)).thenReturn(Optional.of(rutaInactiva));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Cliente mayorista(Integer idRuta) {
        Cliente cliente = Cliente.builder().nombre("Libreria Uno").direccion("Monjas").tipoCliente(TipoClienteEnum.MAYORISTA).build();
        if (idRuta != null) {
            Ruta referencia = new Ruta();
            referencia.setIdRuta(idRuta);
            cliente.setRuta(referencia);
        }
        return cliente;
    }

    @Test
    void unClienteNuevoSinTipoEsMinorista() {
        Cliente cliente = new Cliente();
        cliente.setTipoCliente(null);
        cliente.setNombre("Cliente de tienda");

        Cliente guardado = service.save(cliente);

        assertEquals(TipoClienteEnum.MINORISTA, guardado.getTipoCliente());
        assertTrue(guardado.getFechaRegistro() != null);
    }

    @Test
    void unMayoristaDebeTenerRuta() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> service.save(mayorista(null)));

        assertTrue(error.getMessage().contains("ruta"));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void unMayoristaNoPuedeIrEnUnaRutaInexistenteNiInactiva() {
        when(rutaRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ReglaNegocioException.class, () -> service.save(mayorista(99)));
        ReglaNegocioException inactiva = assertThrows(ReglaNegocioException.class, () -> service.save(mayorista(2)));
        assertTrue(inactiva.getMessage().contains("inactiva"));
    }

    @Test
    void guardaUnMayoristaConSuRutaYCategoria() {
        Cliente cliente = mayorista(1);
        cliente.setCategoriaDescuento(CategoriaClienteEnum.VOLUMEN);
        cliente.setCodigoCliente("  55551234  ");

        Cliente guardado = service.save(cliente);

        assertSame(rutaActiva, guardado.getRuta());
        assertEquals(CategoriaClienteEnum.VOLUMEN, guardado.getCategoriaDescuento());
        assertEquals("55551234", guardado.getCodigoCliente());
    }

    @Test
    void unMinoristaPierdeLaRutaYLaCategoria() {
        Cliente cliente = mayorista(1);
        cliente.setTipoCliente(TipoClienteEnum.MINORISTA);
        cliente.setCategoriaDescuento(CategoriaClienteEnum.A);

        Cliente guardado = service.save(cliente);

        assertNull(guardado.getRuta());
        assertNull(guardado.getCategoriaDescuento());
    }

    @Test
    void rechazaUnCodigoQueYaPerteneceAOtroCliente() {
        Cliente otro = Cliente.builder().idCliente(8).nombre("Tienda Dos").codigoCliente("55551234").build();
        when(clienteRepository.findByCodigoCliente("55551234")).thenReturn(Optional.of(otro));
        Cliente cliente = mayorista(1);
        cliente.setCodigoCliente("55551234");

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> service.save(cliente));

        assertTrue(error.getMessage().contains("Tienda Dos"));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void elCodigoVacioSeGuardaComoNulo() {
        Cliente cliente = mayorista(1);
        cliente.setCodigoCliente("   ");

        assertNull(service.save(cliente).getCodigoCliente());
        verify(clienteRepository, never()).findByCodigoCliente(any());
    }

    @Test
    void rechazaCreditoNegativo() {
        Cliente cliente = mayorista(1);
        cliente.setLimiteCredito(new BigDecimal("-1"));
        assertThrows(ReglaNegocioException.class, () -> service.save(cliente));

        Cliente dias = mayorista(1);
        dias.setDiasCredito(-5);
        assertThrows(ReglaNegocioException.class, () -> service.save(dias));
    }

    private Cliente existente() {
        Cliente existente = Cliente.builder().idCliente(4).nombre("Libreria Uno").nit("4514627-6")
                .fechaRegistro(LocalDateTime.of(2026, 1, 1, 9, 0)).tipoCliente(TipoClienteEnum.MAYORISTA)
                .ruta(rutaInactiva).build();
        when(clienteRepository.findById(4)).thenReturn(Optional.of(existente));
        return existente;
    }

    @Test
    void alActualizarConservaLaFechaDeRegistroYCopiaLosCamposEditables() {
        Cliente existente = existente();
        Cliente cambios = mayorista(2);
        cambios.setIdCliente(4);
        cambios.setNombre("Libreria Uno S.A.");
        cambios.setFechaRegistro(LocalDateTime.of(1999, 1, 1, 0, 0));
        cambios.setContacto("Ana Perez");
        cambios.setLimiteCredito(new BigDecimal("3000"));
        cambios.setDiasCredito(30);

        Cliente guardado = service.save(cambios);

        assertSame(existente, guardado);
        assertEquals("Libreria Uno S.A.", guardado.getNombre());
        assertEquals("Ana Perez", guardado.getContacto());
        assertEquals(new BigDecimal("3000"), guardado.getLimiteCredito());
        assertEquals(LocalDateTime.of(2026, 1, 1, 9, 0), guardado.getFechaRegistro());
    }

    @Test
    void unClienteQueYaEstabaEnUnaRutaInactivaPuedeSeguirEditandose() {
        existente();
        Cliente cambios = mayorista(2);
        cambios.setIdCliente(4);
        cambios.setNombre("Libreria Uno");

        assertSame(rutaInactiva, service.save(cambios).getRuta());
    }

    @Test
    void noPermiteMoverAUnClienteAUnaRutaInactivaDistintaDeLaSuya() {
        Cliente existente = existente();
        existente.setRuta(rutaActiva);
        Cliente cambios = mayorista(2);
        cambios.setIdCliente(4);

        assertThrows(ReglaNegocioException.class, () -> service.save(cambios));
    }

    @Test
    void elMismoClienteConservaSuPropioCodigo() {
        Cliente existente = existente();
        existente.setCodigoCliente("55551234");
        when(clienteRepository.findByCodigoCliente("55551234")).thenReturn(Optional.of(existente));
        Cliente cambios = mayorista(2);
        cambios.setIdCliente(4);
        cambios.setCodigoCliente("55551234");

        assertEquals("55551234", service.save(cambios).getCodigoCliente());
    }

    @Test
    void actualizarUnClienteInexistenteEsNotFound() {
        when(clienteRepository.findById(77)).thenReturn(Optional.empty());
        Cliente cambios = mayorista(1);
        cambios.setIdCliente(77);

        assertThrows(NotFoundException.class, () -> service.save(cambios));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void buscaPorCodigoYAvisaSiNoExiste() {
        Cliente cliente = Cliente.builder().idCliente(4).codigoCliente("55551234").build();
        when(clienteRepository.findByCodigoCliente("55551234")).thenReturn(Optional.of(cliente));

        assertSame(cliente, service.findByCodigo(" 55551234 "));
        assertThrows(NotFoundException.class, () -> service.findByCodigo("0000"));
    }
}
