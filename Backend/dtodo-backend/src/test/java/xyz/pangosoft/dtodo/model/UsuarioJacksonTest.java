package xyz.pangosoft.dtodo.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * El formulario de usuarios envía el Usuario con la Sucursal elegida del listado de sucursales, y esa
 * Sucursal trae anidado su propio usuario encargado (con todos sus campos). Este test reproduce ese
 * cuerpo contra el ObjectMapper real de la aplicación.
 */
@SpringBootTest
@ActiveProfiles("context-test")
class UsuarioJacksonTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deserializaUsuarioConSucursalQueTraeUsuarioAnidado() {
        String json = "{"
                + "\"idUsuario\":3,\"usuario\":\"DAVID\",\"password\":\"x\",\"primerNombre\":\"JAIRON\","
                + "\"segundoNombre\":\"DAVID\",\"apellido\":\"AGUILAR\",\"enabled\":true,"
                + "\"fechaRegistro\":\"2022-04-30T09:49:21\","
                + "\"correlativos\":[],\"roles\":[{\"idRole\":1,\"role\":\"ROLE_ADMIN\"}],"
                + "\"sucursal\":{\"idSucursal\":2,\"nombre\":\"MATAQUESCUINTLA\",\"direccion\":\"X\","
                + "\"telefono\":\"1\",\"encargado\":\"Y\",\"codigoEstablecimientoSat\":2,\"esPrincipal\":false,"
                + "\"fechaRegistro\":\"2026-10-21T07:13:06\",\"estado\":{\"idEstado\":1,\"estado\":\"ACTIVO\"},"
                + "\"usuario\":{\"idUsuario\":3,\"usuario\":\"DAVID\",\"primerNombre\":\"JAIRON\","
                + "\"segundoNombre\":\"DAVID\",\"apellido\":\"AGUILAR\",\"enabled\":true,"
                + "\"fechaRegistro\":\"2022-04-30T09:49:21\"}}"
                + "}";

        Usuario usuario = assertDoesNotThrow(() -> objectMapper.readValue(json, Usuario.class));

        assertEquals(2, usuario.getSucursal().getIdSucursal());
    }

    @Test
    void roundTripDeUsuarioConSucursalSerializadaPorLaAplicacion() throws Exception {
        Sucursal central = Sucursal.builder().idSucursal(1).nombre("Central").direccion("d")
                .codigoEstablecimientoSat(1).esPrincipal(true).build();
        Usuario david = Usuario.builder().idUsuario(3).usuario("DAVID").password("hash").primerNombre("J")
                .apellido("A").enabled(true).roles(java.util.List.of(Role.builder().idRole(1).role("ROLE_ADMIN").build()))
                .sucursal(central).build();
        Sucursal mata = Sucursal.builder().idSucursal(2).nombre("Mata").direccion("d").codigoEstablecimientoSat(2)
                .usuario(david).build();
        String jsonSucursal = objectMapper.writeValueAsString(mata);
        System.out.println("JSON-SUCURSAL=" + jsonSucursal);

        String jsonUsuario = "{\"usuario\":\"NUEVO\",\"password\":\"x\",\"primerNombre\":\"N\",\"apellido\":\"A\","
                + "\"roles\":[{\"idRole\":1,\"role\":\"ROLE_ADMIN\"}],\"sucursal\":" + jsonSucursal + "}";
        Usuario usuario = assertDoesNotThrow(() -> objectMapper.readValue(jsonUsuario, Usuario.class));
        assertEquals(2, usuario.getSucursal().getIdSucursal());
    }
}
