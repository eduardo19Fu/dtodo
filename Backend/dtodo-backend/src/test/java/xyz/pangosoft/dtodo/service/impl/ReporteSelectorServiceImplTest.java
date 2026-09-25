package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.dto.ReporteSelectorDto;
import xyz.pangosoft.dtodo.repository.IClienteRepository;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.repository.IProveedorRepository;
import xyz.pangosoft.dtodo.repository.IProductoRepository;
import xyz.pangosoft.dtodo.repository.ISucursalRepository;
import xyz.pangosoft.dtodo.repository.ITipoProductoRepository;
import xyz.pangosoft.dtodo.repository.IUsuarioRepository;

class ReporteSelectorServiceImplTest {

    private ISucursalRepository sucursalRepository;
    private IUsuarioRepository usuarioRepository;
    private IProformaRepository proformaRepository;
    private ITipoProductoRepository tipoProductoRepository;
    private IClienteRepository clienteRepository;
    private IProveedorRepository proveedorRepository;
    private IProductoRepository productoRepository;
    private ReporteSelectorServiceImpl service;

    @BeforeEach
    void setUp() {
        sucursalRepository = mock(ISucursalRepository.class);
        usuarioRepository = mock(IUsuarioRepository.class);
        proformaRepository = mock(IProformaRepository.class);
        tipoProductoRepository = mock(ITipoProductoRepository.class);
        clienteRepository = mock(IClienteRepository.class);
        proveedorRepository = mock(IProveedorRepository.class);
        productoRepository = mock(IProductoRepository.class);
        service = new ReporteSelectorServiceImpl(
                sucursalRepository,
                usuarioRepository,
                proformaRepository,
                tipoProductoRepository,
                clienteRepository,
                proveedorRepository,
                productoRepository);
    }

    @Test
    void usaConsultasDtoParaTodosLosSelectores() {
        List<ReporteSelectorDto> opciones = List.of(new ReporteSelectorDto(1, "Opción", "Detalle"));
        when(sucursalRepository.findOpcionesReporte()).thenReturn(opciones);
        when(usuarioRepository.findOpcionesCajeroReporte(2)).thenReturn(opciones);
        when(proformaRepository.findOpcionesUsuarioReporte(2)).thenReturn(opciones);
        when(tipoProductoRepository.findOpcionesReporte()).thenReturn(opciones);
        when(clienteRepository.findOpcionesReporte()).thenReturn(opciones);
        when(proveedorRepository.findOpcionesReporte()).thenReturn(opciones);
        when(productoRepository.findOpcionesReporte(2)).thenReturn(opciones);

        assertSame(opciones, service.listarSucursales());
        assertSame(opciones, service.listarCajeros(2));
        assertSame(opciones, service.listarUsuariosProformas(2));
        assertSame(opciones, service.listarCategorias());
        assertSame(opciones, service.listarClientes());
        assertSame(opciones, service.listarProveedores());
        assertSame(opciones, service.listarProductos(2));

        verify(sucursalRepository).findOpcionesReporte();
        verify(usuarioRepository).findOpcionesCajeroReporte(2);
        verify(proformaRepository).findOpcionesUsuarioReporte(2);
        verify(tipoProductoRepository).findOpcionesReporte();
        verify(clienteRepository).findOpcionesReporte();
        verify(proveedorRepository).findOpcionesReporte();
        verify(productoRepository).findOpcionesReporte(2);
    }
}
