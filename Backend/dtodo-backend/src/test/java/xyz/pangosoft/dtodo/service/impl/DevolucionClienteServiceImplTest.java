package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import xyz.pangosoft.dtodo.dto.CrearDevolucionClienteDto;
import xyz.pangosoft.dtodo.dto.DevolucionClienteDto;
import xyz.pangosoft.dtodo.dto.PedidoDevolubleDto;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.DetalleProforma;
import xyz.pangosoft.dtodo.model.DevolucionCliente;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.Factura;
import xyz.pangosoft.dtodo.model.MovimientoProducto;
import xyz.pangosoft.dtodo.model.NotaCredito;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.AccionAuditoriaEnum;
import xyz.pangosoft.dtodo.model.enums.EstadoNotaCreditoEnum;
import xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum;
import xyz.pangosoft.dtodo.model.enums.TipoDocumentoOrigenEnum;
import xyz.pangosoft.dtodo.model.enums.TipoMovimientoEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import xyz.pangosoft.dtodo.repository.IDevolucionClienteRepository;
import xyz.pangosoft.dtodo.repository.IFacturaRepository;
import xyz.pangosoft.dtodo.repository.INotaCreditoRepository;
import xyz.pangosoft.dtodo.repository.IProformaRepository;
import xyz.pangosoft.dtodo.repository.ISucursalRepository;
import xyz.pangosoft.dtodo.service.IAmbitoVendedorService;
import xyz.pangosoft.dtodo.service.IMovimientoProductoService;
import xyz.pangosoft.dtodo.service.IProformaAuditoriaService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

class DevolucionClienteServiceImplTest {

    private final IDevolucionClienteRepository devoluciones = mock(IDevolucionClienteRepository.class);
    private final IProformaRepository proformas = mock(IProformaRepository.class);
    private final IFacturaRepository facturas = mock(IFacturaRepository.class);
    private final INotaCreditoRepository notas = mock(INotaCreditoRepository.class);
    private final ISucursalRepository sucursales = mock(ISucursalRepository.class);
    private final IMovimientoProductoService movimientos = mock(IMovimientoProductoService.class);
    private final IAmbitoVendedorService ambito = mock(IAmbitoVendedorService.class);
    private final IUsuarioService usuarios = mock(IUsuarioService.class);
    private final IProformaAuditoriaService auditoria = mock(IProformaAuditoriaService.class);
    private final DevolucionClienteServiceImpl service = new DevolucionClienteServiceImpl(devoluciones, proformas, facturas, notas,
            sucursales, movimientos, ambito, usuarios, auditoria);

    private final Sucursal central = Sucursal.builder().idSucursal(1).nombre("Central").build();
    private final Sucursal norte = Sucursal.builder().idSucursal(2).nombre("Norte").build();
    private final Cliente cliente = Cliente.builder().idCliente(5).nombre("Libreria Uno").build();
    private Proforma pedido;
    private DetalleProforma cuadernos;
    private DetalleProforma reglas;

    @BeforeEach
    void escenario() {
        when(ambito.idUsuarioActual()).thenReturn(9);
        when(usuarios.findById(9)).thenReturn(Usuario.builder().idUsuario(9).primerNombre("Irene").apellido("Ortega").build());
        when(sucursales.findById(2)).thenReturn(Optional.of(norte));
        when(auditoria.motivoObligatorio(any(), any())).thenAnswer(inv -> {
            String motivo = inv.getArgument(0);
            if (motivo == null || motivo.trim().isEmpty()) {
                throw new ReglaNegocioException("Indique el motivo");
            }
            return motivo.trim();
        });
        when(devoluciones.save(any(DevolucionCliente.class))).thenAnswer(inv -> {
            DevolucionCliente d = inv.getArgument(0);
            d.setIdDevolucion(50L);
            return d;
        });
        pedido = new Proforma();
        pedido.setIdProforma(7L);
        pedido.setNoProforma("P-7");
        pedido.setTipoProforma(TipoProformaEnum.MAYORISTA);
        pedido.setEstadoPedido(EstadoPedidoEnum.ENTREGADA);
        pedido.setEstado(Estado.builder().estado("ACTIVO").build());
        pedido.setCliente(cliente);
        pedido.setSucursal(central);
        // 10 cuadernos por Q100 (Q10 c/u), entregados 8; 5 reglas por Q25 (Q5 c/u), entregadas 5.
        cuadernos = linea(1L, "CUADERNO", 10, "100.00", 8);
        reglas = linea(2L, "REGLA", 5, "25.00", 5);
        pedido.getItemsProforma().add(cuadernos);
        pedido.getItemsProforma().add(reglas);
        when(facturas.bloquearProforma(7L)).thenReturn(Optional.of(pedido));
    }

