package xyz.pangosoft.dtodo.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** El hash de la contraseña no debe salir nunca en una respuesta JSON ni en el toString (logs). */
class UsuarioPasswordJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void noSerializaElPasswordPeroSiLoLee() throws Exception {
        Usuario usuario = Usuario.builder().idUsuario(1).usuario("admin").password("$2a$10$hash").build();

        assertFalse(objectMapper.writeValueAsString(usuario).contains("password"));
        assertFalse(usuario.toString().contains("$2a$10$hash"));

        Usuario leido = objectMapper.readValue("{\"usuario\":\"admin\",\"password\":\"nueva\"}", Usuario.class);
        assertEquals("nueva", leido.getPassword());
    }
}
