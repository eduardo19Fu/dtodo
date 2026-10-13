package xyz.pangosoft.dtodo.util;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import xyz.pangosoft.dtodo.dto.DespachoBodegaDto;
import xyz.pangosoft.dtodo.dto.InventarioBodegaDto;
import xyz.pangosoft.dtodo.dto.ProductoDto;
import xyz.pangosoft.dtodo.dto.ProductoDtoMejorado;
import xyz.pangosoft.dtodo.model.DespachoBodega;
import xyz.pangosoft.dtodo.model.DespachoBodegaDetalle;
import xyz.pangosoft.dtodo.model.Producto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El costo (precio de compra), el margen que lo revela y el valor de los despachos al costo solo salen en
 * las respuestas JSON para un administrador. El resto de los datos no se ve afectado.
 */
class SoloAdministradorSerializerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private void iniciarSesion(String rol) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("usuario", "x", rol));
    }

    private JsonNode json(Object objeto) throws Exception {
        return objectMapper.readTree(objectMapper.writeValueAsString(objeto));
    }

    private Producto producto() {
        return Producto.builder().idProducto(1).nombre("Lápiz").precioCompra(new BigDecimal("8.50"))
                .precioVenta(new BigDecimal("12.00")).porcentajeGanancia(41.18f).stock(5).build();
    }

    @Test
    void elAdministradorVeElCostoYElMargenDelProducto() throws Exception {
        iniciarSesion("ROLE_ADMIN");

        JsonNode producto = json(producto());

        assertEquals(8.5, producto.get("precioCompra").asDouble());
        assertEquals(41.18, producto.get("porcentajeGanancia").asDouble(), 0.001);
        assertEquals(12.0, producto.get("precioVenta").asDouble());
    }

    @Test
    void cualquierOtroRolVeElProductoSinCostoNiMargenPeroConSuPrecioDeVenta() throws Exception {
        for (String rol : new String[] { "ROLE_BODEGA", "ROLE_COBRADOR", "ROLE_INVENTARIO" }) {
            iniciarSesion(rol);

            JsonNode producto = json(producto());

            assertTrue(producto.get("precioCompra").isNull(), rol);
            assertTrue(producto.get("porcentajeGanancia").isNull(), rol);
            assertEquals(12.0, producto.get("precioVenta").asDouble(), rol);
            assertEquals("Lápiz", producto.get("nombre").asText(), rol);
            assertEquals(5, producto.get("stock").asInt(), rol);
        }
    }

    @Test
    void sinSesionTampocoSeEntregaElCosto() throws Exception {
        JsonNode producto = json(producto());

        assertTrue(producto.get("precioCompra").isNull());
        assertTrue(producto.get("porcentajeGanancia").isNull());
    }

    @Test
    void losDtoDeProductoOcultanElCostoYElMargenAQuienNoEsAdministrador() throws Exception {
        iniciarSesion("ROLE_COBRADOR");
        ProductoDto dto = new ProductoDto();
        dto.setPrecioCompra(new BigDecimal("8.50"));
        dto.setPrecioVenta(new BigDecimal("12.00"));
        dto.setPorcentajeGanancia(41.18f);
        ProductoDtoMejorado mejorado = ProductoDtoMejorado.builder().precioCompra(new BigDecimal("8.50"))
                .precioVenta(new BigDecimal("12.00")).porcentajeGanancia(41.18f).build();

        for (JsonNode nodo : new JsonNode[] { json(dto), json(mejorado) }) {
            assertTrue(nodo.get("precioCompra").isNull());
            assertTrue(nodo.get("porcentajeGanancia").isNull());
            assertEquals(12.0, nodo.get("precioVenta").asDouble());
        }
        iniciarSesion("ROLE_ADMIN");
        assertEquals(8.5, json(dto).get("precioCompra").asDouble());
        assertEquals(41.18, json(mejorado).get("porcentajeGanancia").asDouble(), 0.001);
    }

    @Test
    void elInventarioDeBodegaOcultaElCostoUnitario() throws Exception {
        InventarioBodegaDto inventario = new InventarioBodegaDto();
        inventario.setStock(7);
        inventario.setPrecioCompra(new BigDecimal("2.60"));

        iniciarSesion("ROLE_BODEGA");
        JsonNode comoBodega = json(inventario);
        iniciarSesion("ROLE_ADMIN");
        JsonNode comoAdmin = json(inventario);

        assertTrue(comoBodega.get("precioCompra").isNull());
        assertEquals(7, comoBodega.get("stock").asInt());
        assertEquals(2.6, comoAdmin.get("precioCompra").asDouble());
    }

    @Test
    void losDespachosOcultanSusTotalesCostosUnitariosYSubtotalesAQuienNoEsAdministrador() throws Exception {
        DespachoBodega despacho = DespachoBodega.builder().idDespacho(7L).total(new BigDecimal("75.00")).recibidoPor("María").build();
        despacho.getItems().add(DespachoBodegaDetalle.builder().cantidad(6).existenciaBodega(40)
                .precioUnitario(new BigDecimal("12.50")).subTotal(new BigDecimal("75.00"))
                .producto(producto()).build());
        DespachoBodegaDto fila = new DespachoBodegaDto();
        fila.setIdDespacho(7L);
        fila.setTotal(new BigDecimal("75.00"));

        iniciarSesion("ROLE_BODEGA");
        JsonNode comoBodega = json(despacho);
        JsonNode filaComoBodega = json(fila);
        iniciarSesion("ROLE_ADMIN");
        JsonNode comoAdmin = json(despacho);
        JsonNode filaComoAdmin = json(fila);

        assertTrue(comoBodega.get("total").isNull());
        assertTrue(comoBodega.get("items").get(0).get("precioUnitario").isNull());
        assertTrue(comoBodega.get("items").get(0).get("subTotal").isNull());
        // el costo del producto anidado también queda oculto
        assertTrue(comoBodega.get("items").get(0).get("producto").get("precioCompra").isNull());
        assertEquals(6, comoBodega.get("items").get(0).get("cantidad").asInt());
        assertEquals(40, comoBodega.get("items").get(0).get("existenciaBodega").asInt());
        assertTrue(filaComoBodega.get("total").isNull());

        assertEquals(75.0, comoAdmin.get("total").asDouble());
        assertEquals(12.5, comoAdmin.get("items").get(0).get("precioUnitario").asDouble());
        assertEquals(8.5, comoAdmin.get("items").get(0).get("producto").get("precioCompra").asDouble());
        assertEquals(75.0, filaComoAdmin.get("total").asDouble());
    }
}
