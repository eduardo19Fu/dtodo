package xyz.pangosoft.dtodo.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import xyz.pangosoft.dtodo.dto.CompraDto;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.model.Compra;
import xyz.pangosoft.dtodo.model.CompraDetalle;
import xyz.pangosoft.dtodo.model.MovimientoProducto;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Proveedor;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.EstadoCompraEnum;
import xyz.pangosoft.dtodo.model.enums.TipoMovimientoEnum;
import xyz.pangosoft.dtodo.repository.ICompraRepository;
import xyz.pangosoft.dtodo.service.IMovimientoProductoService;
import xyz.pangosoft.dtodo.service.IProductoService;
import xyz.pangosoft.dtodo.service.IProveedorService;
import xyz.pangosoft.dtodo.service.ISucursalService;
import xyz.pangosoft.dtodo.service.IUsuarioService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompraServiceImplTest {

    private final ICompraRepository compraRepository = mock(ICompraRepository.class);
    private final IProveedorService proveedorService = mock(IProveedorService.class);
    private final IUsuarioService usuarioService = mock(IUsuarioService.class);
    private final ISucursalService sucursalService = mock(ISucursalService.class);
    private final IProductoService productoService = mock(IProductoService.class);
    private final IMovimientoProductoService movimientoProductoService = mock(IMovimientoProductoService.class);

    private final CompraServiceImpl service = new CompraServiceImpl(
            compraRepository, proveedorService, usuarioService, sucursalService, productoService, movimientoProductoService);

    private Usuario usuario() {
        return Usuario.builder().idUsuario(1).usuario("admin").build();
    }

    private Sucursal sucursal() {
        return Sucursal.builder().idSucursal(1).nombre("Central").build();
    }

    private Proveedor proveedor() {
        return Proveedor.builder().idProveedor(1).nombre("Distribuidora XYZ").build();
    }

    @Test
    void registraUnaCompraConProductoExistenteYGeneraElMovimientoDeCompra() {
        Usuario usuario = usuario();
        Sucursal sucursal = sucursal();
        Proveedor proveedor = proveedor();
        Producto producto = Producto.builder().idProducto(10).nombre("Producto A").build();

        when(usuarioService.findById(1)).thenReturn(usuario);
        when(sucursalService.findById(1)).thenReturn(sucursal);
        when(proveedorService.findById(1)).thenReturn(proveedor);
        when(productoService.findById(10)).thenReturn(producto);
        when(compraRepository.save(any(Compra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompraDetalle detalle = CompraDetalle.builder()
                .producto(Producto.builder().idProducto(10).build())
                .cantidad(5)
                .precioUnitario(new BigDecimal("10.00"))
                .build();

        Compra compra = new Compra();
        compra.setUsuario(Usuario.builder().idUsuario(1).build());
        compra.setSucursal(Sucursal.builder().idSucursal(1).build());
        compra.setProveedor(Proveedor.builder().idProveedor(1).build());
        compra.setCostoEnvio(new BigDecimal("5.00"));
        compra.setItems(List.of(detalle));

        Compra resultado = service.crear(compra);

        assertEquals(EstadoCompraEnum.ACTIVA, resultado.getEstado());
        assertEquals(new BigDecimal("50.00"), detalle.getSubTotal());
        assertEquals(new BigDecimal("55.00"), resultado.getTotal());

        verify(movimientoProductoService).save(argThatMovimiento(TipoMovimientoEnum.COMPRA, producto, sucursal, 5));
        verify(productoService, never()).save(any(Producto.class));
    }

    @Test
    void siElProductoDelDetalleNoTieneIdSeCreaComoProductoNuevoAntesDeGuardarLaCompra() {
        when(usuarioService.findById(1)).thenReturn(usuario());
        when(sucursalService.findById(1)).thenReturn(sucursal());
        when(proveedorService.findById(1)).thenReturn(proveedor());

        Producto productoNuevo = Producto.builder().nombre("Producto Nuevo").build();
        Producto productoGuardado = Producto.builder().idProducto(99).nombre("Producto Nuevo").build();
        when(productoService.save(productoNuevo)).thenReturn(productoGuardado);
        when(compraRepository.save(any(Compra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompraDetalle detalle = CompraDetalle.builder()
                .producto(productoNuevo)
                .cantidad(3)
                .precioUnitario(new BigDecimal("20.00"))
                .build();

        Compra compra = new Compra();
        compra.setUsuario(Usuario.builder().idUsuario(1).build());
        compra.setSucursal(Sucursal.builder().idSucursal(1).build());
        compra.setProveedor(Proveedor.builder().idProveedor(1).build());
        compra.setItems(List.of(detalle));

        Compra resultado = service.crear(compra);

        assertEquals(productoGuardado, detalle.getProducto());
        assertEquals(new BigDecimal("60.00"), resultado.getTotal());
        verify(productoService).save(productoNuevo);
    }

    @Test
    void lanzaBadRequestSiLaCompraNoTieneDetalle() {
        Compra compra = new Compra();
        compra.setUsuario(Usuario.builder().idUsuario(1).build());
        compra.setSucursal(Sucursal.builder().idSucursal(1).build());
        compra.setProveedor(Proveedor.builder().idProveedor(1).build());

        assertThrows(BadRequestException.class, () -> service.crear(compra));
    }

    @Test
    void lanzaBadRequestSiFaltaLaSucursal() {
        CompraDetalle detalle = CompraDetalle.builder()
                .producto(Producto.builder().idProducto(10).build())
                .cantidad(1)
                .precioUnitario(BigDecimal.TEN)
                .build();

        Compra compra = new Compra();
        compra.setUsuario(Usuario.builder().idUsuario(1).build());
        compra.setProveedor(Proveedor.builder().idProveedor(1).build());
        compra.setItems(List.of(detalle));

        assertThrows(BadRequestException.class, () -> service.crear(compra));
    }

    @Test
    void anularUnaCompraActivaCambiaSuEstadoYRevierteElStockConEliminarCompra() {
        Producto producto = Producto.builder().idProducto(10).build();
        Sucursal sucursal = sucursal();
        CompraDetalle detalle = CompraDetalle.builder().producto(producto).cantidad(4).build();

        Compra compraExistente = new Compra();
        compraExistente.setIdCompra(1L);
        compraExistente.setEstado(EstadoCompraEnum.ACTIVA);
        compraExistente.setSucursal(sucursal);
        compraExistente.setItems(List.of(detalle));

        Usuario usuarioAnula = Usuario.builder().idUsuario(2).build();

        when(compraRepository.findById(1L)).thenReturn(Optional.of(compraExistente));
        when(usuarioService.findById(2)).thenReturn(usuarioAnula);
        when(compraRepository.save(any(Compra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Compra resultado = service.anular(1L, 2);

        assertEquals(EstadoCompraEnum.ANULADA, resultado.getEstado());
        verify(movimientoProductoService).save(argThatMovimiento(TipoMovimientoEnum.ELIMINAR_COMPRA, producto, sucursal, 4));
    }

    @Test
    void noPermiteAnularUnaCompraYaAnulada() {
        Compra compraAnulada = new Compra();
        compraAnulada.setIdCompra(1L);
        compraAnulada.setEstado(EstadoCompraEnum.ANULADA);
        when(compraRepository.findById(1L)).thenReturn(Optional.of(compraAnulada));

        assertThrows(BadRequestException.class, () -> service.anular(1L, 2));
        verify(movimientoProductoService, never()).save(any());
    }

    @Test
    void lanzaNotFoundSiLaCompraNoExiste() {
        when(compraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.findById(99L));
    }

    @Test
    void delegaElListadoAlRepositorio() {
        Page<CompraDto> pagina = new PageImpl<>(List.of());
        when(compraRepository.findListado("", null, PageRequest.of(0, 5))).thenReturn(pagina);

        assertEquals(pagina, service.findListado(null, null, PageRequest.of(0, 5)));
    }

    private static MovimientoProducto argThatMovimiento(
            TipoMovimientoEnum tipo, Producto producto, Sucursal sucursal, int cantidad) {
        return org.mockito.ArgumentMatchers.argThat(movimiento ->
                movimiento != null
                        && tipo.equals(movimiento.getTipoMovimiento())
                        && producto.equals(movimiento.getProducto())
                        && sucursal.equals(movimiento.getSucursal())
                        && cantidad == movimiento.getCantidad());
    }
}