    private DetalleProforma linea(long id, String nombre, int cantidad, String importe, int entregada) {
        DetalleProforma l = new DetalleProforma();
        l.setIdDetalle(id);
        l.setCantidad(cantidad);
        l.setSubTotalDescuento(new BigDecimal(importe));
        l.setCantidadEntregada(entregada);
        l.setCantidadDevuelta(0);
        l.setProducto(Producto.builder().idProducto((int) id).nombre(nombre).codProducto("C" + id).build());
        return l;
    }

    private static CrearDevolucionClienteDto.Linea pide(long idDetalle, int cantidad, String destino) {
        CrearDevolucionClienteDto.Linea l = new CrearDevolucionClienteDto.Linea();
        l.setIdDetalle(idDetalle);
        l.setCantidad(cantidad);
        l.setDestino(destino);
        return l;
    }

    private CrearDevolucionClienteDto solicitud(CrearDevolucionClienteDto.Linea... lineas) {
        CrearDevolucionClienteDto dto = new CrearDevolucionClienteDto();
        dto.setIdProforma(7L);
        dto.setMotivo("  producto dañado ");
        dto.getLineas().addAll(Arrays.asList(lineas));
        return dto;
    }

    // ------------------------------------------------------------------------------------------ registrar

    @Test
    void registraLaDevolucionReingresaLoQueCorrespondeYDejaConstancia() {
        DevolucionClienteDto dto = service.registrar(solicitud(pide(1, 3, "reingreso"), pide(2, 2, "BAJA")));

        assertEquals(Long.valueOf(50), dto.getIdDevolucion());
        assertEquals(new BigDecimal("40.00"), dto.getTotal());      // 3 x 10 + 2 x 5
        assertEquals("producto dañado", dto.getMotivo());
        assertEquals("Irene Ortega", dto.getUsuario());
        assertEquals(LocalDate.now(), dto.getFecha());
        assertEquals(Integer.valueOf(1), dto.getIdSucursal());
        assertEquals(2, dto.getLineas().size());
        assertEquals(Integer.valueOf(3), cuadernos.getCantidadDevuelta());
        assertEquals(Integer.valueOf(2), reglas.getCantidadDevuelta());
        verify(proformas).save(pedido);

        ArgumentCaptor<MovimientoProducto> captor = ArgumentCaptor.forClass(MovimientoProducto.class);
        verify(movimientos).save(captor.capture());      // solo el reingreso: la baja no mueve inventario
        MovimientoProducto movimiento = captor.getValue();
        assertEquals(TipoMovimientoEnum.DEVOLUCION_CLIENTE, movimiento.getTipoMovimiento());
        assertEquals(Integer.valueOf(3), movimiento.getCantidad());
        assertEquals("DEVOLUCION_CLIENTE", movimiento.getTipoDocumentoOrigen());
        assertEquals(Long.valueOf(50), movimiento.getIdDocumentoOrigen());
        assertEquals(central, movimiento.getSucursal());
        verify(auditoria).registrar(eq(7L), eq(AccionAuditoriaEnum.DEVOLUCION_CLIENTE), eq("producto dañado"), any());
    }

    @Test
    void elAdministradorPuedeElegirOtraSucursalParaElReingreso() {
        CrearDevolucionClienteDto solicitud = solicitud(pide(1, 1, "REINGRESO"));
        solicitud.setIdSucursal(2);

        DevolucionClienteDto dto = service.registrar(solicitud);

        assertEquals(Integer.valueOf(2), dto.getIdSucursal());
        ArgumentCaptor<MovimientoProducto> captor = ArgumentCaptor.forClass(MovimientoProducto.class);
        verify(movimientos).save(captor.capture());
        assertEquals(norte, captor.getValue().getSucursal());
    }

