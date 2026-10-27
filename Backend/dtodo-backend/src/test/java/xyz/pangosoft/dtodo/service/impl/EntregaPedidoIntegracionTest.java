package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import xyz.pangosoft.dtodo.dto.EntregaSolicitudDto;
import xyz.pangosoft.dtodo.dto.PedidoDetalleDto;
import xyz.pangosoft.dtodo.error.exceptions.ReglaNegocioException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.DetalleProforma;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.InventarioSucursal;
import xyz.pangosoft.dtodo.model.MarcaProducto;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Proforma;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.TipoProducto;
import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum;
import xyz.pangosoft.dtodo.model.enums.TipoProformaEnum;
import xyz.pangosoft.dtodo.service.IEntregaPedidoService;
import xyz.pangosoft.dtodo.service.IInventarioSucursalService;

/**
 * Las entregas contra H2 con los servicios reales: lo importante es que el inventario quede exacto después de
 * entregar, entregar por partes y anular, y que los movimientos queden con su tipo y su documento.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("context-test")
@Transactional
class EntregaPedidoIntegracionTest {

    @Autowired
    private IEntregaPedidoService entregaService;
    @Autowired
    private IInventarioSucursalService inventarioService;
    @Autowired
    private EntityManager em;

    private Sucursal central;
    private Sucursal norte;
    private Producto cuadernos;
    private Producto reglas;
    private Proforma pedido;

    @BeforeEach
    void escenario() {
        Estado activo = Estado.builder().estado("ACTIVO").build();
        em.persist(activo);
        central = Sucursal.builder().nombre("Central").direccion("Jalapa").codigoEstablecimientoSat(1).esPrincipal(true)
                .estado(activo).build();
        norte = Sucursal.builder().nombre("Norte").direccion("Monjas").codigoEstablecimientoSat(2).estado(activo).build();
        em.persist(central);
        em.persist(norte);
        Usuario irene = Usuario.builder().usuario("irene").primerNombre("Irene").apellido("Ortega").password("x").build();
        em.persist(irene);
        Cliente cliente = Cliente.builder().nombre("ANA PEREZ").nit("4514627-6").build();
        em.persist(cliente);
        MarcaProducto marca = MarcaProducto.builder().marca("MARCA").build();
        TipoProducto tipo = TipoProducto.builder().tipoProducto("TIPO").build();
        em.persist(marca);
        em.persist(tipo);
        cuadernos = producto("A-1", "CUADERNO", marca, tipo);
        reglas = producto("B-2", "REGLA", marca, tipo);
        existencias(central, cuadernos, 100);
        existencias(central, reglas, 100);
        existencias(norte, cuadernos, 3);

        pedido = new Proforma();
        pedido.setNoProforma("123P");
        pedido.setTotal(new BigDecimal("100.00"));
        pedido.setEstado(activo);
        pedido.setUsuario(irene);
        pedido.setCliente(cliente);
        pedido.setSucursal(central);
        pedido.setTipoProforma(TipoProformaEnum.MAYORISTA);
        pedido.setEstadoPedido(EstadoPedidoEnum.EN_RUTA);
        pedido.getItemsProforma().add(linea(cuadernos, 10));
        pedido.getItemsProforma().add(linea(reglas, 5));
        em.persist(pedido);
        em.flush();

        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("id_usuario", String.valueOf(irene.getIdUsuario()))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private Producto producto(String codigo, String nombre, MarcaProducto marca, TipoProducto tipo) {
        Producto producto = Producto.builder().codProducto(codigo).nombre(nombre).precioVenta(new BigDecimal("10.00"))
                .precioCompra(new BigDecimal("5.00")).marcaProducto(marca).tipoProducto(tipo).build();
        em.persist(producto);
        return producto;
    }

    private void existencias(Sucursal sucursal, Producto producto, int stock) {
        em.persist(InventarioSucursal.builder().sucursal(sucursal).producto(producto).stock(stock).build());
    }

    private DetalleProforma linea(Producto producto, int cantidad) {
        DetalleProforma linea = new DetalleProforma();
        linea.setProducto(producto);
        linea.setCantidad(cantidad);
        return linea;
    }

    private Long idLinea(Producto producto) {
        return pedido.getItemsProforma().stream().filter(l -> l.getProducto().equals(producto)).findFirst().get().getIdDetalle();
    }

    private EntregaSolicitudDto solicitud(Sucursal sucursal, int cuadernosEntregados, int reglasEntregadas, boolean aMano) {
        EntregaSolicitudDto solicitud = new EntregaSolicitudDto();
        solicitud.setIdSucursal(sucursal.getIdSucursal());
        solicitud.setFechaEntrega(LocalDate.now());
        solicitud.setDescontadoAMano(aMano);
        EntregaSolicitudDto.Linea a = new EntregaSolicitudDto.Linea();
        a.setIdDetalle(idLinea(cuadernos));
        a.setCantidad(cuadernosEntregados);
        EntregaSolicitudDto.Linea b = new EntregaSolicitudDto.Linea();
        b.setIdDetalle(idLinea(reglas));
        b.setCantidad(reglasEntregadas);
        solicitud.getLineas().add(a);
        solicitud.getLineas().add(b);
        return solicitud;
    }

    private int stock(Sucursal sucursal, Producto producto) {
        em.flush();
        em.clear();
        return inventarioService.obtenerStock(sucursal.getIdSucursal(), producto.getIdProducto());
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> movimientos() {
        em.flush();
        return em.createQuery("select m.tipoMovimiento, m.cantidad, m.tipoDocumentoOrigen, m.idDocumentoOrigen "
                + "from MovimientoProducto m order by m.idMovimiento").getResultList();
    }

    @Test
    void entregarTodoDescuentaDeLaSucursalYDejaElPedidoEntregado() {
        PedidoDetalleDto detalle = entregaService.registrar(pedido.getIdProforma(), solicitud(central, 10, 5, false));

        assertEquals(EstadoPedidoEnum.ENTREGADA, detalle.getEstadoPedido());
        assertEquals(90, stock(central, cuadernos));
        assertEquals(95, stock(central, reglas));
        assertEquals(1, detalle.getEntregas().size());
        assertEquals(2, detalle.getEntregas().get(0).getLineas().size());
        assertEquals(0, detalle.getLineas().get(0).getPendiente());
        List<Object[]> movimientos = movimientos();
        assertEquals(2, movimientos.size());
        assertEquals("ENTREGA_PROFORMA", movimientos.get(0)[0].toString());
        assertEquals("ENTREGA_PROFORMA", movimientos.get(0)[2]);
        assertEquals(detalle.getEntregas().get(0).getIdEntrega(), movimientos.get(0)[3]);
    }

    @Test
    void entregarPorPartesDescuentaCadaVezSoloLoEntregado() {
        entregaService.registrar(pedido.getIdProforma(), solicitud(central, 4, 0, false));
        assertEquals(96, stock(central, cuadernos));
        assertEquals(100, stock(central, reglas));

        PedidoDetalleDto parcial = entregaService.registrar(pedido.getIdProforma(), solicitud(central, 3, 5, false));
        assertEquals(EstadoPedidoEnum.ENTREGADA_PARCIAL, parcial.getEstadoPedido());
        assertEquals(93, stock(central, cuadernos));
        assertEquals(3, parcial.getLineas().get(0).getPendiente());

        PedidoDetalleDto completo = entregaService.registrar(pedido.getIdProforma(), solicitud(central, 3, 0, false));
        assertEquals(EstadoPedidoEnum.ENTREGADA, completo.getEstadoPedido());
        assertEquals(90, stock(central, cuadernos));
        assertEquals(95, stock(central, reglas));
        assertEquals(3, completo.getEntregas().size());
    }

    @Test
    void lasUnidadesDescontadasAManoNoSeDescuentanDeNuevo() {
        PedidoDetalleDto detalle = entregaService.registrar(pedido.getIdProforma(), solicitud(central, 10, 5, true));

        assertEquals(EstadoPedidoEnum.ENTREGADA, detalle.getEstadoPedido());
        assertEquals(100, stock(central, cuadernos));
        assertEquals(100, stock(central, reglas));
        assertTrue(movimientos().isEmpty());
        assertTrue(detalle.getEntregas().get(0).isDescontadoAMano());
    }

    @Test
    void sePuedeDescontarDeOtraSucursalQueNoSeaLaDelPedido() {
        existencias(norte, reglas, 50);

        entregaService.registrar(pedido.getIdProforma(), solicitud(norte, 0, 5, false));

        assertEquals(45, stock(norte, reglas));
        assertEquals(100, stock(central, reglas));
    }

    @Test
    void sinExistenciasEnLaSucursalElegidaNoSeRegistraNada() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> entregaService.registrar(pedido.getIdProforma(), solicitud(norte, 10, 0, false)));

        assertTrue(error.getMessage().contains("CUADERNO"));
        assertTrue(error.getMessage().contains("Disponible: 3"));
        assertEquals(3, stock(norte, cuadernos));
        assertTrue(movimientos().isEmpty());
    }

    @Test
    void anularUnaEntregaDevuelveElInventarioYElPedidoVuelveARuta() {
        PedidoDetalleDto entregado = entregaService.registrar(pedido.getIdProforma(), solicitud(central, 10, 5, false));
        Long idEntrega = entregado.getEntregas().get(0).getIdEntrega();

        PedidoDetalleDto anulado = entregaService.anular(pedido.getIdProforma(), idEntrega, "se registró de más");

        assertEquals(EstadoPedidoEnum.EN_RUTA, anulado.getEstadoPedido());
        assertEquals(100, stock(central, cuadernos));
        assertEquals(100, stock(central, reglas));
        assertTrue(anulado.getEntregas().get(0).isAnulada());
        assertEquals(10, anulado.getLineas().get(0).getPendiente());
        List<Object[]> movimientos = movimientos();
        assertEquals(4, movimientos.size());
        assertEquals("ANULACION_ENTREGA_PROFORMA", movimientos.get(2)[0].toString());
    }

    @Test
    void anularUnaEntregaSoloDevuelveLoQueEsaEntregaDescontoYSePuedeEntregarOtraVez() {
        entregaService.registrar(pedido.getIdProforma(), solicitud(central, 4, 0, false));
        PedidoDetalleDto segunda = entregaService.registrar(pedido.getIdProforma(), solicitud(central, 6, 5, false));
        Long idSegunda = segunda.getEntregas().get(1).getIdEntrega();
        assertEquals(90, stock(central, cuadernos));

        PedidoDetalleDto tras = entregaService.anular(pedido.getIdProforma(), idSegunda, "devolvieron los cuadernos");

        assertEquals(EstadoPedidoEnum.ENTREGADA_PARCIAL, tras.getEstadoPedido());
        assertEquals(96, stock(central, cuadernos));
        assertEquals(100, stock(central, reglas));
        assertEquals(6, tras.getLineas().get(0).getPendiente());

        PedidoDetalleDto otraVez = entregaService.registrar(pedido.getIdProforma(), solicitud(central, 6, 5, false));
        assertEquals(EstadoPedidoEnum.ENTREGADA, otraVez.getEstadoPedido());
        assertEquals(90, stock(central, cuadernos));
    }

    @Test
    void cerrarConLoEntregadoNoMueveInventario() {
        entregaService.registrar(pedido.getIdProforma(), solicitud(central, 4, 5, false));

        PedidoDetalleDto cerrado = entregaService.cerrarConLoEntregado(pedido.getIdProforma(), "ya no se llevará el resto");

        assertEquals(EstadoPedidoEnum.ENTREGADA, cerrado.getEstadoPedido());
        assertEquals(96, stock(central, cuadernos));
        assertEquals(2, movimientos().size());
        assertTrue(cerrado.getEtapas().get(cerrado.getEtapas().size() - 1).getMotivo().contains("ya no se llevará"));
    }
}
