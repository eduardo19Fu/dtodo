package xyz.pangosoft.dtodo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import xyz.pangosoft.dtodo.dto.ProductoDtoMejorado;
import xyz.pangosoft.dtodo.model.DescuentoProducto;
import xyz.pangosoft.dtodo.model.Estado;
import xyz.pangosoft.dtodo.model.InventarioSucursal;
import xyz.pangosoft.dtodo.model.MarcaProducto;
import xyz.pangosoft.dtodo.model.PrecioMayorista;
import xyz.pangosoft.dtodo.model.Producto;
import xyz.pangosoft.dtodo.model.Sucursal;
import xyz.pangosoft.dtodo.model.TipoProducto;
import xyz.pangosoft.dtodo.model.enums.TipoDescuentoEnum;
import xyz.pangosoft.dtodo.service.IDescuentoService;
import xyz.pangosoft.dtodo.service.IProductoService;

/** El filtro de descuentos mayoristas del listado de productos, ejecutado contra H2 con las consultas reales. */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("context-test")
@Transactional
class ProductoFiltroMayoristaTest {

    @Autowired
    private IProductoService productoService;
    @Autowired
    private IDescuentoService descuentoService;
    @Autowired
    private EntityManager em;

    private Integer idSucursal;

    @BeforeEach
    void escenario() {
        Estado activo = Estado.builder().estado("ACTIVO").build();
        em.persist(activo);
        Sucursal central = Sucursal.builder().nombre("Central").direccion("Jalapa").codigoEstablecimientoSat(1)
                .esPrincipal(true).estado(activo).build();
        em.persist(central);
        idSucursal = central.getIdSucursal();
        MarcaProducto marca = MarcaProducto.builder().marca("MARCA").build();
        TipoProducto tipo = TipoProducto.builder().tipoProducto("TIPO").build();
        em.persist(marca);
        em.persist(tipo);

        Producto resma = producto("R1", "RESMA CARTA", marca, tipo, activo, central);
        Producto lapiz = producto("L1", "LAPIZ HB", marca, tipo, activo, central);
        Producto regla = producto("G1", "REGLA 30 CM", marca, tipo, activo, central);
        producto("C1", "CUADERNO", marca, tipo, activo, central);
        Producto inactivo = producto("I1", "BORRADOR", marca, tipo, activo, central);

        em.persist(DescuentoProducto.builder().producto(resma).tipo(TipoDescuentoEnum.CIENTO).cantidadMinima(100)
                .porcentaje(new BigDecimal("12.00")).activo(true).build());
        em.persist(DescuentoProducto.builder().producto(resma).tipo(TipoDescuentoEnum.FARDO).cantidadMinima(24)
                .porcentaje(new BigDecimal("15.00")).activo(true).build());
        em.persist(PrecioMayorista.builder().idProducto(lapiz.getIdProducto()).precio(new BigDecimal("1.50")).build());
        em.persist(DescuentoProducto.builder().producto(regla).tipo(TipoDescuentoEnum.FARDO).cantidadMinima(12)
                .porcentaje(new BigDecimal("10.00")).activo(true).build());
        // un descuento desactivado no cuenta
        em.persist(DescuentoProducto.builder().producto(inactivo).tipo(TipoDescuentoEnum.CIENTO).cantidadMinima(100)
                .porcentaje(new BigDecimal("5.00")).activo(false).build());
        em.flush();
        em.clear();
    }

    private Producto producto(String codigo, String nombre, MarcaProducto marca, TipoProducto tipo, Estado estado, Sucursal sucursal) {
        Producto producto = Producto.builder().codProducto(codigo).nombre(nombre).precioVenta(new BigDecimal("10.00"))
                .precioCompra(new BigDecimal("5.00")).marcaProducto(marca).tipoProducto(tipo).estado(estado).build();
        em.persist(producto);
        em.persist(InventarioSucursal.builder().sucursal(sucursal).producto(producto).stock(10).build());
        return producto;
    }

    private List<String> buscar(String filtro, String mayorista) {
        return productoService.searchProductoDtoMejorado(filtro, mayorista, "nombre", "asc", idSucursal, PageRequest.of(0, 20))
                .getContent().stream().map(ProductoDtoMejorado::getCodProducto).collect(Collectors.toList());
    }

    @Test
    void sinFiltroSeListanTodosLosProductos() {
        assertEquals(5, buscar("", null).size());
        assertEquals(5, buscar("", "").size());
        assertEquals(5, buscar("", "VALOR-DESCONOCIDO").size());
    }

    @Test
    void cadaOpcionFiltraPorLoQueElProductoTieneConfigurado() {
        assertEquals(List.of("L1", "G1", "R1"), buscar("", "ALGUNO"));   // por nombre: LAPIZ, REGLA, RESMA
        assertEquals(List.of("L1"), buscar("", "PRECIO"));
        assertEquals(List.of("R1"), buscar("", "CIENTO"));
        assertEquals(List.of("G1", "R1"), buscar("", "FARDO"));
        assertEquals(List.of("I1", "C1"), buscar("", "ninguno"));   // BORRADOR (descuento inactivo) y CUADERNO
    }

    @Test
    void elFiltroSeCombinaConLaBusquedaPorTexto() {
        assertEquals(List.of("L1"), buscar("lapiz", "ALGUNO"));
        assertTrue(buscar("cuaderno", "ALGUNO").isEmpty());
        assertEquals(List.of("C1"), buscar("cuaderno", "NINGUNO"));
    }

    @Test
    void elListadoTraeElResumenMayoristaDeCadaProducto() {
        List<ProductoDtoMejorado> pagina = productoService.searchProductoDtoMejorado("", null, "codigo", "asc", idSucursal,
                PageRequest.of(0, 20)).getContent();
        descuentoService.completarResumenMayorista(pagina);

        ProductoDtoMejorado resma = pagina.stream().filter(p -> "R1".equals(p.getCodProducto())).findFirst().get();
        ProductoDtoMejorado lapiz = pagina.stream().filter(p -> "L1".equals(p.getCodProducto())).findFirst().get();
        ProductoDtoMejorado inactivo = pagina.stream().filter(p -> "I1".equals(p.getCodProducto())).findFirst().get();
        assertTrue(resma.isDescuentoCiento());
        assertTrue(resma.isDescuentoFardo());
        assertEquals(null, resma.getPrecioMayorista());
        assertEquals(0, new BigDecimal("1.50").compareTo(lapiz.getPrecioMayorista()));
        assertFalse(lapiz.isDescuentoCiento());
        assertFalse(inactivo.isDescuentoCiento());
    }
}
