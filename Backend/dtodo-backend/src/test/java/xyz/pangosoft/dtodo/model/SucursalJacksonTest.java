package xyz.pangosoft.dtodo.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Sucursal.usuario y Usuario.sucursal se referencian mutuamente. Sin romper el ciclo en ambos
 * lados con @JsonIgnoreProperties, Jackson falla al construir el deserializador con
 * "JSON parse error: No _valueDeserializer assigned" al recibir un Sucursal con usuario anidado
 * (ver POST /api/sucursales). Este test reproduce exactamente ese escenario contra el ObjectMapper
 * real de la aplicación.
 */
@SpringBootTest
@ActiveProfiles("context-test")
class SucursalJacksonTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deserializaUnaSucursalConUsuarioAnidadoSinCicloInfinito() {
        String json = "{"
                + "\"nombre\":\"Sucursal Prueba\","
                + "\"direccion\":\"Calle de prueba\","
                + "\"codigoEstablecimientoSat\":2,"
                + "\"usuario\":{\"idUsuario\":1,\"usuario\":\"admin\"}"
                + "}";

        Sucursal sucursal = assertDoesNotThrow(() -> objectMapper.readValue(json, Sucursal.class));

        assertEquals("Sucursal Prueba", sucursal.getNombre());
        assertEquals(1, sucursal.getUsuario().getIdUsuario());
    }

    @Test
    void serializaUnaSucursalConUsuarioAnidadoSinCicloInfinito() {
        Sucursal sucursal = Sucursal.builder()
                .nombre("Sucursal Prueba")
                .direccion("Calle de prueba")
                .codigoEstablecimientoSat(2)
                .usuario(Usuario.builder().idUsuario(1).usuario("admin").build())
                .build();

        assertDoesNotThrow(() -> objectMapper.writeValueAsString(sucursal));
    }
}
