package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import xyz.pangosoft.dtodo.dto.EntregaSolicitudDto;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.DetalleProforma;
import xyz.pangosoft.dtodo.model.Factura;
import xyz.pangosoft.dtodo.model.InventarioSucursal;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.ProformaEntrega;
import xyz.pangosoft.dtodo.model.ProformaEntregaDetalle;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum;
import xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum;
import xyz.pangosoft.dtodo.model.enums.TipoMovimientoEnum;
import xyz.pangosoft.dtodo.repository.IFacturaRepository;
import xyz.pangosoft.dtodo.repository.IProformaEntregaRepository;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.repository.ISucursalRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import xyz.pangosoft.dtodo.model.enums.CondicionPagoEnum;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;
import xyz.pangosoft.dtodo.service.IInventarioSucursalService;
import xyz.pangosoft.dtodo.service.IMovimientoProductoService;
import xyz.pangosoft.dtodo.service.IPedidoService;
import xyz.pangosoft.dtodo.service.IProformaAuditoriaService;
import xyz.pangosoft.dtodo.service.ISaldoClienteService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

class EntregaPedidoServiceImplTest {

    private final IPedidoService pedidoService = mock(IPedidoService.class);
    private final IProformaRepository proformaRepository = mock(IProformaRepository.class);
    private final IProformaEntregaRepository entregaRepository = mock(IProformaEntregaRepository.class);
    private final ISucursalRepository sucursalRepository = mock(ISucursalRepository.class);
    private final IInventarioSucursalService inventarioService = mock(IInventarioSucursalService.class);
    private final IMovimientoProductoService movimientoService = mock(IMovimientoProductoService.class);
    private final IFacturaRepository facturaRepository = mock(IFacturaRepository.class);
    private final IAmbitoVendedorService ambito = mock(IAmbitoVendedorService.class);
    private final IUsuarioService usuarioService = mock(IUsuarioService.class);
    private final IProformaAuditoriaService auditoria = mock(IProformaAuditoriaService.class);
    private final ISaldoClienteService saldoService = mock(ISaldoClienteService.class);
    private final EntregaPedidoServiceImpl service = new EntregaPedidoServiceImpl(pedidoService, proformaRepository,
            entregaRepository, sucursalRepository, inventarioService, movimientoService, facturaRepository, ambito,
            usuarioService, auditoria, saldoService);

    private final Sucursal central = Sucursal.builder().idSucursal(1).nombre("Central").build();
    private Proforma pedido;
    private DetalleProforma cuadernos;
    private DetalleProforma reglas;

    @BeforeEach
    void escenario() {
        when(ambito.idUsuarioActual()).thenReturn(9);
        when(usuarioService.findById(9)).thenReturn(Usuario.builder().idUsuario(9).primerNombre("Irene").apellido("Ortega").build());
        when(sucursalRepository.findById(1)).thenReturn(Optional.of(central));
        when(auditoria.motivoObligatorio(any(), any())).thenAnswer(inv -> {
            String motivo = inv.getArgument(0);
            if (motivo == null || motivo.trim().isEmpty()) {
                throw new ReglaNegocioException("Indique el motivo");
            }
            return motivo.trim();
        });
        when(entregaRepository.save(any(ProformaEntrega.class))).thenAnswer(inv -> {
            ProformaEntrega entrega = inv.getArgument(0);
            if (entrega.getIdEntrega() == null) {
                entrega.setIdEntrega(50L);
            }
            return entrega;
        });
        pedido = new Proforma();
        pedido.setIdProforma(7L);
        pedido.setNoProforma("123P");
        pedido.setEstadoPedido(EstadoPedidoEnum.EN_RUTA);
        pedido.setFechaEmision(new Date());
        // Por defecto a crédito: la entrega no se condiciona al cobro. Las pruebas del contado lo cambian.
        pedido.setCondicionPago(CondicionPagoEnum.CREDITO);
        when(saldoService.valorEntregado(7L)).thenReturn(BigDecimal.ZERO);
        when(saldoService.cobrado(7L)).thenReturn(BigDecimal.ZERO);
        cuadernos = linea(1L, 1, "CUADERNO", 10);
        reglas = linea(2L, 2, "REGLA", 5);
        pedido.getItemsProforma().add(cuadernos);
        pedido.getItemsProforma().add(reglas);
        when(pedidoService.obtenerPedido(7L)).thenReturn(pedido);
        hayExistencias(1, 100);
        hayExistencias(2, 100);
    }

