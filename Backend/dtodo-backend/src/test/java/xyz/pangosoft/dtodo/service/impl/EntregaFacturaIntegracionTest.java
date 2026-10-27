package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

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
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.model.Cliente;
import xyz.pangosoft.dtodo.model.DetalleFactura;
import xyz.pangosoft.dtodo.model.DetalleProforma;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.Factura;
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
import xyz.pangosoft.dtodo.service.IInventarioFacturaPedidoService;
import xyz.pangosoft.dtodo.service.IInventarioSucursalService;

/**
 * La entrega y la factura de un mismo pedido, contra H2 con los servicios reales, en todas las secuencias: el
 * inventario debe descontarse una sola vez y quedar exacto aunque se anule la factura o la entrega.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("context-test")
@Transactional
class EntregaFacturaIntegracionTest {

    @Autowired
    private IEntregaPedidoService entregaService;
    @Autowired
    private IInventarioFacturaPedidoService inventarioFactura;
    @Autowired
    private IInventarioSucursalService inventarioService;
    @Autowired
    private EntityManager em;

    private Estado activo;
    private Sucursal central;
    private Usuario irene;
    private Cliente cliente;
    private Producto cuadernos;
    private Producto lapices;
    private Proforma pedido;

    @BeforeEach
    void escenario() {
        activo = Estado.builder().estado("ACTIVO").build();
        em.persist(activo);
        central = Sucursal.builder().nombre("Central").direccion("Jalapa").codigoEstablecimientoSat(1).esPrincipal(true)
                .estado(activo).build();
        em.persist(central);
        irene = Usuario.builder().usuario("irene").primerNombre("Irene").apellido("Ortega").password("x").build();
        em.persist(irene);
        cliente = Cliente.builder().nombre("ANA PEREZ").nit("4514627-6").build();
        em.persist(cliente);
        MarcaProducto marca = MarcaProducto.builder().marca("MARCA").build();
        TipoProducto tipo = TipoProducto.builder().tipoProducto("TIPO").build();
        em.persist(marca);
        em.persist(tipo);
        cuadernos = producto("A-1", "CUADERNO", marca, tipo);
        lapices = producto("B-2", "LAPIZ", marca, tipo);
        em.persist(InventarioSucursal.builder().sucursal(central).producto(cuadernos).stock(100).build());
        em.persist(InventarioSucursal.builder().sucursal(central).producto(lapices).stock(100).build());

        pedido = new Proforma();
        pedido.setNoProforma("123P");
        pedido.setTotal(new BigDecimal("100.00"));
        pedido.setEstado(activo);
        pedido.setUsuario(irene);
        pedido.setCliente(cliente);
        pedido.setSucursal(central);
        pedido.setTipoProforma(TipoProformaEnum.MAYORISTA);
        pedido.setEstadoPedido(EstadoPedidoEnum.EN_RUTA);
        DetalleProforma linea = new DetalleProforma();
        linea.setProducto(cuadernos);
        linea.setCantidad(10);
        pedido.getItemsProforma().add(linea);
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

    private int stock(Producto producto) {
        em.flush();
        em.clear();
        return inventarioService.obtenerStock(central.getIdSucursal(), producto.getIdProducto());
    }

    private Long entregar(int cantidad, boolean aMano) {
        EntregaSolicitudDto solicitud = new EntregaSolicitudDto();
        solicitud.setIdSucursal(central.getIdSucursal());
        solicitud.setFechaEntrega(LocalDate.now());
        solicitud.setDescontadoAMano(aMano);
        EntregaSolicitudDto.Linea linea = new EntregaSolicitudDto.Linea();
        linea.setIdDetalle(pedido.getItemsProforma().get(0).getIdDetalle());
        linea.setCantidad(cantidad);
        solicitud.getLineas().add(linea);
        var entregas = entregaService.registrar(pedido.getIdProforma(), solicitud).getEntregas();
        return entregas.get(entregas.size() - 1).getIdEntrega();
    }

    /** Emite la factura como lo hace FacturaServiceImpl: planea, valida, guarda y descuenta. */
    private Factura facturar(int facturadosCuadernos) {
        Factura factura = new Factura();
        factura.setNoFactura(1L);
        factura.setSerie("A");
        factura.setTotal(new BigDecimal("100.00"));
        factura.setIdProformaOrigen(pedido.getIdProforma());
        factura.setEstado(activo);
        factura.setUsuario(irene);
        factura.setCliente(cliente);
        factura.setSucursal(central);
        DetalleFactura item = new DetalleFactura();
        item.setProducto(cuadernos);
        item.setCantidad(facturadosCuadernos);
        factura.getItemsFactura().add(item);
        Map<Integer, Integer> plan = inventarioFactura.planearVenta(factura);
        inventarioFactura.validarExistencias(plan, central, factura);
        em.persist(factura);
        em.flush();
        inventarioFactura.descontarVenta(factura, irene, plan);
        em.flush();
        return factura;
    }

    private void anular(Factura factura) {
        assertTrue(inventarioFactura.devolverVenta(factura, irene));
        em.flush();
    }

    private DetalleProforma linea() {
        em.flush();
        em.clear();
        return em.find(Proforma.class, pedido.getIdProforma()).getItemsProforma().get(0);
    }

    @Test
    void entregarYDespuesFacturarDescuentaUnaSolaVez() {
        entregar(10, false);
        assertEquals(90, stock(cuadernos));

        Factura factura = facturar(10);

        assertEquals(90, stock(cuadernos));
        assertEquals(0, factura.getItemsFactura().get(0).getCantidadDescontada());
    }

    @Test
    void facturarYDespuesEntregarDescuentaUnaSolaVez() {
        facturar(10);
        assertEquals(90, stock(cuadernos));

        entregar(10, false);

        assertEquals(90, stock(cuadernos));
        assertEquals(10, linea().getCantidadEntregada());
        assertEquals(10, linea().getCantidadDescontada());
    }

    @Test
    void entregarUnaParteYFacturarTodoDescuentaLoRestante() {
        entregar(4, false);
        assertEquals(96, stock(cuadernos));

        Factura factura = facturar(10);

        assertEquals(90, stock(cuadernos));
        assertEquals(6, factura.getItemsFactura().get(0).getCantidadDescontada());
    }

    @Test
    void anularUnaFacturaSinEntregasDevuelveTodo() {
        Factura factura = facturar(10);
        assertEquals(90, stock(cuadernos));

        anular(factura);

        assertEquals(100, stock(cuadernos));
        assertEquals(0, linea().getCantidadDescontada());
    }

    @Test
    void anularLaFacturaNoDevuelveLoQueYaSeEntregoDespuesDeFacturar() {
        Factura factura = facturar(10);
        entregar(10, false);
        assertEquals(90, stock(cuadernos));

        anular(factura);

        assertEquals(90, stock(cuadernos));
        assertEquals(10, linea().getCantidadDescontada());
        assertEquals(10, linea().getCantidadEntregada());
    }

    @Test
    void anularLaFacturaDespuesDeUnaEntregaParcialDevuelveSoloLoQueElLaDescontó() {
        entregar(4, false);
        Factura factura = facturar(10);
        assertEquals(90, stock(cuadernos));

        anular(factura);

        assertEquals(96, stock(cuadernos));
        assertEquals(4, linea().getCantidadDescontada());
    }

    @Test
    void reemitirLaFacturaTrasAnularlaDespuesDeEntregarNoDescuentaDeNuevo() {
        Factura primera = facturar(10);
        entregar(10, false);
        anular(primera);
        assertEquals(90, stock(cuadernos));

        // La reemisión de una factura entregada: el pedido ya cubre todo.
        Factura segunda = new Factura();
        segunda.setNoFactura(2L);
        segunda.setSerie("A");
        segunda.setTotal(new BigDecimal("100.00"));
        segunda.setIdProformaOrigen(pedido.getIdProforma());
        segunda.setEstado(activo);
        segunda.setUsuario(irene);
        segunda.setCliente(cliente);
        segunda.setSucursal(central);
        DetalleFactura item = new DetalleFactura();
        item.setProducto(cuadernos);
        item.setCantidad(10);
        segunda.getItemsFactura().add(item);

        assertEquals(0, inventarioFactura.planearVenta(segunda).get(cuadernos.getIdProducto()));
        assertEquals(90, stock(cuadernos));
    }

    @Test
    void entregarDescontadoAManoYFacturarNoDescuentaNada() {
        entregar(10, true);
        assertEquals(100, stock(cuadernos));

        facturar(10);

        assertEquals(100, stock(cuadernos));
    }

    @Test
    void lasVentasQueNoSonDeUnPedidoSiguenComoSiempre() {
        Factura directa = new Factura();
        directa.setSucursal(central);
        DetalleFactura item = new DetalleFactura();
        item.setProducto(lapices);
        item.setCantidad(3);
        directa.getItemsFactura().add(item);

        assertNull(inventarioFactura.planearVenta(directa));
        assertTrue(!inventarioFactura.devolverVenta(directa, irene));
        assertEquals(100, stock(lapices));
    }

    @Test
    void sinExistenciasParaLoQueFaltaDescontarLaFacturaFalla() {
        em.createQuery("update InventarioSucursal i set i.stock = 3 where i.producto.idProducto = :id")
                .setParameter("id", cuadernos.getIdProducto()).executeUpdate();
        em.clear();
        pedido = em.find(Proforma.class, pedido.getIdProforma());

        Factura factura = new Factura();
        factura.setIdProformaOrigen(pedido.getIdProforma());
        factura.setSucursal(central);
        DetalleFactura item = new DetalleFactura();
        item.setProducto(cuadernos);
        item.setCantidad(10);
        factura.getItemsFactura().add(item);
        Map<Integer, Integer> plan = inventarioFactura.planearVenta(factura);

        assertThrows(BadRequestException.class, () -> inventarioFactura.validarExistencias(plan, central, factura));
    }
}
