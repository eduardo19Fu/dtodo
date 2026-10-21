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

    @Test
    void deserializaSucursalConElUsuarioCompletoQueEnviaElFormulario() {
        String json = "{\"nombre\":\"N\",\"direccion\":\"D\",\"telefono\":\"1\",\"encargado\":\"E\",\"codigoEstablecimientoSat\":\"3\","
                + "\"usuario\":{\"idUsuario\":3,\"usuario\":\"DAVID\",\"password\":\"$2a$10$h\",\"primerNombre\":\"J\","
                + "\"segundoNombre\":null,\"apellido\":\"A\",\"enabled\":true,\"fechaRegistro\":\"2022-04-30T09:49:21\","
                + "\"roles\":[{\"idRole\":1,\"role\":\"ROLE_ADMIN\"}],"
                + "\"sucursal\":{\"idSucursal\":1,\"nombre\":\"C\",\"direccion\":\"d\",\"codigoEstablecimientoSat\":1,"
                + "\"esPrincipal\":true,\"fechaRegistro\":\"2026-09-09T22:04:18\",\"estado\":{\"idEstado\":1,\"estado\":\"ACTIVO\"}}}}";

        Sucursal sucursal = assertDoesNotThrow(() -> objectMapper.readValue(json, Sucursal.class));

        assertEquals(3, sucursal.getUsuario().getIdUsuario());
    }

    @Test
    void deserializaElCuerpoExactoReportadoEnDev() {
        String json = "{\"nombre\":\"Sucursal de prueba\",\"direccion\":\"Jalapa\",\"codigoEstablecimientoSat\":2,\"usuario\":{\"idUsuario\":1,\"usuario\":\"efuentes\",\"password\":\"******\",\"primerNombre\":\"Ramiro\",\"segundoNombre\":\"Eduardo\",\"apellido\":\"Fuentes\",\"enabled\":true,\"fechaRegistro\":\"2022-03-19T00:00:00\",\"roles\":[{\"idRole\":1,\"role\":\"ROLE_ADMIN\"},{\"idRole\":2,\"role\":\"ROLE_COBRADOR\"}],\"sucursal\":{\"idSucursal\":1,\"nombre\":\"Sucursal Central\",\"direccion\":\"1 CALLE 4-95 ZONA 2\",\"telefono\":null,\"encargado\":null,\"codigoEstablecimientoSat\":1,\"esPrincipal\":true,\"fechaRegistro\":\"2026-09-09T22:04:18\",\"estado\":{\"idEstado\":1,\"estado\":\"ACTIVO\"}}}}";

        Sucursal sucursal = assertDoesNotThrow(() -> objectMapper.readValue(json, Sucursal.class));

        assertEquals(1, sucursal.getUsuario().getIdUsuario());
    }
}