    @Test
    void siTodoSeDaDeBajaNoHaceFaltaSucursalNiSeMueveInventario() {
        pedido.setSucursal(null);

        DevolucionClienteDto dto = service.registrar(solicitud(pide(1, 2, "BAJA")));

        assertNull(dto.getIdSucursal());
        verify(movimientos, never()).save(any());
    }

    @Test
    void conReingresoYSinSucursalEnElPedidoPideElegirla() {
        pedido.setSucursal(null);

        assertThrows(ReglaNegocioException.class, () -> service.registrar(solicitud(pide(1, 2, "REINGRESO"))));
        CrearDevolucionClienteDto inexistente = solicitud(pide(1, 2, "REINGRESO"));
        inexistente.setIdSucursal(99);
        assertThrows(ReglaNegocioException.class, () -> service.registrar(inexistente));
        verify(devoluciones, never()).save(any());
    }

    @Test
    void noSeDevuelveMasDeLoEntregadoMenosLoYaDevuelto() {
        cuadernos.setCantidadDevuelta(5);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.registrar(solicitud(pide(1, 4, "REINGRESO"))));

        assertTrue(error.getMessage().contains("solo se pueden devolver 3"), error.getMessage());
        service.registrar(solicitud(pide(1, 3, "REINGRESO")));
        assertEquals(Integer.valueOf(8), cuadernos.getCantidadDevuelta());
    }

    @Test
    void validaLasLineas() {
        assertThrows(ReglaNegocioException.class, () -> service.registrar(solicitud()));
        assertThrows(ReglaNegocioException.class, () -> service.registrar(solicitud(pide(1, 0, "BAJA"))));
        assertThrows(ReglaNegocioException.class, () -> service.registrar(solicitud(pide(1, -2, "BAJA"))));
        assertThrows(ReglaNegocioException.class, () -> service.registrar(solicitud(pide(99, 1, "BAJA"))));
        assertThrows(ReglaNegocioException.class, () -> service.registrar(solicitud(pide(1, 1, "BAJA"), pide(1, 1, "BAJA"))));
        assertThrows(ReglaNegocioException.class, () -> service.registrar(solicitud(pide(1, 1, null))));
        assertThrows(ReglaNegocioException.class, () -> service.registrar(solicitud(pide(1, 1, "TIRAR"))));
        CrearDevolucionClienteDto sinId = solicitud(new CrearDevolucionClienteDto.Linea());
        assertThrows(ReglaNegocioException.class, () -> service.registrar(sinId));
        verify(devoluciones, never()).save(any());
    }

    @Test
    void pideMotivoYNoAceptaFechasFuturas() {
        CrearDevolucionClienteDto sinMotivo = solicitud(pide(1, 1, "BAJA"));
        sinMotivo.setMotivo("  ");
        assertThrows(ReglaNegocioException.class, () -> service.registrar(sinMotivo));

        CrearDevolucionClienteDto futura = solicitud(pide(1, 1, "BAJA"));
        futura.setFecha(LocalDate.now().plusDays(1));
        assertThrows(ReglaNegocioException.class, () -> service.registrar(futura));

        CrearDevolucionClienteDto antigua = solicitud(pide(1, 1, "BAJA"));
        antigua.setFecha(LocalDate.now().minusDays(20));
        assertEquals(LocalDate.now().minusDays(20), service.registrar(antigua).getFecha());
    }

    @Test
    void soloSeDevuelveDeUnPedidoMayoristaVigenteConAlgoEntregado() {
        pedido.setEstado(Estado.builder().estado("ANULADO").build());
        assertThrows(NotFoundException.class, () -> service.registrar(solicitud(pide(1, 1, "BAJA"))));

        pedido.setEstado(Estado.builder().estado("ACTIVO").build());
        pedido.setTipoProforma(TipoProformaEnum.MINORISTA);
        assertThrows(NotFoundException.class, () -> service.registrar(solicitud(pide(1, 1, "BAJA"))));

        pedido.setTipoProforma(TipoProformaEnum.MAYORISTA);
        cuadernos.setCantidadEntregada(0);
        reglas.setCantidadEntregada(0);
        ReglaNegocioException sinEntregas = assertThrows(ReglaNegocioException.class,
                () -> service.registrar(solicitud(pide(1, 1, "BAJA"))));
        assertTrue(sinEntregas.getMessage().contains("nada entregado"));

        when(facturas.bloquearProforma(8L)).thenReturn(Optional.empty());
        CrearDevolucionClienteDto otro = solicitud(pide(1, 1, "BAJA"));
        otro.setIdProforma(8L);
        assertThrows(NotFoundException.class, () -> service.registrar(otro));
        assertThrows(ReglaNegocioException.class, () -> service.registrar(null));
    }

    // ------------------------------------------------------------------------------------------ pedido facturado

    private Factura facturaVigente() {
        Factura factura = new Factura();
        factura.setIdFactura(40L);
        factura.setNoFactura(1234L);
        factura.setCorrelativoSat("ABC123");
        factura.setSerieSat("S1");
        when(facturas.findVigenteByProformaOrigen(7L)).thenReturn(Optional.of(factura));
        return factura;
    }

    private NotaCredito nota(long id, Cliente deCliente, EstadoNotaCreditoEnum estado, TipoDocumentoOrigenEnum origen) {
        NotaCredito n = new NotaCredito();
        n.setIdNotaCredito(id);
        n.setCliente(deCliente);
        n.setEstado(estado);
        n.setTipoDocumentoOrigen(origen);
        n.setTotal(new BigDecimal("40.00"));
        when(notas.findById(id)).thenReturn(Optional.of(n));
        return n;
    }

    @Test
    void siElPedidoTieneFacturaVigenteExigeUnaNotaDeCreditoQueCorresponda() {
        facturaVigente();

        ReglaNegocioException sinNota = assertThrows(ReglaNegocioException.class, () -> service.registrar(solicitud(pide(1, 1, "BAJA"))));
        assertTrue(sinNota.getMessage().contains("nota de crédito"));

        NotaCredito deOtro = nota(1, Cliente.builder().idCliente(6).build(), EstadoNotaCreditoEnum.ENTREGA_PENDIENTE, TipoDocumentoOrigenEnum.FACTURA);
        deOtro.setCorrelativoFacturaSat("ABC123");
        deOtro.setSerieFacturaSat("S1");
        NotaCredito anulada = nota(2, cliente, EstadoNotaCreditoEnum.ANULADO, TipoDocumentoOrigenEnum.FACTURA);
        anulada.setCorrelativoFacturaSat("ABC123");
        anulada.setSerieFacturaSat("S1");
        NotaCredito deOtraFactura = nota(3, cliente, EstadoNotaCreditoEnum.ENTREGA_PENDIENTE, TipoDocumentoOrigenEnum.FACTURA);
        deOtraFactura.setCorrelativoFacturaSat("OTRA");
        deOtraFactura.setSerieFacturaSat("S1");
        for (long idNota : new long[] { 1, 2, 3, 99 }) {
            CrearDevolucionClienteDto dto = solicitud(pide(1, 1, "BAJA"));
            dto.setIdNotaCredito(idNota);
            assertThrows(ReglaNegocioException.class, () -> service.registrar(dto), "nota " + idNota);
        }
        verify(devoluciones, never()).save(any());
    }

    @Test
    void conUnaNotaDeLaFacturaOdelPedidoSeRegistraYQuedaLigada() {
        facturaVigente();
        NotaCredito deLaFactura = nota(4, cliente, EstadoNotaCreditoEnum.ENTREGADO, TipoDocumentoOrigenEnum.FACTURA);
        deLaFactura.setCorrelativoFacturaSat("ABC123");
        deLaFactura.setSerieFacturaSat("S1");
        NotaCredito deLaProforma = nota(5, cliente, EstadoNotaCreditoEnum.PAGADO, TipoDocumentoOrigenEnum.PROFORMA);
        deLaProforma.setNoProforma("P-7");

        CrearDevolucionClienteDto a = solicitud(pide(1, 1, "BAJA"));
        a.setIdNotaCredito(4L);
        assertEquals(Long.valueOf(4), service.registrar(a).getIdNotaCredito());

        CrearDevolucionClienteDto b = solicitud(pide(2, 1, "BAJA"));
        b.setIdNotaCredito(5L);
        assertEquals(Long.valueOf(5), service.registrar(b).getIdNotaCredito());
    }

    @Test
    void sinFacturaLaNotaSeIgnora() {
        CrearDevolucionClienteDto dto = solicitud(pide(1, 1, "BAJA"));
        dto.setIdNotaCredito(77L);

        assertNull(service.registrar(dto).getIdNotaCredito());
    }

    // ------------------------------------------------------------------------------------------ consultas

    @Test
    void losPedidosDevolublesTraenLoDevolvibleYLasNotasQueCorresponden() {
        Factura factura = facturaVigente();
        cuadernos.setCantidadDevuelta(2);
        pedido.getItemsProforma().add(linea(3L, "SIN ENTREGA", 4, "40.00", 0));
        when(proformas.pedidosVigentesDelCliente(5)).thenReturn(Collections.singletonList(pedido));
        NotaCredito buena = nota(4, cliente, EstadoNotaCreditoEnum.ENTREGADO, TipoDocumentoOrigenEnum.FACTURA);
        buena.setCorrelativoFacturaSat(factura.getCorrelativoSat());
        buena.setSerieFacturaSat(factura.getSerieSat());
        NotaCredito ajena = nota(6, Cliente.builder().idCliente(8).build(), EstadoNotaCreditoEnum.ENTREGADO, TipoDocumentoOrigenEnum.FACTURA);
        ajena.setCorrelativoFacturaSat(factura.getCorrelativoSat());
        when(notas.findByCliente_IdClienteAndEstadoNot(5, EstadoNotaCreditoEnum.ANULADO)).thenReturn(Arrays.asList(buena, ajena));

        List<PedidoDevolubleDto> lista = service.pedidosDevolubles(5);

        assertEquals(1, lista.size());
        PedidoDevolubleDto dto = lista.get(0);
        assertTrue(dto.isFacturado());
        assertEquals(Long.valueOf(1234), dto.getNoFactura());
        assertEquals("Central", dto.getSucursal());
        assertEquals(2, dto.getLineas().size());
        PedidoDevolubleDto.Linea primera = dto.getLineas().get(0);
        assertEquals(Integer.valueOf(8), primera.getEntregada());
        assertEquals(Integer.valueOf(2), primera.getDevuelta());
        assertEquals(Integer.valueOf(6), primera.getDevolvible());
        assertEquals(new BigDecimal("10.00"), primera.getPrecioUnitario());
        assertEquals(1, dto.getNotasCredito().size());
        assertEquals(Long.valueOf(4), dto.getNotasCredito().get(0).getIdNotaCredito());
    }

    @Test
    void unPedidoSinEntregasNoSeOfrece() {
        cuadernos.setCantidadEntregada(0);
        reglas.setCantidadEntregada(0);
        when(proformas.pedidosVigentesDelCliente(5)).thenReturn(Collections.singletonList(pedido));

        assertTrue(service.pedidosDevolubles(5).isEmpty());
        assertFalse(service.pedidosDevolubles(5).stream().anyMatch(PedidoDevolubleDto::isFacturado));
    }

    @Test
    void elValorDeLaLineaSeRedondeaADosDecimales() {
        DetalleProforma tres = linea(9L, "TERNA", 3, "10.00", 3);

        assertEquals(new BigDecimal("3.33"), DevolucionClienteServiceImpl.valorDeLinea(tres, 1));
        assertEquals(new BigDecimal("6.67"), DevolucionClienteServiceImpl.valorDeLinea(tres, 2));
        tres.setSubTotalDescuento(null);
        assertEquals(new BigDecimal("0.00"), DevolucionClienteServiceImpl.valorDeLinea(tres, 2));
    }

    @Test
    void buscarYListarRespondenNotFoundYValidanElRango() {
        when(devoluciones.findById(77L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.buscar(77L));
        assertThrows(ReglaNegocioException.class, () -> service.listar(null, LocalDate.now(), LocalDate.now().minusDays(1),
                org.springframework.data.domain.PageRequest.of(0, 10)));
    }
}