    private DetalleProforma linea(long idDetalle, int idProducto, String nombre, int cantidad) {
        DetalleProforma linea = new DetalleProforma();
        linea.setIdDetalle(idDetalle);
        linea.setCantidad(cantidad);
        linea.setSubTotalDescuento(new BigDecimal(cantidad * 10 / (idDetalle == 1L ? 1 : 2)));
        linea.setProducto(Producto.builder().idProducto(idProducto).nombre(nombre).build());
        return linea;
    }

    private void hayExistencias(int idProducto, int stock) {
        InventarioSucursal inventario = new InventarioSucursal();
        inventario.setStock(stock);
        when(inventarioService.obtenerParaActualizar(eq(central), argThat(p -> p != null && p.getIdProducto() == idProducto)))
                .thenReturn(inventario);
    }

    private EntregaSolicitudDto solicitud(int cuadernosEntregados, int reglasEntregadas) {
        EntregaSolicitudDto solicitud = new EntregaSolicitudDto();
        solicitud.setIdSucursal(1);
        solicitud.setFechaEntrega(LocalDate.now());
        EntregaSolicitudDto.Linea a = new EntregaSolicitudDto.Linea();
        a.setIdDetalle(1L);
        a.setCantidad(cuadernosEntregados);
        EntregaSolicitudDto.Linea b = new EntregaSolicitudDto.Linea();
        b.setIdDetalle(2L);
        b.setCantidad(reglasEntregadas);
        solicitud.getLineas().add(a);
        solicitud.getLineas().add(b);
        return solicitud;
    }

    // ------------------------------------------------------------------------------------------ registrar

    @Test
    void laEntregaCompletaDescuentaLoEntregadoYDejaElPedidoEntregado() {
        EntregaSolicitudDto solicitud = solicitud(10, 5);
        solicitud.setEntregadoPor("  David Aguilar ");

        service.registrar(7L, solicitud);

        assertEquals(10, cuadernos.getCantidadEntregada());
        assertEquals(10, cuadernos.getCantidadDescontada());
        assertEquals(5, reglas.getCantidadEntregada());
        verify(movimientoService).save(argThat(m -> m.getTipoMovimiento() == TipoMovimientoEnum.ENTREGA_PROFORMA
                && m.getCantidad() == 10 && m.getProducto().getIdProducto() == 1 && m.getSucursal() == central
                && "ENTREGA_PROFORMA".equals(m.getTipoDocumentoOrigen()) && Long.valueOf(50L).equals(m.getIdDocumentoOrigen())));
        verify(movimientoService).save(argThat(m -> m.getCantidad() == 5 && m.getProducto().getIdProducto() == 2));
        verify(pedidoService).cambiarEstado(eq(pedido), eq(EstadoPedidoEnum.ENTREGADA), eq("David Aguilar"), isNull());
        verify(proformaRepository).save(pedido);
    }

    @Test
    void laEntregaParcialDejaElPedidoParcialYLoPendienteSigueEntregable() {
        service.registrar(7L, solicitud(4, 0));

        assertEquals(4, cuadernos.getCantidadEntregada());
        assertEquals(0, reglas.getCantidadEntregada());
        verify(movimientoService).save(argThat(m -> m.getCantidad() == 4));
        verify(pedidoService).cambiarEstado(eq(pedido), eq(EstadoPedidoEnum.ENTREGADA_PARCIAL), any(), any());

        pedido.setEstadoPedido(EstadoPedidoEnum.ENTREGADA_PARCIAL);
        service.registrar(7L, solicitud(6, 5));

        assertEquals(10, cuadernos.getCantidadEntregada());
        assertEquals(10, cuadernos.getCantidadDescontada());
        verify(pedidoService).cambiarEstado(eq(pedido), eq(EstadoPedidoEnum.ENTREGADA), any(), any());
    }

