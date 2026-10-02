package xyz.pangosoft.dtodo.model;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import xyz.pangosoft.dtodo.model.enums.EstadoDespachoBodegaEnum;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bodega, Sucursal y Usuario se referencian entre sí (Bodega.sucursal, Sucursal.usuario, Usuario.sucursal).
 * Estos tests comprueban, contra el ObjectMapper real de la aplicación, que los gráficos de objetos que
 * viajan por la API de bodegas y despachos se (de)serializan sin ciclos ni exponer datos sensibles.
 */
@SpringBootTest
@ActiveProfiles("context-test")
class BodegaJacksonTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deserializaUnaBodegaConSucursalYEstadoAnidados() {
        String json = "{"
                + "\"nombre\":\"Principal\","
                + "\"ubicacion\":\"Zona 1\","
                + "\"descripcion\":\"Bodega central\","
                + "\"sucursal\":{\"idSucursal\":3},"
                + "\"estado\":{\"idEstado\":2,\"estado\":\"INACTIVO\"}"
                + "}";

        Bodega bodega = assertDoesNotThrow(() -> objectMapper.readValue(json, Bodega.class));

        assertEquals("Principal", bodega.getNombre());
        assertEquals(3, bodega.getSucursal().getIdSucursal());
        assertEquals("INACTIVO", bodega.getEstado().getEstado());
    }

    @Test
    void serializaUnaBodegaSinExponerLaContrasenaDelUsuarioNiRepetirLaSucursalDelUsuario() throws Exception {
        Sucursal sucursal = Sucursal.builder().idSucursal(3).nombre("Norte").direccion("Calle 1").build();
        Usuario creador = Usuario.builder().idUsuario(1).usuario("admin").password("secreto").sucursal(sucursal).build();
        sucursal.setUsuario(creador);
        Bodega bodega = Bodega.builder().idBodega(1).nombre("Principal").ubicacion("Zona 1")
                .sucursal(sucursal).usuario(creador).build();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(bodega));

        assertEquals("Norte", json.get("sucursal").get("nombre").asText());
        assertFalse(json.get("sucursal").has("usuario"));
        assertFalse(json.get("usuario").has("password"));
        assertFalse(json.get("usuario").has("sucursal"));
    }

    @Test
    void serializaUnDespachoConTodoSuGrafoSinCiclos() throws Exception {
        Sucursal sucursal = Sucursal.builder().idSucursal(3).nombre("Norte").direccion("Calle 1").build();
        Usuario despacha = Usuario.builder().idUsuario(1).usuario("bodeguero").password("secreto").sucursal(sucursal).build();
        sucursal.setUsuario(despacha);
        Bodega bodega = Bodega.builder().idBodega(1).nombre("Principal").sucursal(sucursal).usuario(despacha).build();
        DespachoBodega despacho = DespachoBodega.builder().idDespacho(7L).estado(EstadoDespachoBodegaEnum.PENDIENTE)
                .total(new BigDecimal("12.50")).recibidoPor("María").bodega(bodega).sucursalDestino(sucursal)
                .usuarioDespacha(despacha).build();
        despacho.getItems().add(DespachoBodegaDetalle.builder().cantidad(5).existenciaBodega(40)
                .precioUnitario(new BigDecimal("2.50")).subTotal(new BigDecimal("12.50"))
                .producto(Producto.builder().idProducto(9).nombre("Lápiz").build()).build());

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(despacho));

        assertEquals("PENDIENTE", json.get("estado").asText());
        assertEquals("Norte", json.get("sucursalDestino").get("nombre").asText());
        assertEquals(1, json.get("items").size());
        assertEquals(40, json.get("items").get(0).get("existenciaBodega").asInt());
        assertFalse(json.get("usuarioDespacha").has("password"));
        assertTrue(json.get("bodega").has("sucursal"));
    }

    @Test
    void serializaElInventarioYLosMovimientosDeUnaBodega() {
        Bodega bodega = Bodega.builder().idBodega(1).nombre("Principal").build();
        Producto producto = Producto.builder().idProducto(9).nombre("Lápiz").build();

        InventarioBodega inventario = InventarioBodega.builder().bodega(bodega).producto(producto).stock(3).build();
        MovimientoBodega movimiento = MovimientoBodega.builder().bodega(bodega).producto(producto).cantidad(3).build();

        assertDoesNotThrow(() -> objectMapper.writeValueAsString(List.of(inventario)));
        assertDoesNotThrow(() -> objectMapper.writeValueAsString(List.of(movimiento)));
    }
}
