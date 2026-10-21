package xyz.pangosoft.dtodo.util;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import xyz.pangosoft.dtodo.model.Usuario;

/**
 * Lee un Usuario anidado en una Sucursal solo como referencia (su idUsuario).
 *
 * Usuario.sucursal y Sucursal.usuario se referencian mutuamente; con deserializadores de bean en ambos
 * lados, Jackson falla con "No _valueDeserializer assigned" según el orden en que los construye. Al no
 * delegar en el deserializador de Usuario, el ciclo desaparece. Para persistir la relación basta el id.
 */
public class UsuarioReferenciaDeserializer extends JsonDeserializer<Usuario> {

    @Override
    public Usuario deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode nodo = parser.getCodec().readTree(parser);
        JsonNode id = nodo.get("idUsuario");
        if (id == null || id.isNull()) {
            return null;
        }
        return Usuario.builder().idUsuario(id.asInt()).build();
    }
}