    @Test
    void siYaSeDescontoAManoLaEntregaNoMueveInventarioPeroLoDaPorDescontado() {
        EntregaSolicitudDto solicitud = solicitud(10, 5);
        solicitud.setDescontadoAMano(true);

        service.registrar(7L, solicitud);

        verify(movimientoService, never()).save(any());
        assertEquals(10, cuadernos.getCantidadEntregada());
        assertEquals(10, cuadernos.getCantidadDescontada());
        verify(inventarioService, never()).obtenerParaActualizar(any(), any());
    }

    @Test
    void loQueLaFacturaYaDescontoNoSeDescuentaOtraVez() {
        cuadernos.setCantidadDescontada(10);
        reglas.setCantidadDescontada(2);

        service.registrar(7L, solicitud(10, 5));

        verify(movimientoService, never()).save(argThat(m -> m.getProducto().getIdProducto() == 1));
        verify(movimientoService).save(argThat(m -> m.getProducto().getIdProducto() == 2 && m.getCantidad() == 3));
        assertEquals(5, reglas.getCantidadDescontada());
    }

    @Test
    void sinExistenciasNoSeRegistraNada() {
        hayExistencias(2, 3);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, solicitud(10, 5)));

        assertTrue(error.getMessage().contains("REGLA"));
        assertTrue(error.getMessage().contains("Disponible: 3"));
        verify(entregaRepository, never()).save(any());
        verify(movimientoService, never()).save(any());
        verify(pedidoService, never()).cambiarEstado(any(), any(), any(), any());
        assertEquals(0, cuadernos.getCantidadEntregada());
    }

    @Test
    void soloSeEntregaUnPedidoEnRutaOParcial() {
        for (EstadoPedidoEnum estado : new EstadoPedidoEnum[] { EstadoPedidoEnum.CONFIRMADA, EstadoPedidoEnum.EN_PREPARACION,
                EstadoPedidoEnum.EMPACADA, EstadoPedidoEnum.ENTREGADA }) {
            pedido.setEstadoPedido(estado);
            assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, solicitud(1, 1)), estado.name());
        }
        verify(entregaRepository, never()).save(any());
    }

    @Test
    void conElDescuentoPendienteORechazadoNoSeEntrega() {
        pedido.setEstadoAutorizacion(EstadoAutorizacionEnum.PENDIENTE);
        assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, solicitud(1, 1)));
        pedido.setEstadoAutorizacion(EstadoAutorizacionEnum.RECHAZADA);
        assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, solicitud(1, 1)));
        pedido.setEstadoAutorizacion(EstadoAutorizacionEnum.APROBADA);
        service.registrar(7L, solicitud(1, 1));
    }

    @Test
    void noSeEntregaMasDeLoPendienteNiLineasAjenasNiRepetidas() {
        ReglaNegocioException demasiado = assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, solicitud(11, 0)));
        assertTrue(demasiado.getMessage().contains("solo quedan 10"));

        cuadernos.setCantidadEntregada(8);
        assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, solicitud(3, 0)));
        cuadernos.setCantidadEntregada(0);

        EntregaSolicitudDto ajena = solicitud(1, 1);
        ajena.getLineas().get(0).setIdDetalle(999L);
        assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, ajena));

        EntregaSolicitudDto repetida = solicitud(1, 1);
        repetida.getLineas().get(1).setIdDetalle(1L);
        assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, repetida));

        assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, solicitud(0, 0)));
        assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, solicitud(-1, 1)));
        EntregaSolicitudDto vacia = solicitud(1, 1);
        vacia.getLineas().clear();
        assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, vacia));
        assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, null));
        verify(entregaRepository, never()).save(any());
    }

    @Test
    void unaLineaConCantidadCeroSeOmiteYLasDemasSeEntregan() {
        service.registrar(7L, solicitud(0, 5));

        assertEquals(0, cuadernos.getCantidadEntregada());
        assertEquals(5, reglas.getCantidadEntregada());
        verify(movimientoService).save(argThat(m -> m.getProducto().getIdProducto() == 2));
        verify(movimientoService, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void laFechaDeEntregaNoPuedeSerFuturaNiDeHaceMasDeSieteDiasNiAnteriorAlPedido() {
        EntregaSolicitudDto futura = solicitud(1, 1);
        futura.setFechaEntrega(LocalDate.now().plusDays(1));
        assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, futura));

        EntregaSolicitudDto antigua = solicitud(1, 1);
        antigua.setFechaEntrega(LocalDate.now().minusDays(8));
        assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, antigua));

        pedido.setFechaEmision(new Date());
        EntregaSolicitudDto anteriorAlPedido = solicitud(1, 1);
        anteriorAlPedido.setFechaEntrega(LocalDate.now().minusDays(2));
        assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, anteriorAlPedido));

        EntregaSolicitudDto dentroDelPlazo = solicitud(1, 1);
        dentroDelPlazo.setFechaEntrega(LocalDate.now().minusDays(2));
        pedido.setFechaEmision(java.sql.Date.valueOf(LocalDate.now().minusDays(5)));
        service.registrar(7L, dentroDelPlazo);
    }

    @Test
    void sinFechaSeTomaHoyYLaSucursalEsObligatoriaYDebeExistir() {
        EntregaSolicitudDto sinFecha = solicitud(1, 1);
        sinFecha.setFechaEntrega(null);
        service.registrar(7L, sinFecha);

        EntregaSolicitudDto sinSucursal = solicitud(1, 1);
        sinSucursal.setIdSucursal(null);
        assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, sinSucursal));

        EntregaSolicitudDto inexistente = solicitud(1, 1);
        inexistente.setIdSucursal(99);
        when(sucursalRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, inexistente));
    }

    @Test
    void losTextosMuyLargosSeRechazan() {
        EntregaSolicitudDto largo = solicitud(1, 1);
        largo.setEntregadoPor(new String(new char[151]).replace('\0', 'a'));
        assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, largo));

        EntregaSolicitudDto observaciones = solicitud(1, 1);
        observaciones.setObservaciones(new String(new char[256]).replace('\0', 'a'));
        assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, observaciones));
    }

    @Test
    void unVendedorSoloRegistraLaEntregaDeSusPedidos() {
        org.mockito.Mockito.doThrow(new NotFoundException("no es suyo")).when(ambito).exigirProformaPropia(pedido);

        assertThrows(NotFoundException.class, () -> service.registrar(7L, solicitud(1, 1)));

        verify(entregaRepository, never()).save(any());
    }

    // ------------------------------------------------------------------------------------------ anular

    private ProformaEntrega entregaRegistrada(boolean aMano, int entregadas, int cubiertas, int descontadas) {
        ProformaEntrega entrega = ProformaEntrega.builder().idEntrega(50L).idProforma(7L).sucursal(central)
                .descontadoAMano(aMano).build();
        entrega.getDetalles().add(ProformaEntregaDetalle.builder().entrega(entrega).idDetalle(1L).cantidad(entregadas)
                .cantidadCubierta(cubiertas).cantidadDescontada(descontadas).build());
        when(entregaRepository.findById(50L)).thenReturn(Optional.of(entrega));
        cuadernos.setCantidadEntregada(entregadas);
        cuadernos.setCantidadDescontada(cubiertas);
        pedido.setEstadoPedido(EstadoPedidoEnum.ENTREGADA_PARCIAL);
        return entrega;
    }

    @Test
    void anularUnaEntregaReingresaLoQueDescontoYElPedidoVuelveARuta() {
        ProformaEntrega entrega = entregaRegistrada(false, 6, 6, 6);

        service.anular(7L, 50L, "  se registró de más ");

        assertTrue(entrega.isAnulada());
        assertEquals("se registró de más", entrega.getMotivoAnulacion());
        assertEquals(0, cuadernos.getCantidadEntregada());
        assertEquals(0, cuadernos.getCantidadDescontada());
        verify(movimientoService).save(argThat(m -> m.getTipoMovimiento() == TipoMovimientoEnum.ANULACION_ENTREGA_PROFORMA
                && m.getCantidad() == 6 && m.getSucursal() == central));
        verify(pedidoService).cambiarEstado(eq(pedido), eq(EstadoPedidoEnum.EN_RUTA), isNull(), any());
    }

    @Test
    void anularUnaEntregaDescontadaAManoNoReingresaNada() {
        entregaRegistrada(true, 6, 6, 0);

        service.anular(7L, 50L, "error de captura");

        verify(movimientoService, never()).save(any());
        assertEquals(0, cuadernos.getCantidadDescontada());
    }

    @Test
    void siQuedanOtrasEntregasElPedidoQuedaParcial() {
        entregaRegistrada(false, 4, 4, 4);
        cuadernos.setCantidadEntregada(10);
        cuadernos.setCantidadDescontada(10);

        service.anular(7L, 50L, "se devolvió una parte");

        assertEquals(6, cuadernos.getCantidadEntregada());
        verify(pedidoService).cambiarEstado(eq(pedido), eq(EstadoPedidoEnum.ENTREGADA_PARCIAL), isNull(), any());
    }

    @Test
    void noSeAnulaUnaEntregaSinMotivoYaAnuladaAjenaNiConFacturaVigente() {
        ProformaEntrega entrega = entregaRegistrada(false, 6, 6, 6);
        assertThrows(ReglaNegocioException.class, () -> service.anular(7L, 50L, " "));

        when(facturaRepository.findVigenteByProformaOrigen(7L)).thenReturn(Optional.of(new Factura()));
        ReglaNegocioException conFactura = assertThrows(ReglaNegocioException.class, () -> service.anular(7L, 50L, "motivo"));
        assertTrue(conFactura.getMessage().contains("Anule primero la factura"));
        when(facturaRepository.findVigenteByProformaOrigen(7L)).thenReturn(Optional.empty());

        entrega.setIdProforma(8L);
        assertThrows(NotFoundException.class, () -> service.anular(7L, 50L, "motivo"));
        entrega.setIdProforma(7L);
        assertThrows(NotFoundException.class, () -> service.anular(7L, 404L, "motivo"));

        entrega.setAnulada(true);
        assertThrows(ReglaNegocioException.class, () -> service.anular(7L, 50L, "motivo"));
        verify(movimientoService, never()).save(any());
        assertNull(entrega.getMotivoAnulacion());
    }

    // ------------------------------------------------------------------------------------------ cerrar

    @Test
    void cerrarConLoEntregadoSoloAplicaAUnPedidoParcialYPideMotivo() {
        pedido.setEstadoPedido(EstadoPedidoEnum.ENTREGADA_PARCIAL);
        assertThrows(ReglaNegocioException.class, () -> service.cerrarConLoEntregado(7L, ""));

        service.cerrarConLoEntregado(7L, "el cliente ya no quiere el resto");

        verify(pedidoService).cambiarEstado(eq(pedido), eq(EstadoPedidoEnum.ENTREGADA), isNull(), argThat(m -> m.contains("ya no quiere")));

        pedido.setEstadoPedido(EstadoPedidoEnum.EN_RUTA);
        assertThrows(ReglaNegocioException.class, () -> service.cerrarConLoEntregado(7L, "motivo"));
        pedido.setEstadoPedido(EstadoPedidoEnum.ENTREGADA);
        assertThrows(ReglaNegocioException.class, () -> service.cerrarConLoEntregado(7L, "motivo"));
    }

    // ------------------------------------------------------------------------------------------ repartidor

    @Test
    void elRepartidorEntregaDeLaSucursalDelPedidoSinElegirNiMarcarLaCasilla() {
        when(ambito.esRepartidor()).thenReturn(true);
        pedido.setSucursal(central);
        EntregaSolicitudDto solicitud = solicitud(10, 5);
        solicitud.setIdSucursal(99);
        solicitud.setDescontadoAMano(true);
        solicitud.setEntregadoPor("otro nombre");

        service.registrar(7L, solicitud);

        verify(movimientoService).save(argThat(m -> m.getCantidad() == 10 && m.getSucursal() == central));
        verify(pedidoService).cambiarEstado(eq(pedido), eq(EstadoPedidoEnum.ENTREGADA), isNull(), isNull());
        verify(sucursalRepository, never()).findById(99);
        assertEquals(10, cuadernos.getCantidadDescontada());
    }

    @Test
    void elRepartidorNoEntregaUnPedidoSinSucursal() {
        when(ambito.esRepartidor()).thenReturn(true);
        pedido.setSucursal(null);

        assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, solicitud(1, 1)));

        verify(entregaRepository, never()).save(any());
    }

    @Test
    void elRepartidorSoloEntregaLosPedidosQueSeLeAsignaron() {
        when(ambito.esRepartidor()).thenReturn(true);
        pedido.setSucursal(central);
        org.mockito.Mockito.doThrow(new NotFoundException("no es suyo")).when(ambito).exigirPedidoAsignado(pedido);

        assertThrows(NotFoundException.class, () -> service.registrar(7L, solicitud(1, 1)));

        verify(entregaRepository, never()).save(any());
    }

    // ------------------------------------------------------------------------------------------ cobro del contado

    private void comoContado(String cobrado) {
        pedido.setCondicionPago(CondicionPagoEnum.CONTADO);
        when(saldoService.cobrado(7L)).thenReturn(new BigDecimal(cobrado));
    }

    private static void autenticar(String rol) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("u", "x",
                java.util.Collections.singletonList(new SimpleGrantedAuthority(rol))));
    }

    @org.junit.jupiter.api.AfterEach
    void limpiarSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unPedidoDeContadoSinCobrarNoSeEntrega() {
        comoContado("0");

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, solicitud(3, 2)));

        assertTrue(error.getMessage().contains("falta cobrar Q40.00"), error.getMessage());
        verify(entregaRepository, never()).save(any());
    }

    @Test
    void unPedidoDeContadoCobradoSeEntregaYUnCobroParcialSoloCubreSuParte() {
        comoContado("40.00");
        service.registrar(7L, solicitud(3, 2));
        verify(entregaRepository).save(any());

        comoContado("39.99");
        assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, solicitud(3, 2)));
    }

    @Test
    void loYaEntregadoCuentaContraLoCobrado() {
        comoContado("50.00");
        when(saldoService.valorEntregado(7L)).thenReturn(new BigDecimal("30.00"));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, solicitud(3, 2)));

        assertTrue(error.getMessage().contains("falta cobrar Q20.00"), error.getMessage());
    }

    @Test
    void soloElAdministradorEntregaSinCobroYQuedaAnotado() {
        comoContado("0");
        EntregaSolicitudDto sinPago = solicitud(3, 2);
        sinPago.setEntregarSinPago(true);

        autenticar("ROLE_BODEGA");
        assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, sinPago));

        autenticar("ROLE_ADMIN");
        service.registrar(7L, sinPago);

        verify(entregaRepository).save(any());
        verify(pedidoService).cambiarEstado(any(), any(), any(), org.mockito.ArgumentMatchers.contains("Entregado sin cobrar Q40.00"));
    }

    @Test
    void elRepartidorNuncaEntregaSinCobro() {
        comoContado("0");
        when(ambito.esRepartidor()).thenReturn(true);
        pedido.setSucursal(central);
        EntregaSolicitudDto sinPago = solicitud(3, 2);
        sinPago.setEntregarSinPago(true);
        autenticar("ROLE_ADMIN");

        assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, sinPago));
    }

    @Test
    void unPedidoACreditoNoSeCondicionaAlCobro() {
        service.registrar(7L, solicitud(3, 2));

        verify(saldoService, never()).cobrado(7L);
    }

    @Test
    void conElCreditoSinAutorizarNoSeEntrega() {
        for (xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum estado : new xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum[] {
                xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum.PENDIENTE, xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum.RECHAZADA }) {
            pedido.setCreditoAutorizacion(estado);

            ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> service.registrar(7L, solicitud(1, 0)));

            assertTrue(error.getMessage().contains("crédito"), error.getMessage());
        }
        verify(entregaRepository, never()).save(any());
    }
}
