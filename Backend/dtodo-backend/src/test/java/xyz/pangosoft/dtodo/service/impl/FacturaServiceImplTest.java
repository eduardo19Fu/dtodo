package xyz.pangosoft.dtodo.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import xyz.pangosoft.dtodo.dto.FacturaDto;
import xyz.pangosoft.dtodo.fel.IFelService;
import xyz.pangosoft.dtodo.fel.dto.RespuestaCertificacion;
import xyz.pangosoft.dtodo.fel.dto.RespuestaFirma;
import xyz.pangosoft.dtodo.fel.model.DatosEmisor;
import xyz.pangosoft.dtodo.model.Certificador;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.Correlativo;
import xyz.pangosoft.dtodo.model.DetalleFactura;
import xyz.pangosoft.dtodo.model.Emisor;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.Factura;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.repository.IFacturaRepository;
import xyz.pangosoft.dtodo.repository.ITipoFacturaRepository;
import xyz.pangosoft.dtodo.service.ICertificadorService;
import xyz.pangosoft.dtodo.service.ICorrelativoService;
import xyz.pangosoft.dtodo.service.IEmisorService;
import xyz.pangosoft.dtodo.service.IEstadoService;
import xyz.pangosoft.dtodo.service.IMovimientoProductoService;
import xyz.pangosoft.dtodo.service.ITipoFacturaService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FacturaServiceImplTest {

    @Test
    void propagaElIdUsuarioAlListadoPaginadoParaUnUsuarioNoAdmin() {
        IFacturaRepository repository = mock(IFacturaRepository.class);
        FacturaServiceImpl service = new FacturaServiceImpl(
                repository, mock(ITipoFacturaRepository.class), mock(IEmisorService.class),
                mock(IEstadoService.class), mock(ITipoFacturaService.class), mock(ICorrelativoService.class),
                mock(ICertificadorService.class), mock(IMovimientoProductoService.class), mock(IUsuarioService.class),
                mock(IFelService.class), mock(DataSource.class));
        PageRequest pageable = PageRequest.of(0, 5);
        when(repository.findAllListadoDto(any(), any(), eq(7), eq(pageable))).thenReturn(Page.empty());

        service.findAllListadoDto("2026-08-01", "2026-08-05", 7, pageable);

        verify(repository).findAllListadoDto(any(), any(), eq(7), eq(pageable));
    }

    @Test
    void ordenaLasUltimasFacturasAntesDePaginar() {
        IFacturaRepository repository = mock(IFacturaRepository.class);
        FacturaServiceImpl service = new FacturaServiceImpl(
                repository,
                mock(ITipoFacturaRepository.class),
                mock(IEmisorService.class),
                mock(IEstadoService.class),
                mock(ITipoFacturaService.class),
                mock(ICorrelativoService.class),
                mock(ICertificadorService.class),
                mock(IMovimientoProductoService.class),
                mock(IUsuarioService.class),
                mock(IFelService.class),
                mock(DataSource.class)
        );
        FacturaDto segunda = FacturaDto.builder().noFactura(200L).build();
        FacturaDto primera = FacturaDto.builder().noFactura(100L).build();
        when(repository.findUltimasListadoDto(any(), any()))
                .thenReturn(new ArrayList<>(Arrays.asList(segunda, primera)));

        Page<FacturaDto> resultado = service.findUltimasListadoDto("", null,
                PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "noFactura")));

        assertEquals(100L, resultado.getContent().get(0).getNoFactura());
        assertEquals(200L, resultado.getContent().get(1).getNoFactura());
    }

    @Test
    void totalVentasCuentaSoloLasDelUsuarioIndicado() {
        IFacturaRepository repository = mock(IFacturaRepository.class);
        FacturaServiceImpl service = new FacturaServiceImpl(
                repository, mock(ITipoFacturaRepository.class), mock(IEmisorService.class),
                mock(IEstadoService.class), mock(ITipoFacturaService.class), mock(ICorrelativoService.class),
                mock(ICertificadorService.class), mock(IMovimientoProductoService.class), mock(IUsuarioService.class),
                mock(IFelService.class), mock(DataSource.class));
        when(repository.getCantidadVentasPorUsuario(20)).thenReturn(3L);

        Integer total = service.totalVentas(20);

        assertEquals(3, total);
    }

    @Test
    void totalVentasSinUsuarioDevuelveElConteoGlobal() {
        IFacturaRepository repository = mock(IFacturaRepository.class);
        FacturaServiceImpl service = new FacturaServiceImpl(
                repository, mock(ITipoFacturaRepository.class), mock(IEmisorService.class),
                mock(IEstadoService.class), mock(ITipoFacturaService.class), mock(ICorrelativoService.class),
                mock(ICertificadorService.class), mock(IMovimientoProductoService.class), mock(IUsuarioService.class),
                mock(IFelService.class), mock(DataSource.class));
        when(repository.getCantidadVentas()).thenReturn(150);

        Integer total = service.totalVentas(null);

        assertEquals(150, total);
    }

    @Test
    void usaElCodigoDeEstablecimientoDeLaSucursalEnVezDeUnValorFijo() throws Exception {
        FacturaServiceImpl service = new FacturaServiceImpl(
                mock(IFacturaRepository.class), mock(ITipoFacturaRepository.class), mock(IEmisorService.class),
                mock(IEstadoService.class), mock(ITipoFacturaService.class), mock(ICorrelativoService.class),
                mock(ICertificadorService.class), mock(IMovimientoProductoService.class), mock(IUsuarioService.class),
                mock(IFelService.class), mock(DataSource.class));

        Emisor emisor = Emisor.builder().nit("12345678").nombreEmisor("Comercial De Todo").nombreComercial("De Todo").build();
        Sucursal sucursal = Sucursal.builder().idSucursal(2).nombre("Sucursal Norte").codigoEstablecimientoSat(3).build();

        Method metodo = FacturaServiceImpl.class.getDeclaredMethod("configurarDatosEmisor", Emisor.class, Sucursal.class);
        metodo.setAccessible(true);
        DatosEmisor datosEmisor = (DatosEmisor) metodo.invoke(service, emisor, sucursal);

        assertEquals(3, datosEmisor.getCodigoEstablecimiento());
    }

    @Test
    void usaCodigoDeEstablecimientoPorDefectoCuandoNoHaySucursal() throws Exception {
        FacturaServiceImpl service = new FacturaServiceImpl(
                mock(IFacturaRepository.class), mock(ITipoFacturaRepository.class), mock(IEmisorService.class),
                mock(IEstadoService.class), mock(ITipoFacturaService.class), mock(ICorrelativoService.class),
                mock(ICertificadorService.class), mock(IMovimientoProductoService.class), mock(IUsuarioService.class),
                mock(IFelService.class), mock(DataSource.class));

        Emisor emisor = Emisor.builder().nit("12345678").nombreEmisor("Comercial De Todo").nombreComercial("De Todo").build();

        Method metodo = FacturaServiceImpl.class.getDeclaredMethod("configurarDatosEmisor", Emisor.class, Sucursal.class);
        metodo.setAccessible(true);
        DatosEmisor datosEmisor = (DatosEmisor) metodo.invoke(service, emisor, null);

        assertEquals(1, datosEmisor.getCodigoEstablecimiento());
    }

    // ---------- Venta FEL: la BD se escribe antes de certificar ----------

    private final IFacturaRepository repoVenta = mock(IFacturaRepository.class);
    private final ICorrelativoService correlativoVenta = mock(ICorrelativoService.class);
    private final IMovimientoProductoService movimientosVenta = mock(IMovimientoProductoService.class);
    private final IFelService felVenta = mock(IFelService.class);

    private FacturaServiceImpl servicioDeVenta() {
        IEstadoService estadoService = mock(IEstadoService.class);
        IEmisorService emisorService = mock(IEmisorService.class);
        ICertificadorService certificadorService = mock(ICertificadorService.class);
        IUsuarioService usuarioService = mock(IUsuarioService.class);

        when(estadoService.findByEstado(any())).thenAnswer(inv -> Estado.builder().estado(inv.getArgument(0)).build());
        when(emisorService.getEmisor(1)).thenReturn(Emisor.builder().nit("12345678").nombreEmisor("Comercial De Todo").build());
        when(certificadorService.getCertificador(1)).thenReturn(new Certificador());
        when(usuarioService.findById(7)).thenReturn(cajero());
        when(correlativoVenta.findByUsuario(7)).thenReturn(Correlativo.builder().correlativoActual(100L).correlativoFinal(500L).build());
        when(repoVenta.saveAndFlush(any())).thenAnswer(inv -> {
            Factura factura = inv.getArgument(0);
            factura.setIdFactura(10L);
            return factura;
        });
        when(repoVenta.findLineasGuardadas(10L))
                .thenReturn(Collections.singletonList(new Object[] {1, 2, BigDecimal.ZERO}));

        RespuestaFirma firma = new RespuestaFirma();
        firma.setResultado(true);
        firma.setArchivo("xml-firmado");
        when(felVenta.firmarDocumento(any(), any())).thenReturn(firma);

        return new FacturaServiceImpl(
                repoVenta, mock(ITipoFacturaRepository.class), emisorService,
                estadoService, mock(ITipoFacturaService.class), correlativoVenta,
                certificadorService, movimientosVenta, usuarioService,
                felVenta, mock(DataSource.class));
    }

    private static Usuario cajero() {
        return Usuario.builder().idUsuario(7).usuario("caja1").primerNombre("Ana").apellido("Lopez")
                .sucursal(Sucursal.builder().idSucursal(1).codigoEstablecimientoSat(1).build()).build();
    }

    private static Factura venta() {
        Factura factura = new Factura();
        factura.setNoFactura(100L);
        factura.setSerie("A");
        factura.setUsuario(cajero());
        factura.setCliente(Cliente.builder().nit("C/F").nombre("Consumidor Final").direccion("Ciudad").build());
        Producto producto = Producto.builder().idProducto(1).nombre("Agua").precioVenta(new BigDecimal("10.00")).build();
        factura.getItemsFactura().add(DetalleFactura.builder().producto(producto).cantidad(2).descuento(BigDecimal.ZERO).build());
        return factura;
    }

    private static RespuestaCertificacion certificacion(boolean exitosa) {
        RespuestaCertificacion respuesta = new RespuestaCertificacion();
        respuesta.setResultado(exitosa);
        respuesta.setCantidad_errores(0);
        respuesta.setUuid("UUID-123");
        respuesta.setSerie("SERIE-SAT");
        respuesta.setNumero("987");
        respuesta.setFecha("2026-09-28T10:00:00-06:00");
        return respuesta;
    }

    @Test
    void siLaBaseDeDatosRechazaLaFacturaNuncaSeEnviaAlCertificador() {
        FacturaServiceImpl service = servicioDeVenta();
        doThrow(new InvalidDataAccessResourceUsageException("Unknown column 'id_proforma_origen'"))
                .when(repoVenta).saveAndFlush(any());

        assertThrows(RuntimeException.class, () -> service.facturaFel(venta()));

        verify(felVenta, never()).certificar(any(), any(), any(), any());
    }

    @Test
    void registraVentaCorrelativoYExistenciasAntesDeCertificar() {
        FacturaServiceImpl service = servicioDeVenta();
        when(felVenta.certificar(any(), any(), any(), eq("CERTIFICACION"))).thenReturn(certificacion(true));

        Factura resultado = service.facturaFel(venta());

        InOrder orden = inOrder(repoVenta, correlativoVenta, movimientosVenta, felVenta);
        orden.verify(repoVenta).saveAndFlush(any());
        orden.verify(correlativoVenta).update(any());
        orden.verify(movimientosVenta).save(any());
        orden.verify(repoVenta).flush();
        orden.verify(felVenta).certificar(any(), any(), any(), eq("CERTIFICACION"));
        orden.verify(repoVenta).saveAndFlush(any());
        assertEquals("UUID-123", resultado.getCertificacionSat());
        assertEquals("987", resultado.getCorrelativoSat());
        verify(movimientosVenta).save(argThat(movimiento ->
                "FACTURA".equals(movimiento.getTipoDocumentoOrigen())
                        && Long.valueOf(10L).equals(movimiento.getIdDocumentoOrigen())));
    }

    @Test
    void siLaCertificacionFallaLanzaExcepcionParaRevertirLaVenta() {
        FacturaServiceImpl service = servicioDeVenta();
        when(felVenta.certificar(any(), any(), any(), eq("CERTIFICACION"))).thenReturn(certificacion(false));

        assertThrows(RuntimeException.class, () -> service.facturaFel(venta()));

        verify(repoVenta, times(1)).saveAndFlush(any());
    }

    @Test
    void noCertificaSiLosProductosGuardadosNoCoincidenConLosRecibidos() {
        FacturaServiceImpl service = servicioDeVenta();
        when(repoVenta.findLineasGuardadas(10L))
                .thenReturn(Collections.singletonList(new Object[] {99, 2, BigDecimal.ZERO}));

        assertThrows(RuntimeException.class, () -> service.facturaFel(venta()));

        verify(felVenta, never()).firmarDocumento(any(), any());
        verify(felVenta, never()).certificar(any(), any(), any(), any());
    }

    @Test
    void anulaEnSatElDteDeUnaVentaQueNoQuedoRegistrada() {
        FacturaServiceImpl service = servicioDeVenta();
        when(felVenta.certificar(any(), any(), any(), eq("ANULACION"))).thenReturn(certificacion(true));
        Factura factura = venta();
        factura.setCertificacionSat("UUID-123");
        factura.setFechaCertificacionSat("2026-09-28T10:00:00-06:00");

        service.anularDteHuerfano(factura, Emisor.builder().nit("12345678").build(), new Certificador());

        verify(felVenta).certificar(any(), eq("xml-firmado"), eq("ANULACION_100"), eq("ANULACION"));
    }
}
